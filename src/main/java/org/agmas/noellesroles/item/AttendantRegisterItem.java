package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.puppeteer.PuppeteerPlayerComponent;
import org.agmas.noellesroles.morphling.BodyForgeryWorldComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.OpenAttendantRegisterS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;

import java.util.List;
import java.util.UUID;

/** 乘务登记表：右键空气打开登记面板；右键 3 格内的乘客/尸体进行登记与高亮。 */
public class AttendantRegisterItem extends Item {
    private static final double REGISTER_RANGE_SQ = 9.0D;

    public AttendantRegisterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(user, Noellesroles.ATTENDANT)) {
            return TypedActionResult.pass(stack);
        }
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (user instanceof ServerPlayerEntity serverUser
                && GameFunctions.isPlayerPlayingAndAlive(user)
                && !SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            AttendantPlayerComponent component = AttendantPlayerComponent.KEY.get(user);
            component.ensureEntries();
            component.sync();
            ServerPlayNetworking.send(serverUser, new OpenAttendantRegisterS2CPacket());
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(user.getWorld());
        if (!gameWorld.isRole(user, Noellesroles.ATTENDANT)) {
            return ActionResult.PASS;
        }
        if (!(entity instanceof PlayerEntity) && !(entity instanceof PlayerBodyEntity) && !(entity instanceof PuppetEntity)) {
            return ActionResult.PASS;
        }
        // 客户端先认领这次点击：服务端一旦返回 PASS，原版 doItemUse 会落到 use() 误开面板
        if (user.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity serverUser)
                || !GameFunctions.isPlayerPlayingAndAlive(user)
                || SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            return ActionResult.PASS;
        }

        AttendantPlayerComponent component = AttendantPlayerComponent.KEY.get(user);

        // 傀儡师的闲置傀儡 / 本体替身：按所见外观（傀儡为死者，替身为傀儡师本人）登记见面与存活
        if (entity instanceof PuppetEntity standIn) {
            UUID identity = standIn.getSkinUuid();
            if (identity == null || identity.equals(user.getUuid())
                    || user.squaredDistanceTo(standIn) > REGISTER_RANGE_SQ) {
                return ActionResult.PASS;
            }
            component.ensureEntries();
            boolean metChanged = component.markMet(identity);
            boolean aliveChanged = component.markAlive(identity);
            if (!metChanged && !aliveChanged) {
                return ActionResult.PASS;
            }
            component.highlight(standIn, AttendantPlayerComponent.HIGHLIGHT_TICKS);
            NbtCompound extra = new NbtCompound();
            extra.putString("action", "met");
            extra.putUuid("target", identity);
            GameRecordManager.recordSkillUse(serverUser, Noellesroles.ATTENDANT_ID, null, extra);
            return ActionResult.SUCCESS;
        }

        if (entity instanceof PlayerEntity target) {
            if (target == user
                    || !GameFunctions.isPlayerPlayingAndAlive(target)
                    || SwallowedInteractionHelper.blocksPlayerTargetForViewer(user, target)
                    || user.squaredDistanceTo(target) > REGISTER_RANGE_SQ) {
                return ActionResult.PASS;
            }
            component.ensureEntries();

            // 变形者兼容：按所见外观登记——活人外观登记伪装对象的见面+存活，尸体外观（伪装尸体/本人假尸体）登记对应身份的死亡
            MorphlingPlayerComponent morph = MorphlingPlayerComponent.KEY.get(target);
            // 傀儡形态的傀儡师外观是死者，按死者登记
            UUID puppetDisguise = PuppeteerPlayerComponent.KEY.get(target).getControlledSource();
            boolean disguised = puppetDisguise != null || (morph.getMorphTicks() > 0
                    && morph.disguise != null
                    && !morph.disguise.equals(target.getUuid()));
            UUID identity = puppetDisguise != null ? puppetDisguise : disguised ? morph.disguise : target.getUuid();
            if (identity.equals(user.getUuid())) {
                // 伪装成乘务员本人时不能登记自己（与 target == user 一致）
                return ActionResult.PASS;
            }

            if (morph.corpseMode) {
                AttendantPlayerComponent.Entry identityEntry = component.getEntry(identity);
                if (identityEntry == null || identityEntry.getAlive() == AttendantPlayerComponent.NO) {
                    return ActionResult.PASS;
                }
                component.markDead(identity);
                component.highlight(target, AttendantPlayerComponent.HIGHLIGHT_TICKS);
                NbtCompound corpseExtra = new NbtCompound();
                corpseExtra.putString("action", "dead");
                corpseExtra.putUuid("victim", identity);
                if (disguised) {
                    corpseExtra.putUuid("morphling", target.getUuid());
                }
                GameRecordManager.recordSkillUse(serverUser, Noellesroles.ATTENDANT_ID, null, corpseExtra);
                return ActionResult.SUCCESS;
            }

            // 点击存活玩家（或活人外观的伪装者）同时校正见面与存活两项登记
            boolean metChanged = component.markMet(identity);
            boolean aliveChanged = component.markAlive(identity);
            if (!metChanged && !aliveChanged) {
                return ActionResult.PASS;
            }
            component.highlight(target, AttendantPlayerComponent.HIGHLIGHT_TICKS);
            NbtCompound extra = new NbtCompound();
            extra.putString("action", "met");
            extra.putUuid("target", identity);
            ServerPlayerEntity recordTarget = null;
            if (disguised) {
                // 回放揭示真正被点击的变形者；被登记人已写入 extra.target
                extra.putUuid("morphling", target.getUuid());
            } else if (target instanceof ServerPlayerEntity serverTarget) {
                recordTarget = serverTarget;
            }
            GameRecordManager.recordSkillUse(serverUser, Noellesroles.ATTENDANT_ID, recordTarget, extra);
            return ActionResult.SUCCESS;
        }

        PlayerBodyEntity body = (PlayerBodyEntity) entity;
        if (!BodyTargetHelper.canPlayerSeeBody(user, body)
                || user.squaredDistanceTo(body) > REGISTER_RANGE_SQ) {
            return ActionResult.PASS;
        }
        UUID victimUuid = body.getPlayerUuid();
        // 赝造药水：登记按所见身份，伪装时登记的是指定玩家
        UUID forgedUuid = BodyForgeryWorldComponent.KEY.get(user.getWorld()).getForgedSkin(body.getUuid());
        UUID reportedUuid = forgedUuid != null ? forgedUuid : victimUuid;
        component.ensureEntries();
        AttendantPlayerComponent.Entry entry = component.getEntry(reportedUuid);
        if (entry == null || entry.getAlive() == AttendantPlayerComponent.NO) {
            return ActionResult.PASS;
        }

        NbtCompound extra = new NbtCompound();
        extra.putUuid("victim", victimUuid);
        if (forgedUuid != null) {
            // 回放揭示真实死者；被登记人已写入 extra.victim 的伪装身份
            extra.putUuid("forged_as", forgedUuid);
        }
        boolean decomposed = body.age >= GameConstants.TIME_TO_DECOMPOSITION + GameConstants.DECOMPOSING_TIME;
        if (decomposed) {
            // 白骨化：无法辨认，只高亮不登记
            component.highlight(body, AttendantPlayerComponent.HIGHLIGHT_TICKS);
            serverUser.sendMessage(Text.translatable("tip.noellesroles.attendant.body_unrecognizable"), true);
            extra.putString("action", "unrecognizable");
        } else {
            component.markDead(reportedUuid);
            component.highlight(body, AttendantPlayerComponent.HIGHLIGHT_TICKS);
            extra.putString("action", "dead");
        }
        GameRecordManager.recordSkillUse(serverUser, Noellesroles.ATTENDANT_ID, null, extra);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.attendant_register.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}

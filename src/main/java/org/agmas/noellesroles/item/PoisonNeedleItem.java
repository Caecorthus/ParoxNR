package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.hallucination.HallucinationDummyInteractionHelper;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 毒针物品
 * - 右键点击玩家注入毒素
 * - 可重复使用，使用后45秒冷却
 * - 开局1分钟冷却
 */
public class PoisonNeedleItem extends Item {
    public static final int USE_COOLDOWN_TICKS = 20 * 45; // 45秒冷却

    public PoisonNeedleItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.poison_needle.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            HallucinationPlayerComponent hallucinationComponent = HallucinationPlayerComponent.KEY.get(user);
            Optional<UUID> dummyId = HallucinationDummyInteractionHelper.getDummyId(entity, user);
            if (dummyId.isPresent()) {
                int baseTicks = 20 * 40;
                if (!hallucinationComponent.applyPoisonToDummy(dummyId.get(), baseTicks, user.getUuid(), Noellesroles.POISON_SOURCE_NEEDLE)) {
                    return ActionResult.PASS;
                }
                user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
                hallucinationComponent.sync();
                return ActionResult.SUCCESS;
            }

            // 傀儡师：闲置傀儡被扎即倒地；本体替身被扎则傀儡师被迫回到本体，毒扎在本体身上
            if (entity instanceof PuppetEntity puppet) {
                if (!puppet.isShell()) {
                    PuppeteerControl.onEntityHit(puppet, user instanceof ServerPlayerEntity serverUser ? serverUser : null,
                            GameConstants.DeathReasons.POISON);
                    user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
                    return ActionResult.SUCCESS;
                }
                entity = PuppeteerControl.returnShellOwnerToBody(puppet);
            }
            if (!(entity instanceof PlayerEntity target)) {
                return ActionResult.PASS;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
                return ActionResult.PASS;
            }

            // 固定中毒时间 40秒 (800 ticks)，已中毒则加速
            int baseTicks = 20 * 40;
            PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
            int poisonTicks = poisonComp.poisonTicks > 0
                    ? Math.max(1, poisonComp.poisonTicks - baseTicks)
                    : baseTicks;

            poisonComp.setPoisonTicks(poisonTicks, user.getUuid(), Noellesroles.POISON_SOURCE_NEEDLE);

            // 设置冷却
            user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);

            // 记录使用
            if (user instanceof ServerPlayerEntity serverUser) {
                GameRecordManager.recordItemUse(serverUser, Registries.ITEM.getId(this), (ServerPlayerEntity) target, null);
            }
        }

        return ActionResult.SUCCESS;
    }
}

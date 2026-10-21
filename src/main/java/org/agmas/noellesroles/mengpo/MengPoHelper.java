package org.agmas.noellesroles.mengpo;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.MengPoSoupItem;
import org.agmas.noellesroles.util.RoleTransformHelper;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;
import org.jetbrains.annotations.Nullable;

/**
 * 孟婆：开局选择、卖汤、孟婆汤生效。
 */
public final class MengPoHelper {
    public static final int SOUP_PRICE = 250;

    public static final Identifier DUREN_SKILL_ID = Identifier.of(Noellesroles.MOD_ID, "mengpo_duren");
    public static final Identifier DUJI_SKILL_ID = Identifier.of(Noellesroles.MOD_ID, "mengpo_duji");
    public static final Identifier SOUP_BUY_SKILL_ID = Identifier.of(Noellesroles.MOD_ID, "mengpo_soup_buy");
    public static final Identifier SOUP_DRINK_SKILL_ID = Identifier.of(Noellesroles.MOD_ID, "mengpo_soup_drink");
    public static final Identifier SOUP_FEED_SKILL_ID = Identifier.of(Noellesroles.MOD_ID, "mengpo_soup_feed");
    public static final Identifier SOUP_TRANSFORM_SKILL_ID = Identifier.of(Noellesroles.MOD_ID, "mengpo_soup_transform");

    private MengPoHelper() {}

    /** 开局选择（玩家点选或超时）。渡己：立即转为随机乘客职业，再拿到一碗只能自己喝的汤。 */
    public static void applyChoice(ServerPlayerEntity player, boolean duji) {
        MengPoPlayerComponent component = MengPoPlayerComponent.KEY.get(player);
        if (!duji) {
            component.finishChoice(MengPoPlayerComponent.Mode.DUREN);
            giveSoup(player, MengPoSoupItem.create(MengPoSoupItem.Kind.DUREN, player.getUuid()));
            player.sendMessage(Text.translatable("tip.mengpo.chose_duren"), true);
            GameRecordManager.recordSkillUse(player, DUREN_SKILL_ID, null, null);
            return;
        }

        component.finishChoice(MengPoPlayerComponent.Mode.NONE);
        Role newRole = RoleTransformHelper.pickRandomRole(player, Faction.CIVILIAN);
        if (newRole == null) {
            newRole = WatheRoles.CIVILIAN;
        }
        GameRecordManager.recordSkillUse(player, DUJI_SKILL_ID, null, null);
        RoleTransformHelper.transform(player, newRole);
        // 转职会清空物品栏，汤必须在转职之后再发
        giveSoup(player, MengPoSoupItem.create(MengPoSoupItem.Kind.SELF, player.getUuid()));
        player.sendMessage(Text.translatable("tip.mengpo.chose_duji"), true);
    }

    /** 孟婆汤入口：喝汤者开始 60 秒心跳期。 */
    public static void applySoup(ServerPlayerEntity user, ServerPlayerEntity target) {
        MengPoSoupPlayerComponent.KEY.get(target).start();
        target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.8F, 0.9F);
        if (user == target) {
            GameRecordManager.recordSkillUse(user, SOUP_DRINK_SKILL_ID, null, null);
            user.sendMessage(Text.translatable("tip.mengpo.soup.drank"), true);
        } else {
            GameRecordManager.recordSkillUse(user, SOUP_FEED_SKILL_ID, target, null);
            user.sendMessage(Text.translatable("tip.mengpo.soup.fed", target.getName()), true);
            target.sendMessage(Text.translatable("tip.mengpo.soup.drank"), true);
        }
    }

    /**
     * 心跳期结束：乘客（带枪/义警槽位/列车长除外）与杀手转为本阵营的随机职业；
     * 中立与其它无法转变的职业汤照样消耗、心跳照样持续，但职业不变。
     */
    public static void resolveSoup(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role currentRole = gameWorld.getRole(player);
        Role newRole = canSoupTransform(currentRole)
                ? RoleTransformHelper.pickRandomRole(player, currentRole.getFaction())
                : null;
        if (newRole == null) {
            player.sendMessage(Text.translatable("tip.mengpo.soup.no_change"), true);
            return;
        }
        GameRecordManager.recordSkillUse(player, SOUP_TRANSFORM_SKILL_ID, null, null);
        RoleTransformHelper.transform(player, newRole);
        player.sendMessage(Text.translatable("tip.mengpo.soup.transformed"), true);
    }

    public static boolean canSoupTransform(Role role) {
        if (role == null || WatheRoles.SPECIAL_ROLES.contains(role)) {
            return false;
        }
        if (role.getFaction() == Faction.KILLER) {
            return true;
        }
        return role.getFaction() == Faction.CIVILIAN
                && role != Noellesroles.CONDUCTOR
                && !Noellesroles.isVigilanteSlotRole(role);
    }

    /** 潜行空手右键渡人孟婆：花 250 金币买一碗孟婆汤。 */
    public static ActionResult tryBuySoup(PlayerEntity player, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        // 客户端返回 PASS 让原版流程正常发包，实际逻辑由服务端执行。
        // 客户端每次右键会先后发送 INTERACT_AT（带命中点）与 INTERACT 两个包，服务端只在后者结算，避免一次右键买两碗
        if (!(player instanceof ServerPlayerEntity buyer) || hand != Hand.MAIN_HAND || hitResult != null) {
            return ActionResult.PASS;
        }
        if (!(entity instanceof ServerPlayerEntity mengpo) || buyer == mengpo
                || !buyer.isSneaking() || !buyer.getMainHandStack().isEmpty()) {
            return ActionResult.PASS;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(buyer.getWorld());
        if (!gameWorld.isRole(mengpo, Noellesroles.MENGPO) || !MengPoPlayerComponent.KEY.get(mengpo).isDuren()) {
            return ActionResult.PASS;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(buyer) || !GameFunctions.isPlayerPlayingAndAlive(mengpo)
                || SwallowedInteractionHelper.blocksPlayerTarget(buyer) || SwallowedInteractionHelper.blocksPlayerTarget(mengpo)) {
            return ActionResult.PASS;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(buyer);
        if (shop.getBalance() < SOUP_PRICE) {
            buyer.playSoundToPlayer(WatheSounds.UI_SHOP_BUY_FAIL, SoundCategory.PLAYERS, 1.0F, 1.0F);
            buyer.sendMessage(Text.translatable("tip.mengpo.buy.not_enough", SOUP_PRICE), true);
            return ActionResult.SUCCESS;
        }

        shop.addToBalance(-SOUP_PRICE);
        giveSoup(buyer, MengPoSoupItem.create(MengPoSoupItem.Kind.BOUGHT, null));
        buyer.playSoundToPlayer(WatheSounds.UI_SHOP_BUY, SoundCategory.PLAYERS, 1.0F, 1.0F);
        buyer.sendMessage(Text.translatable("tip.mengpo.buy.success", SOUP_PRICE), true);
        mengpo.sendMessage(Text.translatable("tip.mengpo.buy.sold", buyer.getName()), true);
        GameRecordManager.recordSkillUse(buyer, SOUP_BUY_SKILL_ID, mengpo, null);
        return ActionResult.SUCCESS;
    }

    private static void giveSoup(ServerPlayerEntity player, ItemStack soup) {
        if (!player.giveItemStack(soup)) {
            player.dropItem(soup, false);
        }
        player.currentScreenHandler.sendContentUpdates();
    }
}

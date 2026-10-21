package org.agmas.noellesroles.mixin.puppeteer;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerVeteranComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刀刺傀儡 / 本体替身：Wathe 的 KnifeStabPayload 只结算玩家目标，这里接管目标为 PuppetEntity 的情况，
 * 按 Wathe 原流程结算：老兵刺击次数、刺击音效（清道夫静音）、挥手，以及同一条动态刀冷却公式
 * （连环杀手减半、残局者固定 45 秒）。
 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class PuppetKnifeStabMixin {
    @Unique
    private static final int LOOSE_END_KNIFE_COOLDOWN_TICKS = 20 * 45;

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void noellesroles$stabPuppet(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PuppetEntity puppet)) {
            return;
        }
        ci.cancel();
        if (SwallowedInteractionHelper.blocksActor(player)
                || player.getUuid().equals(puppet.getOwnerUuid())
                || puppet.distanceTo(player) > 3.0F) {
            return;
        }
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.getRole(player) == WatheRoles.VETERAN && !noellesroles$useVeteranStab(player)) {
            return;
        }
        PuppeteerControl.onEntityHit(puppet, player, GameConstants.DeathReasons.KNIFE);
        if (!gameWorldComponent.isRole(player, Noellesroles.SCAVENGER)) {
            puppet.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0F, 1.0F);
        }
        player.swingHand(Hand.MAIN_HAND);
        if (player.isCreative()) {
            return;
        }
        if (gameWorldComponent.isRole(player, WatheRoles.LOOSE_END)) {
            player.getItemCooldownManager().set(WatheItems.KNIFE, LOOSE_END_KNIFE_COOLDOWN_TICKS);
            return;
        }
        if (gameWorldComponent.getGameMode() == WatheGameModes.LOOSE_ENDS) {
            return;
        }
        int cooldown = noellesroles$knifeCooldown(player, gameWorldComponent);
        if (gameWorldComponent.isRole(player, Noellesroles.SERIAL_KILLER)) {
            cooldown /= 2;
        }
        player.getItemCooldownManager().set(WatheItems.KNIFE, cooldown);
    }

    /** 与 Wathe 相同：老兵每次刺击消耗一次次数，用完收走刀。返回 false 表示已无次数、不能刺击。 */
    @Unique
    private static boolean noellesroles$useVeteranStab(ServerPlayerEntity player) {
        PlayerVeteranComponent veteran = PlayerVeteranComponent.KEY.get(player);
        if (!veteran.hasStabUsesLeft()) {
            return false;
        }
        veteran.useStab();
        if (!veteran.hasStabUsesLeft()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                if (player.getInventory().getStack(i).isOf(WatheItems.KNIFE)) {
                    player.getInventory().removeStack(i);
                    break;
                }
            }
        }
        return true;
    }

    /** 与 Wathe KnifeStabPayload 相同的动态冷却公式（随人数递减，最低10秒）。 */
    @Unique
    private static int noellesroles$knifeCooldown(ServerPlayerEntity player, GameWorldComponent gameWorldComponent) {
        int totalPlayers = player.getServerWorld().getPlayers().size();
        int killerCount = gameWorldComponent.getAllKillerTeamPlayers().size();
        int excessPlayers = Math.max(0, totalPlayers - (killerCount * gameWorldComponent.getKillerDividend()));
        int baseCooldown = GameConstants.ITEM_COOLDOWNS.get(WatheItems.KNIFE);
        return Math.max(GameConstants.getInTicks(0, 10), baseCooldown - (excessPlayers * GameConstants.getInTicks(0, 5)));
    }
}

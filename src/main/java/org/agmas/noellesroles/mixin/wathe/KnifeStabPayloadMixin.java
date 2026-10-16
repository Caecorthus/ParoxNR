package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 连环杀手的刀冷却减半：在 Wathe 的 KnifeStabPayload 结算完默认动态冷却后，
 * 用同一条公式重算并写入一半的冷却。每次成功捅刺（无论是否击杀）都生效。
 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class KnifeStabPayloadMixin {
    @Inject(method = "receive", at = @At("RETURN"))
    private void noellesroles$halveSerialKillerKnifeCooldown(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (player.isCreative()) return;
        if (!(player.getServerWorld().getEntityById(payload.target()) instanceof ServerPlayerEntity target)) return;
        if (target.distanceTo(player) > 3.0) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.getGameMode() == WatheGameModes.LOOSE_ENDS) return;
        if (!gameWorldComponent.isRole(player, Noellesroles.SERIAL_KILLER)) return;

        // 与 Wathe KnifeStabPayload 相同的动态冷却公式（随人数递减，最低10秒），再取其一半
        int totalPlayers = player.getServerWorld().getPlayers().size();
        int killerCount = gameWorldComponent.getAllKillerTeamPlayers().size();
        int killerRatio = gameWorldComponent.getKillerDividend();
        int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
        int baseCooldown = GameConstants.ITEM_COOLDOWNS.get(WatheItems.KNIFE);
        int adjustedCooldown = Math.max(GameConstants.getInTicks(0, 10), baseCooldown - (excessPlayers * GameConstants.getInTicks(0, 5)));
        player.getItemCooldownManager().set(WatheItems.KNIFE, adjustedCooldown / 2);
    }
}

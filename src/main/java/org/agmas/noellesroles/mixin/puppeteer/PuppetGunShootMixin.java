package org.agmas.noellesroles.mixin.puppeteer;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 枪击傀儡 / 本体替身：在 Wathe 通过持枪、冷却与掌心雷弹药校验之后、解析目标之前结算 PuppetEntity 目标。
 * 之后 Wathe 按「未命中玩家」继续走完枪声、枪口火光与冷却，不会触发误杀惩罚。
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class PuppetGunShootMixin {
    @Inject(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getEntityById(I)Lnet/minecraft/entity/Entity;",
                    ordinal = 0
            )
    )
    private void noellesroles$shootPuppet(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PuppetEntity puppet)) {
            return;
        }
        // 与 Wathe 相同的 65 格命中上限；白兰地「射程无限制」期间不限
        if (!player.hasStatusEffect(ModEffects.UNLIMITED_RANGE) && player.distanceTo(puppet) >= 65.0F) {
            return;
        }
        PuppeteerControl.onEntityHit(puppet, player, GameConstants.DeathReasons.GUN);
    }
}

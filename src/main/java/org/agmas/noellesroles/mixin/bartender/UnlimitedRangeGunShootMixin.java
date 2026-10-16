package org.agmas.noellesroles.mixin.bartender;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 白兰地「射程无限制」：效果期间跳过服务端 65 格命中上限校验（把距离判定压为 0 恒小于 65）。
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class UnlimitedRangeGunShootMixin {

    @ModifyExpressionValue(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;distanceTo(Lnet/minecraft/entity/Entity;)F"
            )
    )
    private float noellesroles$unlimitedRangeDistance(float original,
                                                      @Local(argsOnly = true) ServerPlayNetworking.Context context) {
        if (context.player().hasStatusEffect(ModEffects.UNLIMITED_RANGE)) {
            return 0.0F;
        }
        return original;
    }
}

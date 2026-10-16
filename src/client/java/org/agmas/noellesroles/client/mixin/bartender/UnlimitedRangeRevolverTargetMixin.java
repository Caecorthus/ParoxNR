package org.agmas.noellesroles.client.mixin.bartender;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 白兰地「射程无限制」：效果期间左轮开火射线由 30 格扩大为 2048 格（getGunTarget 仅客户端调用）。
 */
@Mixin(targets = "dev.doctor4t.wathe.item.RevolverItem")
public abstract class UnlimitedRangeRevolverTargetMixin {

    @ModifyConstant(method = "getGunTarget", constant = @Constant(doubleValue = 30.0D), remap = false)
    private static double noellesroles$unlimitedGunRange(double original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.hasStatusEffect(ModEffects.UNLIMITED_RANGE)) {
            return 2048.0D;
        }
        return original;
    }
}

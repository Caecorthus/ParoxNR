package org.agmas.noellesroles.client.mixin.bartender;

import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 白兰地「射程无限制」：效果期间准星对 30 格外的可见玩家也显示目标样式（renderCrosshair 里左轮那次 30 格判定）。
 */
@Mixin(CrosshairRenderer.class)
public abstract class UnlimitedRangeCrosshairMixin {

    @ModifyConstant(method = "renderCrosshair", constant = @Constant(doubleValue = 30.0D), remap = false)
    private static double noellesroles$unlimitedCrosshairRange(double original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.hasStatusEffect(ModEffects.UNLIMITED_RANGE)) {
            return 2048.0D;
        }
        return original;
    }
}

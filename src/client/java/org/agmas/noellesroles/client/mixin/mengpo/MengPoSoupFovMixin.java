package org.agmas.noellesroles.client.mixin.mengpo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.mengpo.MengPoSoupPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 孟婆汤心跳期：每次心跳时视野轻微收缩再恢复。
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class MengPoSoupFovMixin {
    @Unique
    private static final float PULSE_DURATION_MS = 250.0F;
    @Unique
    private static final float PULSE_STRENGTH = 0.03F;

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void noellesroles$mengPoSoupPulse(CallbackInfoReturnable<Float> cir) {
        MengPoSoupPlayerComponent soup = MengPoSoupPlayerComponent.KEY.get((AbstractClientPlayerEntity) (Object) this);
        if (!soup.isActive()) {
            return;
        }
        float progress = (System.currentTimeMillis() - soup.clientLastBeatMs) / PULSE_DURATION_MS;
        if (progress < 0.0F || progress >= 1.0F) {
            return;
        }
        // 跟随「视场角效果」无障碍滑条缩放，关闭该选项的玩家不会看到跳动
        float effectScale = MinecraftClient.getInstance().options.getFovEffectScale().getValue().floatValue();
        float pulse = MathHelper.sin(progress * MathHelper.PI) * PULSE_STRENGTH * effectScale;
        cir.setReturnValue(cir.getReturnValue() * (1.0F - pulse));
    }
}

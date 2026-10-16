package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import org.agmas.noellesroles.dreameater.CurtainBlackoutHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 降帷：通过 CurtainBlackoutHelper.pendingHalve 标记本次 triggerBlackout 由降帷触发，
 * 在返回后把关灯剩余时长与每盏灯的恢复时间全部减半（15~20秒）。
 * 直接在 triggerBlackout 返回后改写，避免依赖随机数表达式的注入点。
 */
@Mixin(WorldBlackoutComponent.class)
public abstract class CurtainBlackoutMixin {

    @Inject(method = "triggerBlackout", at = @At("RETURN"))
    private void noellesroles$halveCurtainBlackout(CallbackInfoReturnable<Boolean> cir) {
        if (!CurtainBlackoutHelper.pendingHalve) return;
        if (!cir.getReturnValueZ()) return;

        WorldBlackoutAccessor accessor = (WorldBlackoutAccessor) this;
        accessor.noellesroles$setTicks(accessor.noellesroles$getTicks() / 2);
        for (WorldBlackoutComponent.BlackoutDetails detail : accessor.noellesroles$getBlackouts()) {
            BlackoutDetailsAccessor detailAccessor = (BlackoutDetailsAccessor) detail;
            detailAccessor.noellesroles$setTime(detailAccessor.noellesroles$getTime() / 2);
        }
    }
}

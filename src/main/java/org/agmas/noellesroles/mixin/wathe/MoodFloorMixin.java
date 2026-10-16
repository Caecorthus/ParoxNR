package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.util.MoodHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 理智下限从 -1（-100%）压缩为 -0.1（-10%）：
 * setMood 的 clamp 下限与 serverTick 的精神崩溃判定同步调整，其余阈值（0.55/0.2，均为正值判定）不受影响。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class MoodFloorMixin {

    @ModifyConstant(method = "setMood", constant = @Constant(floatValue = -1.0F))
    private float noellesroles$replaceMoodFloor(float original) {
        return MoodHelper.NEGATIVE_MOOD_FLOOR;
    }

    @ModifyConstant(method = "serverTick", constant = @Constant(floatValue = -1.0F))
    private float noellesroles$replaceBreakdownThreshold(float original) {
        return MoodHelper.NEGATIVE_MOOD_FLOOR;
    }
}

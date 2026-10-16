package org.agmas.noellesroles.client.mixin.wathe;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import org.agmas.noellesroles.util.MoodHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 负理智的 HUD 视觉缩放：理智下限被压缩到 -0.1 后，把负数按 |mood| / 0.1 放大回旧的 [-1, 0) 区间，
 * 于是 -0.1 时读条满格、颜色与 HUD 抖动强度等同旧的 -100%。
 * <p>
 * renderHud 内只有这一处 getMood()，其结果写入 moodRender 后驱动读条宽度、颜色、抖动与图标，
 * 因此一处注入即可整体缩放；正数与假心情角色（恒为 1）不受影响。
 */
@Mixin(MoodRenderer.class)
public abstract class MoodNegativeScalingMixin {

    @ModifyExpressionValue(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;getMood()F"
            ),
            remap = false
    )
    private static float noellesroles$scaleNegativeMoodForDisplay(float original) {
        if (original >= 0f) {
            return original;
        }
        return Math.max(-1f, original / Math.abs(MoodHelper.NEGATIVE_MOOD_FLOOR));
    }
}

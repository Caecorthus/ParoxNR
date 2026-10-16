package org.agmas.noellesroles.client.mixin.wathe;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 心情值条长度上限固定为 92px（宽度目标 100，与「无任务时」的现有默认一致），
 * 不再随最长任务文本宽度变化。条长 = (moodTextWidth - 8) * |mood|，平民条与杀手条共用该宽度。
 * renderHud 中宽度目标有两个分支：有任务=最长任务文本宽度、无任务=100f；
 * 此注入把有任务分支的取值改为 100，使两分支一致（lerp 平滑并入的动画保留）。
 */
@Mixin(MoodRenderer.class)
public abstract class MoodBarFixedWidthMixin {

    @ModifyExpressionValue(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/StringVisitable;)I"
            )
    )
    private static int noellesroles$fixedMoodBarWidth(int original) {
        return 100;
    }
}

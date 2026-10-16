package org.agmas.noellesroles.client.mixin.dreameater;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 持有噬梦者睡觉任务（被入梦标记后获得，{@code DREAM_SLEEP} 槽位）的玩家，
 * 平民心情进度条在**正数心情段**恒为纯亮红，不再随心情变绿 —— 作为「被盯上」的常驻警示。
 * <p>
 * 只改 {@code renderCivilian} 里第一个 {@code hsvToRgb} 调用（正数分支的条色，ordinal=0）；
 * 负数崩溃条（深红→黑紫渐变 + 死亡警告）刻意保留，颜色仍能区分正负心情。
 * 假心情/杀手条本来就恒为暗红，不受影响；精神崩溃模式的渲染不走这条分支。
 * <p>
 * 返回值只含颜色位：调用点是 {@code hsvToRgb(...) | (moodAlpha << 24)}，
 * 透明度由 wathe 后续的或运算补上。
 */
@Mixin(MoodRenderer.class)
public abstract class DreamTaskMoodBarColorMixin {

    @ModifyExpressionValue(
            method = "renderCivilian",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;hsvToRgb(FFF)I",
                    ordinal = 0
            )
    )
    private static int noellesroles$forceRedForDreamTask(int original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return original;
        }
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(client.player);
        for (PlayerMoodComponent.Task task : mood.tasks.keySet()) {
            if (NoellesTaskTypes.isDreamSleepTask(task)) {
                return MathHelper.hsvToRgb(0F, 1.0F, 1.0F);
            }
        }
        return original;
    }
}

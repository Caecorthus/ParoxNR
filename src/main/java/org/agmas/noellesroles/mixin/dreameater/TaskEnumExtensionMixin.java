package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给 wathe 的 {@code PlayerMoodComponent$Task} 追加本模组的任务槽位。
 * <p>
 * wathe 的枚举 {@code <clinit>} 末尾是 {@code $VALUES = $values()}，这里在 TAIL 之后
 * 把 {@code $VALUES} 换成「原版 4 个 + 本模组注册的类型」，于是在**枚举类自己的静态初始化里**
 * 就完成了扩展 —— 之后任何 {@code Task.values()} 调用（HUD 遍历、NBT/同步包的
 * {@code ordinal < values().length} 边界检查、{@code $values()} 本身）看到的都是扩展后的槽位。
 * <p>
 * 用 TAIL 而不是 RETURN 的具体返回位置，是为了在有多个 return 时仍然只在正常出口注入一次。
 * 反序列化侧不需要改 wathe 代码：新槽位的 {@code setFunction} 由
 * {@link NoellesTaskTypes#extendEnum} 反射装好，vanilla 的
 * {@code readFromNbt} / {@code applySyncPacket} 会自动构造我们的任务。
 * <p>
 * 客户端还有一处例外见 {@code ClientMoodTaskDummyMixin}（{@code createDummyTask} 的 switch）。
 */
@Mixin(PlayerMoodComponent.Task.class)
public abstract class TaskEnumExtensionMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void noellesroles$extendTaskSlots(CallbackInfo ci) {
        PlayerMoodComponentTaskAccessor.noellesroles$setValues(
                NoellesTaskTypes.extendEnum(PlayerMoodComponent.Task.values()));
    }
}

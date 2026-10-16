package org.agmas.noellesroles.mixin.dreameater;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

/**
 * 把本模组追加的任务槽位排除在 wathe 的**随机任务生成**之外。
 *
 * <p>为什么必须做：{@code PlayerMoodComponent.generateTask()} 的流程是
 * 「遍历 {@code Task.values()} 收集权重（{@code timesGotten}）→ 按权重抽一个 →
 * {@code switch (task.ordinal())} 构造对应的 TrainTask」。
 * 枚举被扩展后 {@code values()} 多出了我们的槽位，于是：
 * <ol>
 *   <li>抽到新槽位时走进 switch 的 {@code default: throw new MatchException()} ——
 *       <b>2026-10-11 实测崩服</b>（服务端 tick 玩家时崩，栈为
 *       {@code generateTask(PlayerMoodComponent.java:232) ← serverTick}）。注意这是
 *       {@code generateTask} 里的 switch，与客户端反序列化的 {@code createDummyTask} 是两个不同的 switch；</li>
 *   <li>即使不崩，也会让**原版随机任务**发出噬梦者的专属任务（自定义任务应由对应角色/效果注入）。</li>
 * </ol>
 *
 * <p>做法：把这个方法里唯一那次 {@code Task.values()} 的返回值换成「只含原版槽位」的数组。
 * 一处注入同时解决上面两个问题：候选池里没有自定义槽位，switch 永远收不到新 ordinal，
 * {@code timesGotten} 也不会被塞进自定义槽位。
 *
 * <p>新槽位的「手动注入路径」（{@code DreamEaterPlayerComponent.resolveMark} →
 * {@code mood.tasks.put(DREAM_SLEEP.slot(), …)}）不受影响；
 * 客户端反序列化仍由 {@code ClientMoodTaskDummyMixin} 负责。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class MoodTaskGenerationExclusionMixin {

    @ModifyExpressionValue(
            method = "generateTask",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent$Task;values()[Ldev/doctor4t/wathe/cca/PlayerMoodComponent$Task;"
            )
    )
    private PlayerMoodComponent.Task[] noellesroles$excludeCustomTaskSlots(PlayerMoodComponent.Task[] original) {
        int vanillaCount = NoellesTaskTypes.vanillaCount();
        if (original.length <= vanillaCount) {
            return original; // 枚举没被扩展（理论上不会发生），保持原样
        }
        // 原版槽位永远是前 vanillaCount 个（自定义槽位只追加在末尾，见 NoellesTaskTypes 的铁律）。
        return Arrays.copyOf(original, vanillaCount);
    }
}

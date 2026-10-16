package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 噬梦者睡觉任务完成时**不给理智回复**。
 * <p>
 * wathe {@code PlayerMoodComponent.serverTick} 里任务完成的处理是：
 * 把完成的任务加进待移除列表 → {@code setMood(mood + 0.5f)} → 发 TaskComplete 包 → 触发
 * {@code TaskComplete.EVENT} → 循环结束后统一从表里移除。
 * 本模组要保留后半段（噬梦者的死亡结算就挂在 {@code TaskComplete.EVENT} 上），
 * 只把心情回复那一项压掉。
 * <p>
 * 做法：重定向该方法里**第 2 处** {@code setMood} 调用（ordinal=1，即任务完成分支里的那次；
 * ordinal=0 是方法开头的「按任务数扣理智」）。判定条件是「当前 SLEEP 槽位上是噬梦者的睡觉任务」——
 * 此刻它必然就是刚刚完成的那个任务（移除要等循环结束），所以这个条件与「本次完成的是噬梦者任务」等价，
 * 且不需要改 {@code isFulfilled} / 不依赖任何跨组件加载顺序。
 * <p>
 * 没有心情机制的角色（杀手/中立）在 vanilla 里本来就不加心情，本 mixin 对它们无副作用。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class MoodDreamTaskRewardMixin {

    @Shadow public Map<PlayerMoodComponent.Task, PlayerMoodComponent.TrainTask> tasks;

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V", ordinal = 1)
    )
    private void noellesroles$skipDreamTaskMoodGain(PlayerMoodComponent mood, float value) {
        if (isFulfillingDreamSleepTask()) {
            return; // 噬梦者任务：不回复心情
        }
        mood.setMood(value);
    }

    private boolean isFulfillingDreamSleepTask() {
        PlayerMoodComponent.TrainTask sleepTask = this.tasks.get(PlayerMoodComponent.Task.SLEEP);
        return sleepTask != null && NoellesTaskTypes.isCustom(sleepTask.getType());
    }
}

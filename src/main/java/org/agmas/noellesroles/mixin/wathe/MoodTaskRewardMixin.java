package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.util.MoodHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Map;

/**
 * 任务回复递减：完成一个任务时，回复量按当前仍未完成（不含刚刚完成）的任务数量缩减，
 * 1 个 -20%、2 个 -40%、3 个及以上 -80%（封顶）。
 * <p>
 * 原版回复写死为内联常量 MOOD_GAIN = 0.5f（任务奖励在循环内直接累加），因此这里改 0.5f 常量本身。
 * 已完成的移除在循环结束后才统一执行，所以本次完成与同 tick 内其它已完成的任务天然被 isFulfilled 排除。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class MoodTaskRewardMixin {

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    @Final
    public Map<PlayerMoodComponent.Task, PlayerMoodComponent.TrainTask> tasks;

    @ModifyConstant(method = "serverTick", constant = @Constant(floatValue = 0.5F))
    private float noellesroles$scaleTaskMoodGain(float original) {
        int unfinished = 0;
        for (PlayerMoodComponent.TrainTask task : this.tasks.values()) {
            // 「去修复破坏点。」由破坏点状态强制注入且无法经任务系统完成，不计入递减档（否则破坏点存在期间其它任务回复永远被扣到最高档）
            if (task instanceof org.agmas.noellesroles.task.RepairBreachTask) {
                continue;
            }
            if (!task.isFulfilled(this.player)) {
                unfinished++;
            }
        }
        return original * (1f - MoodHelper.getTaskRewardReduction(unfinished));
    }
}

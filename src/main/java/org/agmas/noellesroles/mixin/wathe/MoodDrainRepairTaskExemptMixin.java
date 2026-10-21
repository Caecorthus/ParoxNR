package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.task.RepairBreachTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * 「去修复破坏点。」任务不计入理智下降的任务数倍率。
 *
 * <p>wathe 的理智消耗为 {@code tasks.size() * MOOD_DRAIN}（serverTick 真实心情处
 * 与 clientTick 各一次，均为方法内第一处 {@code Map.size()}），任务越多掉得越快。
 * 修复任务由破坏点存在与否强制注入/移除，玩家无法主动完成，
 * 因此其存在不应加速理智下降：在消耗点把该任务从计数中扣除。
 *
 * <p>只改 drain 的 size()（ordinal 0）；刷新间隔惩罚（extraInterval 处的第二个
 * size()）不动，保持原版「任务多则新任务来得慢」的行为。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class MoodDrainRepairTaskExemptMixin {

    @ModifyExpressionValue(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I", ordinal = 0))
    private int noellesroles$exemptFromMoodDrain(int size) {
        return noellesroles$countWithoutRepairTask(size);
    }

    @ModifyExpressionValue(
            method = "clientTick",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I", ordinal = 0))
    private int noellesroles$exemptFromMoodDrainClient(int size) {
        return noellesroles$countWithoutRepairTask(size);
    }

    private int noellesroles$countWithoutRepairTask(int size) {
        PlayerMoodComponent self = (PlayerMoodComponent) (Object) this;
        int exempt = 0;
        for (PlayerMoodComponent.TrainTask task : self.tasks.values()) {
            if (task instanceof RepairBreachTask) {
                exempt++;
            }
        }
        return Math.max(0, size - exempt);
    }
}

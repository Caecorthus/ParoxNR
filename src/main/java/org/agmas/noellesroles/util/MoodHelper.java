package org.agmas.noellesroles.util;

/**
 * 理智（mood）系统调整的公共常量：
 * 负理智区间由 [-1, 0) 压缩为 [-0.1, 0)（到达下限即精神崩溃），
 * 任务回复量随当前仍未完成的任务数量递减。
 */
public final class MoodHelper {
    /** 理智下限（-10%），到达即触发原版精神崩溃死亡。 */
    public static final float NEGATIVE_MOOD_FLOOR = -0.1f;

    /** 每个未完成任务带来的回复缩减比例。 */
    public static final float TASK_REWARD_REDUCTION_STEP = 0.2f;

    /** 回复缩减上限（至少保留 20% 的回复量）。 */
    public static final float MAX_TASK_REWARD_REDUCTION = 0.8f;

    private MoodHelper() {}

    /** 按未完成任务数计算回复缩减比例（0~0.8）。 */
    public static float getTaskRewardReduction(int unfinishedTaskCount) {
        return Math.min(MAX_TASK_REWARD_REDUCTION, unfinishedTaskCount * TASK_REWARD_REDUCTION_STEP);
    }
}

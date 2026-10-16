package org.agmas.noellesroles.task;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * 噬梦者注入的睡觉任务：与 wathe 原版 {@code SleepTask} 行为一致（睡着时倒计时），
 * 但占用的是本模组追加的独立枚举槽位 {@link NoellesTaskTypes#DREAM_SLEEP}，
 * 因此可以与原版「去睡一会儿。」任务**同时存在**，且 HUD 会为它单独渲染一行。
 *
 * <p>文案：HUD（{@code MoodRenderer$TaskRenderer.tick}）会用
 * {@code task.feel} + {@code task.}{@link #getName()} 拼出显示名，
 * 所以这里返回 {@code "dream_sleep"} → 语言键 {@code task.dream_sleep}
 * =「你被噬梦者盯上了：去睡觉！」。
 *
 * <p>完成奖励：本模组另有一个 mixin 把该任务完成时的理智回复压掉
 * （见 {@code mixin/dreameater/MoodDreamTaskRewardMixin}）；原版任务奖励流程
 * （TaskComplete 网络包 + {@code TaskComplete.EVENT}）照常触发，
 * 噬梦者的死亡结算就挂在那个事件里。
 */
public class DreamSleepTask implements PlayerMoodComponent.TrainTask {

    /** 续期用的默认时长，与 wathe 原版睡觉任务一致（{@code GameConstants.SLEEP_TASK_DURATION}）。 */
    private int timer;

    public DreamSleepTask(int timer) {
        this.timer = timer;
    }

    /** vanilla NBT 反序列化入口（通过新槽位的 {@code setFunction} 被调用）。 */
    public static DreamSleepTask fromNbt(NbtCompound tag) {
        return new DreamSleepTask(tag.getInt("timer"));
    }

    @Override
    public void tick(PlayerEntity player) {
        if (player.isSleeping() && this.timer > 0) {
            this.timer--;
        }
    }

    @Override
    public boolean isFulfilled(PlayerEntity player) {
        return this.timer <= 0;
    }

    @Override
    public String getName() {
        return "dream_sleep";
    }

    @Override
    public PlayerMoodComponent.Task getType() {
        return NoellesTaskTypes.DREAM_SLEEP.slot();
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("type", getType().ordinal());
        tag.putInt("timer", this.timer);
        return tag;
    }
}

package org.agmas.noellesroles.task;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * 「去修复破坏点。」：破袭者放置破坏点后由 {@code BreacherWorldComponent} 注入给全体
 * 真实/假心情玩家的任务；破坏点全部修复后统一移除。
 *
 * <p>与普通任务的区别：
 * <ul>
 *   <li>不会被 wathe 的随机任务刷新选中（见 {@code MoodTaskGenerationExclusionMixin}，
 *       该机制对本模组所有自定义槽位通用）；</li>
 *   <li>{@link #isFulfilled} 恒为 false —— 它不经任务系统完成，没有完成奖励，
 *       其存在也不参与理智下降倍率（见 {@code MoodDrainRepairTaskExemptMixin}）；</li>
 *   <li>文案沿用原版前缀机制：好人 = task.feel + task.repair_breach，
 *       假任务持有者 = task.fake + task.repair_breach。</li>
 * </ul>
 */
public class RepairBreachTask implements PlayerMoodComponent.TrainTask {

    /** vanilla NBT 反序列化入口（通过新槽位的 setFunction 被调用）。 */
    public static RepairBreachTask fromNbt(NbtCompound tag) {
        return new RepairBreachTask();
    }

    @Override
    public void tick(PlayerEntity player) {
    }

    @Override
    public boolean isFulfilled(PlayerEntity player) {
        return false;
    }

    @Override
    public String getName() {
        return "repair_breach";
    }

    @Override
    public PlayerMoodComponent.Task getType() {
        return NoellesTaskTypes.REPAIR_BREACH.slot();
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("type", getType().ordinal());
        return tag;
    }
}

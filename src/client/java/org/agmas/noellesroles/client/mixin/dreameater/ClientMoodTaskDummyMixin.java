package org.agmas.noellesroles.client.mixin.dreameater;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端假任务构造：支持本模组追加的任务槽位。
 * <p>
 * wathe 的 {@code createDummyTask} 是一段 {@code tableswitch(SLEEP..DRINK)}
 * + {@code default: throw new MatchException()}。同步包只传 ordinal，
 * {@code applySyncPacket} 的边界检查在枚举被扩展后允许 ordinal≥4，
 * 于是客户端会拿着新槽位进到这里 —— 不拦就会在 {@code default} 抛
 * {@link MatchException} 直接崩客户端。
 * <p>
 * 这里在 HEAD 拦掉「不属于 vanilla 的槽位」，其余情况一律放行走原版逻辑。
 * dummy 的真实构造委托给 {@link NoellesTaskTypes#createDummy}，
 * 由注册表决定各类型该造什么（这样以后新增任务类型不用再改这个 mixin）。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class ClientMoodTaskDummyMixin {

    @Inject(method = "createDummyTask", at = @At("HEAD"), cancellable = true)
    private void noellesroles$createCustomDummyTask(PlayerMoodComponent.Task task,
                                                    CallbackInfoReturnable<PlayerMoodComponent.TrainTask> cir) {
        if (task == null || task.ordinal() < NoellesTaskTypes.vanillaCount()) {
            return; // vanilla 槽位，交给原版 switch
        }
        if (!NoellesTaskTypes.isCustom(task)) {
            return; // 未知槽位（其它模组追加的），保持原版行为
        }
        cir.setReturnValue(NoellesTaskTypes.createDummy(task));
    }
}

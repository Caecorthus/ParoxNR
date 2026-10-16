package org.agmas.noellesroles.mixin.dualrole;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import org.agmas.noellesroles.util.MatchRecordTimeShift;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * 双角色选择期间对局时钟被冻结、世界刻却照走，回放时间戳（事件刻 − 起始刻）会把选择那几秒算进去。
 * 选完后把起始刻整体后移到选择结束时刻，并把窗口内（含开局瞬间写入的 match_start/角色快照）
 * 的事件压到该刻，让回放计时从 0:00 开始读。
 */
@Mixin(GameRecordManager.MatchRecord.class)
public abstract class MatchRecordTimeShiftMixin implements MatchRecordTimeShift {

    @Mutable
    @Shadow
    private long startTick;

    @Shadow
    @Final
    private List<GameRecordEvent> events;

    @Override
    public void noellesroles$shiftStartTick(long delta) {
        this.startTick += delta;
    }

    @Override
    public void noellesroles$collapseEventsBefore(long tick) {
        for (int i = 0; i < this.events.size(); i++) {
            GameRecordEvent event = this.events.get(i);
            if (event.worldTick() <= tick) {
                this.events.set(i, new GameRecordEvent(
                        event.matchId(),
                        event.seq(),
                        event.type(),
                        tick,
                        event.realTimeMs(),
                        event.data()
                ));
            }
        }
    }
}

package org.agmas.noellesroles.util;

/**
 * 由 {@code org.agmas.noellesroles.mixin.dualrole.MatchRecordTimeShiftMixin} 织入到
 * Wathe 的 {@code GameRecordManager.MatchRecord} 上，用于在选完角色后平移回放时间基准。
 */
public interface MatchRecordTimeShift {

    /** 把回放起始刻向后平移 delta（世界刻）。 */
    void noellesroles$shiftStartTick(long delta);

    /** 把 tick 之前（含）记录的事件统一改写到 tick，避免选择期间的事件出现负时间戳。 */
    void noellesroles$collapseEventsBefore(long tick);
}

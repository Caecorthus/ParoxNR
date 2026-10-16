package org.agmas.noellesroles.dreameater;

/**
 * 降帷触发时置位；WorldBlackoutComponent.triggerBlackout 的 mixin 读取后
 * 将本次关灯的时长减半（灯光恢复、失明、倒计时保持一致）。
 */
public final class CurtainBlackoutHelper {
    public static boolean pendingHalve = false;

    private CurtainBlackoutHelper() {}
}

package org.agmas.noellesroles.client.economist;

import dev.doctor4t.wathe.client.gui.StoreRenderer;

/**
 * 客户端《经济财报》观测状态：服务端定期推送目标余额与剩余时间，
 * 右上角在金币行下方另起一行显示。
 */
public final class EconomistClientState {
    /** 金币行占用 y 6~17，观测行从 18 开始。 */
    public static final int OBSERVATION_LINE_Y = 18;
    /** 观测行显示时，状态效果图标需要再下移的高度。 */
    public static final int OBSERVATION_LINE_HEIGHT = 12;

    // 复用 Wathe 的金币数字渲染器，但不能共用 StoreRenderer.view（会打乱本机金币动画）
    private static final StoreRenderer.MoneyNumberRenderer OBSERVED_VIEW = new StoreRenderer.MoneyNumberRenderer();
    private static int observedBalance;
    private static int remainingTicks;

    private EconomistClientState() {}

    public static void onReport(int balance, int ticks) {
        observedBalance = balance;
        remainingTicks = ticks;
        OBSERVED_VIEW.setTarget(balance);
    }

    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
        OBSERVED_VIEW.update();
    }

    public static boolean isObserving() {
        return remainingTicks > 0;
    }

    public static int getObservedBalance() {
        return observedBalance;
    }

    public static StoreRenderer.MoneyNumberRenderer view() {
        return OBSERVED_VIEW;
    }

    public static void reset() {
        observedBalance = 0;
        remainingTicks = 0;
    }
}

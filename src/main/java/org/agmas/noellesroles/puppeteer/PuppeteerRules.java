package org.agmas.noellesroles.puppeteer;

/** 傀儡师的纯数值规则（不依赖游戏对象，便于单测）。 */
public final class PuppeteerRules {
    public static final int PUPPET_DEATH_COIN_PENALTY = 150;
    public static final int PASSIVE_MONEY_CAP = 300;

    private PuppeteerRules() {}

    /** 一局可制造的傀儡数：本局杀手数量，至少 1。 */
    public static int maxPuppets(int killerCount) {
        return Math.max(1, killerCount);
    }

    /**
     * 傀儡倒地后傀儡师是否连带死亡：巫毒导致的倒地一律连带；
     * 否则仅当名额已用完、且再没有存活傀儡（库存 + 场上）时连带。
     */
    public static boolean shouldChainDie(boolean byVoodoo, int madeCount, int maxPuppets, int alivePuppets) {
        return byVoodoo || (madeCount >= maxPuppets && alivePuppets <= 0);
    }

    /** 傀儡倒地扣除的金币：150，不足则扣到 0。 */
    public static int coinPenalty(int balance) {
        return Math.min(PUPPET_DEATH_COIN_PENALTY, Math.max(0, balance));
    }

    /** 被动收入判定用的余额：把原版上限 vanillaCap 等效抬到 300。 */
    public static int passiveIncomeComparableBalance(int balance, int vanillaCap) {
        return balance - (PASSIVE_MONEY_CAP - vanillaCap);
    }
}

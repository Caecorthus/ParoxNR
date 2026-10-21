package org.agmas.noellesroles.puppeteer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuppeteerRulesTest {

    @Test
    void maxPuppetsEqualsKillerCountButAtLeastOne() {
        assertEquals(1, PuppeteerRules.maxPuppets(0));
        assertEquals(1, PuppeteerRules.maxPuppets(1));
        assertEquals(3, PuppeteerRules.maxPuppets(3));
    }

    @Test
    void lastPuppetFallingKillsPuppeteerOnlyWhenQuotaUsedUp() {
        // 名额 2：做了 1 具、它倒下 → 还能再做，不连带
        assertFalse(PuppeteerRules.shouldChainDie(false, 1, 2, 0));
        // 名额用完但还有一具活着 → 不连带
        assertFalse(PuppeteerRules.shouldChainDie(false, 2, 2, 1));
        // 名额用完且再无存活傀儡 → 连带死亡
        assertTrue(PuppeteerRules.shouldChainDie(false, 2, 2, 0));
    }

    @Test
    void voodooCurseAlwaysKillsPuppeteer() {
        assertTrue(PuppeteerRules.shouldChainDie(true, 1, 3, 2));
    }

    @Test
    void puppetDeathPenaltyNeverGoesBelowZero() {
        assertEquals(150, PuppeteerRules.coinPenalty(400));
        assertEquals(90, PuppeteerRules.coinPenalty(90));
        assertEquals(0, PuppeteerRules.coinPenalty(0));
        assertEquals(0, PuppeteerRules.coinPenalty(-20));
    }

    @Test
    void passiveIncomeCapIsRaisedTo300() {
        int vanillaCap = 200;
        // 原版判定：余额 < 200 才发被动收入；傀儡师把比较值下移 100，即余额 < 300 才发
        assertTrue(PuppeteerRules.passiveIncomeComparableBalance(250, vanillaCap) < vanillaCap);
        assertTrue(PuppeteerRules.passiveIncomeComparableBalance(299, vanillaCap) < vanillaCap);
        assertFalse(PuppeteerRules.passiveIncomeComparableBalance(300, vanillaCap) < vanillaCap);
    }
}

package org.agmas.noellesroles.killerhistory;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillerHistoryCalculatorTest {

    @Test
    void weightBoundaries() {
        // 无历史：1.0
        assertEquals(1.0, KillerHistoryCalculator.computeKillerWeight(0, 0), 1e-9);
        // 6 连空：封顶 2.5
        assertEquals(2.5, KillerHistoryCalculator.computeKillerWeight(6, 0), 1e-9);
        assertEquals(2.5, KillerHistoryCalculator.computeKillerWeight(100, 0), 1e-9);
        // 近 6 场全杀手：下限 0.5
        assertEquals(0.5, KillerHistoryCalculator.computeKillerWeight(0, 6), 1e-9);
        // 中间值：连空 2 且近 6 场杀手 2 → 1 + 0.5 - 0.3 = 1.2
        assertEquals(1.2, KillerHistoryCalculator.computeKillerWeight(2, 2), 1e-9);
        // 连空 1 → 1.25；连空 5 → 2.25（未封顶）
        assertEquals(1.25, KillerHistoryCalculator.computeKillerWeight(1, 0), 1e-9);
        assertEquals(2.25, KillerHistoryCalculator.computeKillerWeight(5, 0), 1e-9);
    }

    @Test
    void pickCountsAreCorrectAndDistinct() {
        List<UUID> candidates = uuids(12);
        Map<UUID, Double> weights = new HashMap<>();
        for (UUID u : candidates) weights.put(u, 1.0);

        List<UUID> picked = KillerHistoryCalculator.pickWeightedPlayers(candidates, weights, 2, new Random(42));
        assertEquals(2, picked.size());
        assertEquals(picked.size(), picked.stream().distinct().count());

        // 抽取数量超过候选数时只取全部候选
        List<UUID> all = KillerHistoryCalculator.pickWeightedPlayers(candidates, weights, 99, new Random(42));
        assertEquals(12, all.size());
        assertEquals(12, all.stream().distinct().count());
    }

    /**
     * 蒙特卡洛：12 人局抽 2 个杀手，其中 1 人 6 连空（权重 2.5），其余权重 1.0。
     * 该玩家被抽中的概率应在 ~33% ± 2% 内（基础均匀概率为 2/12 ≈ 16.7%）。
     */
    @Test
    void monteCarloSixStreakGetsBoosted() {
        final int trials = 200_000;
        UUID lucky = UUID.randomUUID();
        List<UUID> candidates = new ArrayList<>();
        candidates.add(lucky);
        for (int i = 0; i < 11; i++) candidates.add(UUID.randomUUID());

        Map<UUID, Double> weights = new HashMap<>();
        for (UUID u : candidates) weights.put(u, u.equals(lucky) ? 2.5 : 1.0);

        Random random = new Random(12345);
        int luckyPicked = 0;
        for (int t = 0; t < trials; t++) {
            if (KillerHistoryCalculator.pickWeightedPlayers(candidates, weights, 2, random).contains(lucky)) {
                luckyPicked++;
            }
        }
        double rate = (double) luckyPicked / trials;
        assertTrue(rate > 0.31 && rate < 0.35,
                "6连空玩家被抽中率应约为33%，实际 " + String.format("%.3f", rate));
    }

    private static List<UUID> uuids(int count) {
        List<UUID> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(UUID.randomUUID());
        return list;
    }
}

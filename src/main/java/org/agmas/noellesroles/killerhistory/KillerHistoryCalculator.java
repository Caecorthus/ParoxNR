package org.agmas.noellesroles.killerhistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 杀手阵营动态权重计算器（纯函数，无 Minecraft 依赖，便于单元测试）。
 * 权重 = clamp(1 + 0.25×连空场次 − 0.15×近6场杀手次数, 0.5, 2.5)
 */
public final class KillerHistoryCalculator {
    public static final double MIN_WEIGHT = 0.5;
    public static final double MAX_WEIGHT = 2.5;
    public static final int HISTORY_WINDOW = 6;
    private static final double STREAK_FACTOR = 0.25;
    private static final double RECENT_KILLER_PENALTY = 0.15;

    private KillerHistoryCalculator() {
    }

    public static double computeKillerWeight(int consecutiveNonKillerGames, int killerGamesLast6) {
        double weight = 1.0
                + STREAK_FACTOR * consecutiveNonKillerGames
                - RECENT_KILLER_PENALTY * killerGamesLast6;
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weight));
    }

    /**
     * 带权重不放回抽样：从 candidates 中按 weights 给定的概率抽 count 个。
     * 权重缺失的候选按 1.0 计；权重低于 0.001 时按 0.001 兜底避免零概率。
     */
    public static List<UUID> pickWeightedPlayers(List<UUID> candidates, Map<UUID, Double> weights, int count, Random random) {
        List<UUID> pool = new ArrayList<>(candidates);
        List<UUID> picked = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            double total = 0;
            for (UUID u : pool) {
                total += weightOf(u, weights);
            }
            double r = random.nextDouble() * total;
            double acc = 0;
            UUID chosen = pool.get(0);
            for (UUID u : pool) {
                acc += weightOf(u, weights);
                if (r <= acc) {
                    chosen = u;
                    break;
                }
            }
            pool.remove(chosen);
            picked.add(chosen);
        }
        return picked;
    }

    private static double weightOf(UUID uuid, Map<UUID, Double> weights) {
        Double w = weights.get(uuid);
        return w == null ? 1.0 : Math.max(0.001, w);
    }
}

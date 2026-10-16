package org.agmas.noellesroles.rolehistory;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleHistoryCalculatorTest {

    private static final Function<String, String> IDENTITY = s -> s;

    @Test
    void pushToWindowKeepsOnlyLastThree() {
        ArrayDeque<String> window = new ArrayDeque<>();
        RoleHistoryCalculator.pushToWindow(window, "hunter");
        RoleHistoryCalculator.pushToWindow(window, "bomber");
        RoleHistoryCalculator.pushToWindow(window, "assassin");
        assertEquals(List.of("hunter", "bomber", "assassin"), new ArrayList<>(window));

        RoleHistoryCalculator.pushToWindow(window, "conductor");
        assertEquals(List.of("bomber", "assassin", "conductor"), new ArrayList<>(window));
    }

    @Test
    void pushToWindowCountsDuplicateRolesByGame() {
        ArrayDeque<String> window = new ArrayDeque<>();
        RoleHistoryCalculator.pushToWindow(window, "hunter");
        RoleHistoryCalculator.pushToWindow(window, "hunter");
        RoleHistoryCalculator.pushToWindow(window, "hunter");
        assertEquals(List.of("hunter", "hunter", "hunter"), new ArrayList<>(window));

        // 第 4 局仍是 hunter：应淘汰最旧的一局，仍保留 3 条
        RoleHistoryCalculator.pushToWindow(window, "hunter");
        assertEquals(3, window.size());
    }

    @Test
    void pickAndRemoveAvoidsRecentWhileAlternativesExist() {
        List<String> pool = new ArrayList<>(List.of("hunter", "bomber", "assassin", "serial_killer"));
        Set<String> recent = new HashSet<>(List.of("hunter"));

        // 多轮固定随机源：只要池里还有非 recent 角色，就绝不能抽中 recent 角色
        for (int i = 0; i < 200; i++) {
            List<String> round = new ArrayList<>(pool);
            String picked = RoleHistoryCalculator.pickAndRemove(round, recent, IDENTITY, new Random(i));
            assertFalse(recent.contains(picked), "存在替代角色时不应抽中近3局角色，抽中 " + picked);
        }
    }

    @Test
    void pickAndRemoveFallsBackToFullPoolWhenOnlyRecentRemain() {
        // 池全部在该玩家近3局内：回退全池，仍必须返回池中元素（允许重复）
        List<String> pool = new ArrayList<>(List.of("hunter", "bomber", "assassin"));
        Set<String> recent = new HashSet<>(pool);

        String picked = RoleHistoryCalculator.pickAndRemove(new ArrayList<>(pool), recent, IDENTITY, new Random(1));
        assertTrue(pool.contains(picked));
    }

    @Test
    void pickAndRemoveFallsBackWhenAllowedSubsetEmpty() {
        // 近3局覆盖部分池：随抽取推进,池最终只剩 recent 角色时回退,且每个元素最多被抽一次
        List<String> pool = new ArrayList<>(List.of("hunter", "bomber", "assassin"));
        Set<String> recent = new HashSet<>(List.of("hunter", "bomber"));

        List<String> round = new ArrayList<>(pool);
        List<String> picked = new ArrayList<>();
        Random rng = new Random(7);
        while (!round.isEmpty()) {
            picked.add(RoleHistoryCalculator.pickAndRemove(round, recent, IDENTITY, rng));
        }
        assertEquals(pool.size(), picked.size());
        assertEquals(picked.size(), picked.stream().distinct().count(), "每个角色本局只能被抽取一次");
    }

    @Test
    void pickAndRemoveRemovesChosenElementFromPool() {
        List<String> pool = new ArrayList<>(List.of("hunter", "bomber", "assassin"));

        String picked = RoleHistoryCalculator.pickAndRemove(pool, Set.of(), IDENTITY, new Random(3));
        assertEquals(2, pool.size());
        assertFalse(pool.contains(picked));
    }

    @Test
    void pickAndRemoveRejectsEmptyPool() {
        assertThrows(IllegalArgumentException.class,
                () -> RoleHistoryCalculator.pickAndRemove(new ArrayList<>(), Set.of(), IDENTITY, new Random(0)));
    }
}

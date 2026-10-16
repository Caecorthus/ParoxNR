package org.agmas.noellesroles.rolehistory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * 纯逻辑:维护"最近3局已完结局使用过的角色"旋转窗口,并提供带排除的池抽取。
 * 不依赖 wathe/MC 运行时类型,便于 JUnit 直测。
 */
public final class RoleHistoryCalculator {
    public static final int HISTORY_WINDOW = 3;

    private RoleHistoryCalculator() {
    }

    /**
     * 旋转窗口:新值入队,超出 HISTORY_WINDOW 丢弃最旧(按"局"计,允许重复值)。
     */
    public static <T> void pushToWindow(ArrayDeque<T> window, T value) {
        window.addLast(value);
        while (window.size() > HISTORY_WINDOW) {
            window.removeFirst();
        }
    }

    /**
     * 从"单次用完即删"的池中抽取一个值:优先取 key 不在 recentKeys 中的候选项;
     * 若排除后无候选(此时按 vanilla 语义只会落到无技能平民或干脆不分配),
     * 回退到全池,允许重复。返回值会从 pool 中移除。前置条件:pool 非空。
     */
    public static <T, K> T pickAndRemove(List<T> pool, Collection<K> recentKeys, Function<T, K> keyOf, Random random) {
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("pool must not be empty");
        }
        List<T> allowed = new ArrayList<>();
        for (T candidate : pool) {
            if (!recentKeys.contains(keyOf.apply(candidate))) {
                allowed.add(candidate);
            }
        }
        List<T> source = allowed.isEmpty() ? pool : allowed;
        T chosen = source.get(random.nextInt(source.size()));
        pool.remove(chosen);
        return chosen;
    }
}

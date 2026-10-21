package org.agmas.noellesroles.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 记录本局被 /wathe:forceRole 强制指定角色的玩家。
 * ScoreboardRoleSelectorComponent.assignForcedRoles 发放后会立即清空内部 forcedRoles 表，
 * 因此在 HEAD mixin 中提前留档；警类名额分配、列车长保底与开局双选据此跳过这些玩家。
 */
public final class ForcedRoleTracker {
    private static final Set<UUID> FORCED_PLAYERS = new HashSet<>();

    private ForcedRoleTracker() {
    }

    /** 整局替换记录（每次开局 assignForcedRoles 调用时刷新，空强制表即清空） */
    public static void record(Collection<UUID> players) {
        FORCED_PLAYERS.clear();
        FORCED_PLAYERS.addAll(players);
    }

    public static boolean isForced(UUID uuid) {
        return FORCED_PLAYERS.contains(uuid);
    }

    /** 对局结束时清理，防止跨局残留 */
    public static void clear() {
        FORCED_PLAYERS.clear();
    }
}

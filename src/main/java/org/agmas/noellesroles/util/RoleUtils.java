package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.List;
import java.util.Random;
import java.util.Set;

public final class RoleUtils {
    private RoleUtils() {}

    /**
     * 互斥角色对：不可同局共存。
     * 同阶段抽取的所有出场条件在分配前统一求值，isRoleAssigned 无法感知同阶段角色，
     * 因此互斥必须在候选池阶段显式消解。
     */
    public static final List<RolePair> MUTUALLY_EXCLUSIVE_PAIRS = List.of(
            new RolePair(Noellesroles.CORRUPT_COP, Noellesroles.GANGSTER_STAR),
            new RolePair(Noellesroles.VULTURE, Noellesroles.FERRYMAN)
    );

    public record RolePair(Role a, Role b) {}

    /**
     * 消解候选池中的互斥对：两者同在池中时随机剔除其一；一方已被分配（强制指派）时剔除池中另一方。
     * 应在建池后、分配前调用。
     */
    public static void resolveMutualExclusions(List<Role> pool, Set<Role> assigned, Random random) {
        for (RolePair pair : MUTUALLY_EXCLUSIVE_PAIRS) {
            boolean aInPool = pool.contains(pair.a());
            boolean bInPool = pool.contains(pair.b());
            if (aInPool && bInPool) {
                pool.remove(random.nextBoolean() ? pair.a() : pair.b());
            } else if (aInPool && assigned.contains(pair.b())) {
                pool.remove(pair.a());
            } else if (bInPool && assigned.contains(pair.a())) {
                pool.remove(pair.b());
            }
        }
    }

    private static final Set<Role> KILLER_ROLES = Set.of(
            WatheRoles.KILLER,
            Noellesroles.SWAPPER,
            Noellesroles.PHANTOM,
            Noellesroles.MORPHLING,
            Noellesroles.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES,
            Noellesroles.BOMBER,
            Noellesroles.ASSASSIN,
            Noellesroles.SCAVENGER,
            Noellesroles.SERIAL_KILLER,
            Noellesroles.SILENCER,
            Noellesroles.POISONER,
            Noellesroles.BANDIT,
            Noellesroles.HUNTER,
            Noellesroles.COMMANDER,
            Noellesroles.BREACHER,
            Noellesroles.PHONISM,
            Noellesroles.DREAM_EATER
    );

    public static boolean isActualKillerRole(Role role) {
        return role != null && KILLER_ROLES.contains(role);
    }

    public static int countAliveAndNotSwallowed(ServerWorld serverWorld) {
        int count = 0;
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(p) || SpectatorStateHelper.isRealSpectator(p)) continue;
            SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(p);
            if (!swallowed.isSwallowed()) {
                count++;
            }
        }
        return count;
    }
}

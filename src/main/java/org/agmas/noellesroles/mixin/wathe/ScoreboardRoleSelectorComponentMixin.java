package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.killerhistory.KillerHistoryCalculator;
import org.agmas.noellesroles.killerhistory.KillerHistoryPlayerComponent;
import org.agmas.noellesroles.rolehistory.RoleHistoryCalculator;
import org.agmas.noellesroles.rolehistory.RoleHistoryPlayerComponent;
import org.agmas.noellesroles.util.ForcedRoleTracker;
import org.agmas.noellesroles.util.RoleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Mixin(ScoreboardRoleSelectorComponent.class)
public abstract class ScoreboardRoleSelectorComponentMixin {

    /**
     * 留档本局被 /wathe:forceRole 指定角色的玩家：assignForcedRoles 发放后会清空 forcedRoles，
     * 后续警类分配、列车长保底与开局双选据 ForcedRoleTracker 跳过这些玩家。
     */
    @Inject(method = "assignForcedRoles", at = @At("HEAD"))
    private void noellesroles$recordForcedPlayers(ServerWorld world, GameWorldComponent gameWorld,
            List<ServerPlayerEntity> players, CallbackInfo ci) {
        ScoreboardRoleSelectorComponent self = (ScoreboardRoleSelectorComponent) (Object) this;
        Set<UUID> forced = new HashSet<>();
        for (ServerPlayerEntity player : players) {
            if (self.getForcedRoleForPlayer(player.getUuid()) != null) {
                forced.add(player.getUuid());
            }
        }
        ForcedRoleTracker.record(forced);
    }

    /**
     * 按玩家杀手历史动态加权选取杀手，取代 vanilla 的均匀随机选取。
     * 逻辑与 vanilla assignKillers 一致（forced 杀手计数、特殊角色池），仅玩家选取改为加权不放回抽样。
     */
    @Inject(method = "assignKillers", at = @At("HEAD"), cancellable = true)
    private void noellesroles$weightedKillerSelection(
            ServerWorld world, GameWorldComponent gameWorld, List<ServerPlayerEntity> players, int killerCount,
            CallbackInfoReturnable<Integer> cir) {

        // 统计已分配的杀手阵营玩家（forced roles），与新分配的合计为返回值
        Set<Role> assignedKillerRoles = new HashSet<>();
        int existingKillerCount = 0;
        for (ServerPlayerEntity player : players) {
            Role role = gameWorld.getRole(player);
            if (role != null && role.getFaction() == Faction.KILLER) {
                assignedKillerRoles.add(role);
                existingKillerCount++;
            }
        }
        killerCount = Math.max(0, killerCount - existingKillerCount);

        // 特殊杀手角色池（与 vanilla 相同：非 vanilla 杀手阵营、启用、未分配、满足出场条件）
        RoleSelectionContext context = ((ScoreboardRoleSelectorComponentAccessor) this)
                .noellesroles$invokeCreateSelectionContext(world, gameWorld, players);
        List<Role> specialRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.KILLER
                    && !WatheRoles.VANILLA_ROLES.contains(role)
                    && gameWorld.isRoleEnabled(role)
                    && !assignedKillerRoles.contains(role)
                    && role.shouldAppear(context)) {
                specialRoles.add(role);
            }
        }
        // 可用玩家（未分配角色），按杀手历史计算权重后加权抽取
        ArrayList<ServerPlayerEntity> available =
                ((ScoreboardRoleSelectorComponentAccessor) this)
                        .noellesroles$invokeGetAvailablePlayers(world, gameWorld, players);

        Map<UUID, Double> weights = new HashMap<>();
        List<UUID> candidates = new ArrayList<>();
        for (ServerPlayerEntity player : available) {
            KillerHistoryPlayerComponent history = KillerHistoryPlayerComponent.KEY.get(player);
            weights.put(player.getUuid(), KillerHistoryCalculator.computeKillerWeight(
                    history.getConsecutiveNonKillerGames(), history.getKillerGamesLast6()));
            candidates.add(player.getUuid());
        }

        List<UUID> picked = KillerHistoryCalculator.pickWeightedPlayers(
                candidates, weights, killerCount, new Random(world.getRandom().nextLong()));

        int assignedCount = existingKillerCount;
        for (UUID uuid : picked) {
            ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(uuid);
            if (player == null) continue;
            Role assignedRole;
            if (!specialRoles.isEmpty()) {
                // 避开该玩家近3局用过的杀手角色；排除后无候选时回退全池（允许重复）
                List<Identifier> recent = RoleHistoryPlayerComponent.KEY.get(player).getRecentRoleIdentifiers();
                assignedRole = RoleHistoryCalculator.pickAndRemove(specialRoles, recent, Role::identifier,
                        new Random(world.getRandom().nextLong()));
            } else {
                assignedRole = WatheRoles.KILLER; // 池空兜底与 vanilla 一致
            }
            gameWorld.addRole(player, assignedRole);
            assignedCount++;
        }

        cir.setReturnValue(assignedCount);
    }

    /**
     * 复刻 vanilla assignNeutrals 并加入"避开该玩家近3局角色"的抽取。
     * 语义与 vanilla 一致:池空直接返回不发任何中立角色(无 LOOSE_END 兜底);中途池尽即停,
     * 剩余本应中立的玩家留给平民流。
     */
    @Inject(method = "assignNeutrals", at = @At("HEAD"), cancellable = true)
    private void noellesroles$neutralSelection(
            ServerWorld world, GameWorldComponent gameWorld, List<ServerPlayerEntity> players, int neutralCount,
            CallbackInfoReturnable<Integer> cir) {

        // 统计既有中立玩家。NO_ROLE 的阵营也是 NEUTRAL,必须显式排除
        Set<Role> assignedNeutralRoles = new HashSet<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameWorld.getRole(player);
            if (role != null && role.getFaction() == Faction.NEUTRAL && !role.equals(WatheRoles.NO_ROLE)) {
                assignedNeutralRoles.add(role);
            }
        }
        neutralCount = Math.max(0, neutralCount - assignedNeutralRoles.size());

        RoleSelectionContext context = ((ScoreboardRoleSelectorComponentAccessor) this)
                .noellesroles$invokeCreateSelectionContext(world, gameWorld, players);
        List<Role> neutralRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.NEUTRAL
                    && !WatheRoles.VANILLA_ROLES.contains(role)
                    && gameWorld.isRoleEnabled(role)
                    && !assignedNeutralRoles.contains(role)
                    && role.shouldAppear(context)) {
                neutralRoles.add(role);
            }
        }

        // 出场条件在分配前统一求值，同阶段角色无法互相感知，显式消解互斥对
        RoleUtils.resolveMutualExclusions(neutralRoles, assignedNeutralRoles,
                new Random(world.getRandom().nextLong()));

        if (neutralRoles.isEmpty()) {
            cir.setReturnValue(assignedNeutralRoles.size());
            return;
        }

        ArrayList<ServerPlayerEntity> available =
                ((ScoreboardRoleSelectorComponentAccessor) this)
                        .noellesroles$invokeGetAvailablePlayers(world, gameWorld, players);

        int assignedCount = assignedNeutralRoles.size();
        for (ServerPlayerEntity player : available) {
            if (neutralCount <= 0 || neutralRoles.isEmpty()) {
                break;
            }
            List<Identifier> recent = RoleHistoryPlayerComponent.KEY.get(player).getRecentRoleIdentifiers();
            Role assignedRole = RoleHistoryCalculator.pickAndRemove(neutralRoles, recent, Role::identifier,
                    new Random(world.getRandom().nextLong()));
            gameWorld.addRole(player, assignedRole);
            assignedCount++;
            neutralCount--;
        }

        cir.setReturnValue(assignedCount);
    }

    @Inject(method = "assignVigilantes", at = @At("HEAD"), cancellable = true)
    private void noellesroles$distributeVigilanteSlots(
            ServerWorld world,
            GameWorldComponent gameWorld,
            List<ServerPlayerEntity> players,
            int count,
            CallbackInfo ci) {

        int existingVigilante = 0;
        int existingVeteran = 0;
        int existingPatrol = 0;
        int existingWhiteKnight = 0;
        for (ServerPlayerEntity player : players) {
            Role role = gameWorld.getRole(player);
            if (role == null) continue;
            if (role.equals(WatheRoles.VIGILANTE)) existingVigilante++;
            else if (role.equals(WatheRoles.VETERAN)) existingVeteran++;
            else if (role.equals(Noellesroles.RIOT_PATROL)) existingPatrol++;
            else if (role.equals(Noellesroles.WHITE_KNIGHT)) existingWhiteKnight++;
        }

        int targetVigilante;
        int targetVeteran;
        int targetPatrol;
        if (count <= 2) {
            targetVigilante = count;
            targetVeteran = 0;
            targetPatrol = 0;
        } else if (count == 3) {
            targetVigilante = 2;
            if (world.getRandom().nextBoolean()) {
                targetVeteran = 1;
                targetPatrol = 0;
            } else {
                targetVeteran = 0;
                targetPatrol = 1;
            }
        } else {
            targetVigilante = (count + 1) / 2;
            int remaining = count - targetVigilante;
            int extras = remaining - 2;
            int coinVeteran = 0;
            int coinPatrol = 0;
            for (int i = 0; i < extras; i++) {
                if (world.getRandom().nextBoolean()) coinVeteran++;
                else coinPatrol++;
            }
            targetVeteran = 1 + coinVeteran;
            targetPatrol = 1 + coinPatrol;
        }

        // 白骑士：按配置概率替换一名义警出场，每局至多一名（任意义警名额数量下都可替换）
        int targetWhiteKnight = 0;
        int whiteKnightChance = MathHelper.clamp(
                NoellesRolesConfig.HANDLER.instance().whiteKnightChancePercent, 0, 100);
        if (existingWhiteKnight == 0
                && gameWorld.isRoleEnabled(Noellesroles.WHITE_KNIGHT)
                && count >= 1
                && targetVigilante >= 1
                && world.getRandom().nextInt(100) < whiteKnightChance) {
            targetVigilante -= 1;
            targetWhiteKnight = 1;
        }

        int addVigilante = Math.max(0, targetVigilante - existingVigilante);
        int addVeteran = Math.max(0, targetVeteran - existingVeteran);
        int addPatrol = Math.max(0, targetPatrol - existingPatrol);
        int addWhiteKnight = Math.max(0, targetWhiteKnight - existingWhiteKnight);

        ScoreboardRoleSelectorComponentAccessor accessor =
                (ScoreboardRoleSelectorComponentAccessor) this;
        ArrayList<ServerPlayerEntity> available =
                accessor.noellesroles$invokeGetAvailablePlayers(world, gameWorld, players);

        List<Role> queue = new ArrayList<>(addVigilante + addVeteran + addPatrol + addWhiteKnight);
        for (int i = 0; i < addVigilante; i++) queue.add(WatheRoles.VIGILANTE);
        for (int i = 0; i < addVeteran; i++) queue.add(WatheRoles.VETERAN);
        for (int i = 0; i < addPatrol; i++) queue.add(Noellesroles.RIOT_PATROL);
        for (int i = 0; i < addWhiteKnight; i++) queue.add(Noellesroles.WHITE_KNIGHT);
        Collections.shuffle(queue, new java.util.Random(world.getRandom().nextLong()));

        int slot = 0;
        for (ServerPlayerEntity player : available) {
            if (slot >= queue.size()) break;
            if (ForcedRoleTracker.isForced(player.getUuid())) continue; // 被 forceRole 指定的玩家不占警类名额（兜底，正常已在强制阶段拿到角色）
            gameWorld.addRole(player, queue.get(slot));
            slot++;
        }

        ci.cancel();
    }

    @Inject(method = "assignCivilians", at = @At("HEAD"), cancellable = true)
    private void noellesroles$civilianSelection(
            ServerWorld world,
            GameWorldComponent gameWorld,
            List<ServerPlayerEntity> players,
            CallbackInfoReturnable<Integer> cir) {

        ArrayList<ServerPlayerEntity> available =
                ((ScoreboardRoleSelectorComponentAccessor) this)
                        .noellesroles$invokeGetAvailablePlayers(world, gameWorld, players);

        Set<Role> assignedCivilianRoles = new HashSet<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameWorld.getRole(player);
            if (role != null && role.getFaction() == Faction.CIVILIAN) {
                assignedCivilianRoles.add(role);
            }
        }

        RoleSelectionContext context = ((ScoreboardRoleSelectorComponentAccessor) this)
                .noellesroles$invokeCreateSelectionContext(world, gameWorld, players);
        List<Role> civilianRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.CIVILIAN
                    && !WatheRoles.VANILLA_ROLES.contains(role)
                    && gameWorld.isRoleEnabled(role)
                    && !assignedCivilianRoles.contains(role)
                    && role.shouldAppear(context)) {
                civilianRoles.add(role);
            }
        }

        int assignedCount = 0;
        for (ServerPlayerEntity player : available) {
            Role assignedRole;
            if (civilianRoles.isEmpty()) {
                assignedRole = WatheRoles.CIVILIAN;
            } else {
                // 避开该玩家近3局用过的平民角色；排除后无候选时回退全池（允许重复，避免只能当无技能平民）
                List<Identifier> recent = RoleHistoryPlayerComponent.KEY.get(player).getRecentRoleIdentifiers();
                assignedRole = RoleHistoryCalculator.pickAndRemove(civilianRoles, recent, Role::identifier,
                        new Random(world.getRandom().nextLong()));
            }
            gameWorld.addRole(player, assignedRole);
            assignedCount++;
        }

        guaranteeConductor(world, gameWorld, players);

        cir.setReturnValue(assignedCount);
    }

    /** 列车长保底：CONDUCTOR 启用且本局无人分到 CONDUCTOR 时，随机把一名好人玩家改为 CONDUCTOR */
    private static void guaranteeConductor(ServerWorld world, GameWorldComponent gameWorld, List<ServerPlayerEntity> players) {
        if (!gameWorld.isRoleEnabled(Noellesroles.CONDUCTOR)) {
            return;
        }

        ArrayList<ServerPlayerEntity> vanillaCivilianCandidates = new ArrayList<>();
        ArrayList<ServerPlayerEntity> civilianCandidates = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameWorld.getRole(player);
            if (role == null) {
                continue;
            }
            if (role.equals(Noellesroles.CONDUCTOR)) {
                return;
            }
            if (ForcedRoleTracker.isForced(player.getUuid())) {
                continue; // 被 forceRole 指定的玩家不能被改为列车长
            }
            if (role.equals(WatheRoles.CIVILIAN)) {
                vanillaCivilianCandidates.add(player);
                continue;
            }
            if (role.getFaction().equals(WatheRoles.CIVILIAN.getFaction())
                    && !Noellesroles.isVigilanteSlotRole(role)) {
                civilianCandidates.add(player);
            }
        }

        List<ServerPlayerEntity> candidates = vanillaCivilianCandidates.isEmpty()
                ? civilianCandidates
                : vanillaCivilianCandidates;
        if (candidates.isEmpty()) {
            return;
        }

        ServerPlayerEntity selected = candidates.get(world.getRandom().nextInt(candidates.size()));
        gameWorld.addRole(selected, Noellesroles.CONDUCTOR);
    }
}

package org.agmas.noellesroles.dualrole;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.DualRoleOptionsS2CPacket;
import org.agmas.noellesroles.rolehistory.RoleHistoryCalculator;
import org.agmas.noellesroles.rolehistory.RoleHistoryPlayerComponent;
import org.agmas.noellesroles.util.MatchRecordTimeShift;
import org.agmas.noellesroles.util.RoleUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

public class DualRoleSelectionWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<DualRoleSelectionWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "dual_role_selection"),
            DualRoleSelectionWorldComponent.class
    );

    private static final Set<Role> BASE_ROLES = Set.of(
            WatheRoles.KILLER, WatheRoles.VIGILANTE, WatheRoles.CIVILIAN, WatheRoles.LOOSE_END
    );

    public enum Phase { INACTIVE, SELECTING, COMPLETE }

    // --- Static state for deferring RoleAssigned.EVENT ---
    private static boolean DEFER_ROLE_EVENTS = false;
    private static final Map<UUID, Role> PENDING_ASSIGNMENTS = new LinkedHashMap<>();

    public static boolean isDeferringRoleEvents() {
        return DEFER_ROLE_EVENTS;
    }

    public static void storePendingAssignment(UUID uuid, Role role) {
        if (DEFER_ROLE_EVENTS) {
            PENDING_ASSIGNMENTS.put(uuid, role);
        }
    }

    public static void enableDeferral() {
        DEFER_ROLE_EVENTS = true;
        PENDING_ASSIGNMENTS.clear();
    }

    public static void disableDeferral() {
        DEFER_ROLE_EVENTS = false;
    }
    // --- End static state ---

    private final World world;
    private Phase phase = Phase.INACTIVE;
    private final Map<UUID, Identifier> secondOptions = new HashMap<>();
    private final Set<UUID> choseSecond = new HashSet<>();
    private final Set<UUID> responded = new HashSet<>();
    private int ticksRemaining;
    private boolean openingPhaseApplied;
    private boolean assignmentsStored;

    public DualRoleSelectionWorldComponent(World world) {
        this.world = world;
    }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }
    public int getTicksRemaining() { return ticksRemaining; }
    public boolean isOpeningPhaseApplied() { return openingPhaseApplied; }
    public void markOpeningPhaseApplied() { this.openingPhaseApplied = true; }

    /** 选择界面显示期间为 true：对局计时、体力、物品冷却等开局进程应保持冻结。 */
    public static boolean isSelectionFreezeActive(World world) {
        return KEY.get(world).getPhase() == Phase.SELECTING;
    }

    /**
     * Start the dual role selection. Called from ON_FINISH_INITIALIZE.
     * At this point, roles have been assigned by Wathe but RoleAssigned.EVENT was deferred.
     * The pending assignments are stored in PENDING_ASSIGNMENTS.
     */
    public void startSelection(ServerWorld serverWorld, GameWorldComponent gwc) {
        phase = Phase.INACTIVE;
        secondOptions.clear();
        choseSecond.clear();
        responded.clear();
        ticksRemaining = 0;
        openingPhaseApplied = false;
        assignmentsStored = true;

        if (PENDING_ASSIGNMENTS.isEmpty()) {
            disableDeferral();
            return;
        }

        List<UUID> allPlayers = new ArrayList<>(PENDING_ASSIGNMENTS.keySet());
        if (allPlayers.isEmpty()) {
            disableDeferral();
            return;
        }

        // Build a set of all already-assigned role IDs
        Set<Identifier> assignedRoleIds = new HashSet<>();
        for (Role r : PENDING_ASSIGNMENTS.values()) {
            assignedRoleIds.add(r.identifier());
        }
        Set<Role> assignedRoles = new HashSet<>(PENDING_ASSIGNMENTS.values());

        // 出现条件上下文：与 Wathe 首轮分配的构造方式一致，保证双角色抽选不会提供首轮被条件拦下的角色
        List<ServerPlayerEntity> ctxPlayers = new ArrayList<>();
        for (UUID uuid : PENDING_ASSIGNMENTS.keySet()) {
            ServerPlayerEntity p = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
            if (p != null) ctxPlayers.add(p);
        }
        RoleSelectionContext selectionContext = ScoreboardRoleSelectorComponent.KEY
                .get(serverWorld.getScoreboard())
                .createSelectionContext(serverWorld, gwc, ctxPlayers);

        // 按阵营分组（跳过固定槽位玩家——义警/列车长等角色不可参与双角色选择）
        Map<FactionGroup, List<UUID>> factionPlayers = new LinkedHashMap<>();
        for (UUID uuid : allPlayers) {
            Role role = PENDING_ASSIGNMENTS.get(uuid);
            if (role == null) continue;
            if (Noellesroles.isFixedDualRoleSlot(role)) continue;
            FactionGroup fg = getFactionGroup(role);
            factionPlayers.computeIfAbsent(fg, k -> new ArrayList<>()).add(uuid);
        }

        if (factionPlayers.isEmpty()) {
            disableDeferral();
            return;
        }

        // For each faction, find available unassigned roles
        boolean anyFactionHasEnough = false;

        for (Map.Entry<FactionGroup, List<UUID>> entry : factionPlayers.entrySet()) {
            FactionGroup fg = entry.getKey();
            List<UUID> playersInFaction = entry.getValue();

            List<Role> available = new ArrayList<>();
            for (Role r : Noellesroles.ALL_ROLES) {
                if (!gwc.isRoleEnabled(r)) continue;
                if (BASE_ROLES.contains(r)) continue;
                if (Noellesroles.isFixedDualRoleSlot(r)) continue; // 固定槽位角色不能作为备选分配出去
                if (Noellesroles.isInitialDrawOnlyRole(r)) continue; // 黑警/黑帮巨星只能初始抽取
                if (getFactionGroup(r) != fg) continue;
                if (assignedRoleIds.contains(r.identifier())) continue;
                if (!r.shouldAppear(selectionContext)) continue;
                available.add(r);
            }

            // 出场条件在分配前统一求值，同阶段互斥角色无法互相感知，显式消解
            Random rng = new Random();
            RoleUtils.resolveMutualExclusions(available, assignedRoles, rng);

            // Need at least one extra role per player
            if (available.size() < playersInFaction.size()) {
                continue;
            }

            anyFactionHasEnough = true;
            List<Role> remaining = new ArrayList<>(available);

            for (UUID playerUuid : playersInFaction) {
                Role secondRole;
                ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(playerUuid);
                if (player != null) {
                    // 避开该玩家近3局用过的同阵营角色；排除后无候选时回退全池（允许重复）
                    List<Identifier> recent = RoleHistoryPlayerComponent.KEY.get(player).getRecentRoleIdentifiers();
                    secondRole = RoleHistoryCalculator.pickAndRemove(remaining, recent, Role::identifier, rng);
                } else {
                    secondRole = remaining.removeFirst(); // 离线极端情况：不排除，防止取不到组件
                }
                secondOptions.put(playerUuid, secondRole.identifier());
            }
        }

        if (anyFactionHasEnough) {
            phase = Phase.SELECTING;
            ticksRemaining = 100;

            // 给所有玩家发送 UI：可选择的玩家收到二选界面；固定槽位/无备选项玩家收到"你的角色是 XXX"宣言屏。
            // 所有人共享同一个 5 秒倒计时，行动时间在同一水平线上。
            for (UUID uuid : PENDING_ASSIGNMENTS.keySet()) {
                ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
                if (player == null) continue;
                Role firstRole = PENDING_ASSIGNMENTS.get(uuid);
                if (firstRole == null) continue;

                boolean canChoose = secondOptions.containsKey(uuid);
                Identifier secondId = canChoose ? secondOptions.get(uuid) : firstRole.identifier();
                String secondDisplayKey = canChoose
                        ? "announcement.role." + secondId.getPath()
                        : "";

                ServerPlayNetworking.send(player, new DualRoleOptionsS2CPacket(
                        firstRole.identifier(),
                        "announcement.role." + firstRole.identifier().getPath(),
                        secondId,
                        secondDisplayKey,
                        100,
                        canChoose
                ));
            }
        }
    }

    public boolean tickSelection() {
        if (phase != Phase.SELECTING) return false;
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            ticksRemaining = 0;
            // 始终跑满整个倒计时再结算，保证所有玩家（含固定槽位玩家）行动时间一致；
            // 未响应的玩家在 applyResults 中保留第一角色
            phase = Phase.COMPLETE;
            return true;
        }
        return false;
    }

    public void handlePlayerChoice(ServerPlayerEntity player, boolean playerChoseSecond) {
        if (phase != Phase.SELECTING) return;
        UUID uuid = player.getUuid();
        if (responded.contains(uuid)) return;
        responded.add(uuid);
        if (playerChoseSecond) {
            choseSecond.add(uuid);
        }
    }

    /**
     * Apply results: fire RoleAssigned.EVENT once with the final chosen role for each player.
     * Called when selection completes (all responded or timeout).
     */
    public void applyResults(ServerWorld serverWorld, GameWorldComponent gwc) {
        disableDeferral(); // Stop deferring events

        // 双选期间黑屏渐亮已经流逝：把 fade 重置回开局初始化完成时的满值，
        // 让原版的"保持黑屏→渐亮"效果在选完之后完整播放
        gwc.setFade(GameConstants.FADE_TIME + GameConstants.FADE_PAUSE);

        // 选择期间对局时钟被冻结、世界刻却照走：把回放时间基准后移到此刻，
        // 让回放计时从选完角色开始读（窗口内记录的事件统一压到 0:00）
        if (GameRecordManager.hasActiveMatch()) {
            GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
            long now = serverWorld.getTime();
            long frozenTicks = now - match.getStartTick();
            if (frozenTicks > 0) {
                MatchRecordTimeShift shift = (MatchRecordTimeShift) (Object) match;
                shift.noellesroles$shiftStartTick(frozenTicks);
                shift.noellesroles$collapseEventsBefore(now);
            }
        }

        for (Map.Entry<UUID, Role> entry : PENDING_ASSIGNMENTS.entrySet()) {
            UUID uuid = entry.getKey();
            Role assignedRole = entry.getValue();
            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            Role finalRole;
            if (choseSecond.contains(uuid)) {
                // Player chose the second option
                Identifier secondId = secondOptions.get(uuid);
                Role secondRole = null;
                if (secondId != null) {
                    for (Role r : Noellesroles.ALL_ROLES) {
                        if (r.identifier().equals(secondId)) {
                            secondRole = r;
                            break;
                        }
                    }
                }
                finalRole = secondRole != null ? secondRole : assignedRole;
            } else {
                finalRole = assignedRole;
            }

            // Assign the FINAL role and fire RoleAssigned.EVENT ONCE
            gwc.addRole(player, finalRole);
            RoleAssigned.EVENT.invoker().assignRole(player, finalRole);

            // 用最终角色覆盖比赛记录，修正回放/观战系统的角色显示
            // （初始记录在 ON_FINISH_INITIALIZE 时写入的是基础角色，此时选择尚未完成）
            if (GameRecordManager.hasActiveMatch()) {
                NbtCompound playerInfo = new NbtCompound();
                playerInfo.putUuid("uuid", uuid);
                playerInfo.putString("name", player.getName().getString());
                playerInfo.putString("role", finalRole.identifier().toString());
                GameRecordManager.event(GameRecordTypes.ROLE_ASSIGNED)
                        .world(serverWorld)
                        .actor(player)
                        .putNbt("player", playerInfo)
                        .record();
            }
        }

        // Sync updated role data to all clients
        gwc.sync();

        // Send welcome announcements with the correct role
        int totalPlayers = gwc.getAllPlayers().size();
        int killerCount = gwc.getAllKillerTeamPlayers().size();
        for (UUID uuid : PENDING_ASSIGNMENTS.keySet()) {
            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                Role newRole = gwc.getRole(player);
                if (newRole != null) {
                    ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                            newRole.identifier().toString(),
                            killerCount,
                            totalPlayers - killerCount
                    ));
                }
            }
        }

        PENDING_ASSIGNMENTS.clear();
        phase = Phase.COMPLETE;
    }

    /**
     * Fire all pending RoleAssigned.EVENTs immediately (when dual role is not possible).
     * Called from ON_FINISH_INITIALIZE when dual role selection is skipped.
     */
    public void firePendingEvents(ServerWorld serverWorld, GameWorldComponent gwc) {
        disableDeferral();

        for (Map.Entry<UUID, Role> entry : PENDING_ASSIGNMENTS.entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                RoleAssigned.EVENT.invoker().assignRole(player, role);
            }
        }

        // Send welcome announcements (mixin skipped them)
        int totalPlayers = gwc.getAllPlayers().size();
        int killerCount = gwc.getAllKillerTeamPlayers().size();
        for (UUID uuid : PENDING_ASSIGNMENTS.keySet()) {
            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                Role role = gwc.getRole(player);
                if (role != null) {
                    ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                            role.identifier().toString(),
                            killerCount,
                            totalPlayers - killerCount
                    ));
                }
            }
        }

        PENDING_ASSIGNMENTS.clear();
        phase = Phase.INACTIVE;
    }

    public void cancelSelection() {
        disableDeferral();
        PENDING_ASSIGNMENTS.clear();
        phase = Phase.INACTIVE;
        secondOptions.clear();
        choseSecond.clear();
        responded.clear();
        ticksRemaining = 0;
    }

    public void handlePlayerDisconnect(UUID uuid) {
        if (phase != Phase.SELECTING) return;
        secondOptions.remove(uuid);
        choseSecond.remove(uuid);
        responded.add(uuid);
    }

    private FactionGroup getFactionGroup(Role role) {
        if (role.isInnocent()) return FactionGroup.INNOCENT;
        if (role.getFaction() == Faction.KILLER) return FactionGroup.KILLER;
        return FactionGroup.NEUTRAL;
    }

    private enum FactionGroup { INNOCENT, KILLER, NEUTRAL }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeEnumConstant(phase);
        buf.writeInt(ticksRemaining);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.phase = buf.readEnumConstant(Phase.class);
        this.ticksRemaining = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putString("phase", phase.name());
        tag.putInt("ticksRemaining", ticksRemaining);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.phase = Phase.valueOf(tag.getString("phase"));
        this.ticksRemaining = tag.getInt("ticksRemaining");
    }
}

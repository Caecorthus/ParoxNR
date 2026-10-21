package org.agmas.noellesroles.breacher;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheProperties;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.SchulteResultS2CPacket;
import org.agmas.noellesroles.packet.SchulteStartS2CPacket;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.agmas.noellesroles.task.RepairBreachTask;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.RoleUtils;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 破袭者破坏点的世界级状态（全场同时最多 {@link #POINT_CAP} 个）。
 * 负责：破坏点标记实体、每10秒金币收益、按点数递增的电灯损坏、
 * 以及每个破坏点各自三局（3x3/4x4/5x5）舒尔特方格修复会话。
 */
public class BreacherWorldComponent implements ServerTickingComponent {
    public static final ComponentKey<BreacherWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "breacher_world"), BreacherWorldComponent.class);

    public static final int POINT_CAP = 3;
    public static final int INCOME_INTERVAL_TICKS = GameConstants.getInTicks(0, 10);
    public static final int INCOME_AMOUNT = 10;
    public static final int REPAIR_COOLDOWN_TICKS = GameConstants.getInTicks(0, 15);
    public static final int[] LEVEL_SIZE = {3, 4, 5};
    public static final int[] LEVEL_TIME_TICKS = {
            GameConstants.getInTicks(0, 10),
            GameConstants.getInTicks(0, 25),
            GameConstants.getInTicks(0, 40)
    };

    private static final int LIGHT_REASSERT_INTERVAL_TICKS = 20;
    private static final int PROXIMITY_BLINDNESS_TICKS = 30;
    private static final int GLOBAL_EFFECT_TICKS = 40;
    /** 夜视给满 1 分钟：刷新周期远小于时长，避免临近到期时视野闪烁；关灯结束时显式移除。 */
    private static final int GLOBAL_NIGHT_VISION_TICKS = GameConstants.getInTicks(1, 0);
    private static final String MARKER_TAG = "noellesroles_breacher_marker";
    private static final String MARKER_OWNER_TAG_PREFIX = "noellesroles_breacher_owner_";

    private final World world;

    private int nextPointId = 1;
    private final List<PointState> points = new ArrayList<>();
    private final List<DamagedLight> damagedLights = new ArrayList<>();

    /** 场上全部可损坏电灯位置（懒扫描，游戏内地图不变）。 */
    private List<BlockPos> lightCache = null;
    private int reassertTick = 0;

    public BreacherWorldComponent(World world) {
        this.world = world;
    }

    public static class Slot {
        public UUID occupant;
        public int[] sequence;
        public int next;
        public int ticksRemaining;

        Slot() {
        }

        Slot(UUID occupant, int[] sequence, int ticksRemaining) {
            this.occupant = occupant;
            this.sequence = sequence;
            this.next = 0;
            this.ticksRemaining = ticksRemaining;
        }
    }

    public static class PointState {
        public int id;
        public UUID owner;
        public BlockPos pos;
        public UUID markerEntityUuid;
        public int payoutTicksRemaining = INCOME_INTERVAL_TICKS;
        public final boolean[] levelDone = new boolean[LEVEL_SIZE.length];
        public final Slot[] slots = new Slot[LEVEL_SIZE.length];
        public final List<UUID> completedBy = new ArrayList<>();

        public boolean allLevelsDone() {
            for (boolean done : levelDone) {
                if (!done) return false;
            }
            return true;
        }
    }

    private static class DamagedLight {
        BlockPos pos;
        boolean originalLit;
        boolean originalActive;
        int pointId;

        DamagedLight(BlockPos pos, boolean originalLit, boolean originalActive, int pointId) {
            this.pos = pos;
            this.originalLit = originalLit;
            this.originalActive = originalActive;
            this.pointId = pointId;
        }
    }

    // ==================== tick ====================

    @Override
    public void serverTick() {
        if (!(world instanceof ServerWorld serverWorld)) return;
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        if (!game.isRunning()) {
            if (!points.isEmpty() || !damagedLights.isEmpty()) {
                clearAll(serverWorld);
            }
            return;
        }

        for (int i = points.size() - 1; i >= 0; i--) {
            PointState point = points.get(i);
            pinMarkerInPlace(serverWorld, point);
            tickIncome(serverWorld, game, point);
            tickSlots(serverWorld, game, point);
        }

        if (++reassertTick >= LIGHT_REASSERT_INTERVAL_TICKS) {
            reassertTick = 0;
            reassertDamagedLights(serverWorld);
        }

        tickVisionPenalties(serverWorld, game);
    }

    // ==================== 视野惩罚（近距离失明 / 三点关灯） ====================

    /** 破坏点到达上限时的全场关灯状态，持续到点数降回上限以下。 */
    private boolean breachBlackout = false;
    private int visionTick = 0;

    private void tickVisionPenalties(ServerWorld serverWorld, GameWorldComponent game) {
        boolean shouldBlackout = points.size() >= POINT_CAP;
        if (shouldBlackout && !breachBlackout) {
            breachBlackout = true;
            playAmbientBlackoutSound(serverWorld);
        } else if (!shouldBlackout && breachBlackout) {
            breachBlackout = false;
            liftBlackoutEffects(serverWorld);
        }

        if (breachBlackout) {
            if (visionTick % 20 == 0) {
                applyGlobalBlackoutEffects(serverWorld, game);
                sendBlackoutMessage(serverWorld);
            }
        } else if (!points.isEmpty() && visionTick % 10 == 0) {
            applyProximityBlindness(serverWorld, game);
        }
        visionTick++;
    }

    /** 与修复资格一致：在场存活、未被吞噬、非杀手阵营。 */
    private boolean isRepairEligible(ServerPlayerEntity player, GameWorldComponent game) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return false;
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return false;
        return !RoleUtils.isActualKillerRole(game.getRole(player));
    }

    /** 非杀手玩家进入任一破坏点 7 格范围内：短效失明（金酒等免疫通过 BlackoutEffect 事件生效）。 */
    private void applyProximityBlindness(ServerWorld serverWorld, GameWorldComponent game) {
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!isRepairEligible(player, game)) continue;
            if (distanceToNearestPoint(player) > 7.0D) continue;
            if (isBlackoutVisionCancelled(player, PROXIMITY_BLINDNESS_TICKS)) continue;
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, PROXIMITY_BLINDNESS_TICKS, 0, false, false, true));
        }
    }

    private double distanceToNearestPoint(ServerPlayerEntity player) {
        double best = Double.MAX_VALUE;
        for (PointState point : points) {
            double dx = player.getX() - (point.pos.getX() + 0.5D);
            double dy = player.getY() + 1.0D - (point.pos.getY() + 0.5D);
            double dz = player.getZ() - (point.pos.getZ() + 0.5D);
            best = Math.min(best, dx * dx + dy * dy + dz * dz);
        }
        return Math.sqrt(best);
    }

    /** 镜像原版关灯：杀手阵营/老兵夜视，其余玩家失明（可被 BlackoutEffect 事件取消）。 */
    private void applyGlobalBlackoutEffects(ServerWorld serverWorld, GameWorldComponent game) {
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
            if (game.canUseKillerFeatures(player) || game.isRole(player, WatheRoles.VETERAN)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, GLOBAL_NIGHT_VISION_TICKS, 0, false, false, true));
            } else {
                if (isBlackoutVisionCancelled(player, GLOBAL_EFFECT_TICKS)) continue;
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.BLINDNESS, GLOBAL_EFFECT_TICKS, 0, false, false, true));
            }
        }
    }

    /** 复用原版关灯的取消事件（金酒免疫熄灯等）。 */
    private boolean isBlackoutVisionCancelled(ServerPlayerEntity player, int durationTicks) {
        BlackoutEffect.BlackoutResult result = BlackoutEffect.BEFORE.invoker()
                .beforeBlackoutEffect(player, durationTicks);
        return result != null && result.cancelled();
    }

    private void sendBlackoutMessage(ServerWorld serverWorld) {
        // 与商店关灯倒计时同格式（红字）
        Text message = Text.translatable("tip.breacher.blackout_active").formatted(Formatting.RED);
        for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(message, true);
        }
    }

    private void playAmbientBlackoutSound(ServerWorld serverWorld) {
        for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(WatheSounds.AMBIENT_BLACKOUT),
                    SoundCategory.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    100.0F, 1.0F, serverWorld.random.nextLong()));
        }
    }

    private void liftBlackoutEffects(ServerWorld serverWorld) {
        // 商店关灯仍在生效时不越权移除，由其自行管理
        if (WorldBlackoutComponent.KEY.get(serverWorld).isBlackoutActive()) return;
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    private void pinMarkerInPlace(ServerWorld serverWorld, PointState point) {
        Entity entity = point.markerEntityUuid == null ? null : serverWorld.getEntity(point.markerEntityUuid);
        if (entity == null || entity.isRemoved()) {
            // 标记实体意外丢失（地图重置等）：区块已加载时原地重建，避免出现不可修复的破坏点
            if (entity != null || !serverWorld.isChunkLoaded(point.pos)) return;
            BreacherMarkerEntity marker = new BreacherMarkerEntity(
                    org.agmas.noellesroles.NoellesRolesEntities.BREACHER_MARKER_ENTITY, serverWorld);
            marker.refreshPositionAndAngles(
                    point.pos.getX() + 0.5D, point.pos.getY(), point.pos.getZ() + 0.5D, 0.0F, 0.0F);
            marker.setCustomName(Text.translatable("label.breacher.break_point"));
            marker.addCommandTag(MARKER_TAG);
            if (!serverWorld.spawnEntity(marker)) return;
            point.markerEntityUuid = marker.getUuid();
            return;
        }
        entity.setPosition(point.pos.getX() + 0.5D, point.pos.getY(), point.pos.getZ() + 0.5D);
        entity.setVelocity(0.0D, 0.0D, 0.0D);
    }

    private void tickIncome(ServerWorld serverWorld, GameWorldComponent game, PointState point) {
        if (--point.payoutTicksRemaining > 0) return;
        point.payoutTicksRemaining = INCOME_INTERVAL_TICKS;

        PlayerEntity owner = serverWorld.getPlayerByUuid(point.owner);
        if (!(owner instanceof ServerPlayerEntity serverOwner)) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(serverOwner)) return;
        if (SwallowedPlayerComponent.isPlayerSwallowed(serverOwner)) return;
        if (!game.isRole(serverOwner, Noellesroles.BREACHER)) return;

        PlayerShopComponent.KEY.get(serverOwner).addToBalance(INCOME_AMOUNT);
        serverOwner.sendMessage(Text.translatable("tip.breacher.income", INCOME_AMOUNT)
                .withColor(Noellesroles.BREACHER.color() & 0xFFFFFF), true);
    }

    private void tickSlots(ServerWorld serverWorld, GameWorldComponent game, PointState point) {
        for (int level = 0; level < point.slots.length; level++) {
            Slot slot = point.slots[level];
            if (slot == null || point.levelDone[level]) continue;

            slot.ticksRemaining--;
            PlayerEntity occupant = serverWorld.getPlayerByUuid(slot.occupant);

            if (occupant == null || !occupant.isAlive()
                    || !GameFunctions.isPlayerPlayingAndAlive(occupant)
                    || SwallowedPlayerComponent.isPlayerSwallowed(occupant)) {
                // 掉线/死亡/被吞：仅释放槽位，不施加冷却
                point.slots[level] = null;
                continue;
            }
            if (slot.ticksRemaining <= 0) {
                failSlot(serverWorld, game, point, level, (ServerPlayerEntity) occupant,
                        SchulteResultS2CPacket.OUTCOME_FAIL_TIMEOUT, "tip.breacher.repair.fail_timeout");
            }
        }
    }

    // ==================== 破坏点创建 ====================

    /**
     * 生成破坏点：放置标记实体、损坏电灯、记录回放。成功返回 true。
     * 全局上限 {@link #POINT_CAP}；已满时提示破袭者并返回 false。
     */
    public boolean createPoint(ServerPlayerEntity breacher, BlockPos center) {
        if (!(world instanceof ServerWorld serverWorld)) return false;
        if (points.size() >= POINT_CAP) {
            breacher.sendMessage(Text.translatable("tip.breacher.cap_reached", POINT_CAP), true);
            return false;
        }

        BreacherMarkerEntity marker = new BreacherMarkerEntity(
                org.agmas.noellesroles.NoellesRolesEntities.BREACHER_MARKER_ENTITY, serverWorld);
        marker.refreshPositionAndAngles(
                center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        marker.setCustomName(Text.translatable("label.breacher.break_point"));
        marker.addCommandTag(MARKER_TAG);
        marker.addCommandTag(ownerMarkerTag(breacher.getUuid()));
        if (!serverWorld.spawnEntity(marker)) {
            return false;
        }

        PointState point = new PointState();
        point.id = nextPointId++;
        point.owner = breacher.getUuid();
        point.pos = center.toImmutable();
        point.markerEntityUuid = marker.getUuid();
        points.add(point);

        damageRandomLights(serverWorld, point);
        BreacherPlayerComponent.KEY.get(breacher).changeOwnActivePointCount(1);
        grantRepairTask(serverWorld);

        playBreakPointCreatedSound(serverWorld);
        recordBreakPointPlaced(breacher, center);
        return true;
    }

    /** 破坏点存在期间：给全体在场真实/假心情玩家注入「去修复破坏点。」任务（幂等）。 */
    private void grantRepairTask(ServerWorld serverWorld) {
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        PlayerMoodComponent.Task slot = NoellesTaskTypes.REPAIR_BREACH.slot();
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
            Role role = game.getRole(player);
            if (role == null) continue;
            Role.MoodType moodType = role.getMoodType();
            if (moodType != Role.MoodType.REAL && moodType != Role.MoodType.FAKE) continue;
            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
            if (mood.tasks.containsKey(slot)) continue;
            mood.tasks.put(slot, new RepairBreachTask());
            mood.sync();
        }
    }

    /** 破坏点全部修复（或清场）：移除所有玩家身上的「去修复破坏点。」任务。 */
    private void removeRepairTask(ServerWorld serverWorld) {
        PlayerMoodComponent.Task slot = NoellesTaskTypes.REPAIR_BREACH.slot();
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
            if (mood.tasks.remove(slot) != null) {
                mood.sync();
            }
        }
    }

    private void playBreakPointCreatedSound(ServerWorld serverWorld) {
        RegistryEntry<net.minecraft.sound.SoundEvent> soundEntry = RegistryEntry.of(ModSounds.BOMB_EXPLODE);
        long seed = serverWorld.random.nextLong();
        for (ServerPlayerEntity serverPlayer : serverWorld.getServer().getPlayerManager().getPlayerList()) {
            serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    soundEntry,
                    SoundCategory.PLAYERS,
                    serverPlayer.getX(),
                    serverPlayer.getY(),
                    serverPlayer.getZ(),
                    3.0F,
                    1.0F,
                    seed
            ));
        }
    }

    private void recordBreakPointPlaced(ServerPlayerEntity breacher, BlockPos pos) {
        NbtCompound extra = new NbtCompound();
        extra.putString("action", "place");
        extra.putInt("x", pos.getX());
        extra.putInt("y", pos.getY());
        extra.putInt("z", pos.getZ());
        GameRecordManager.recordSkillUse(breacher, Noellesroles.BREACHER_ID, null, extra);
    }

    // ==================== 电灯损坏 ====================

    private List<BlockPos> scanLights(ServerWorld serverWorld) {
        List<BlockPos> lights = new ArrayList<>();
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(serverWorld);
        Box area = areas.getPlayArea();
        if (area == null) return lights;
        for (int x = (int) area.minX; x <= (int) area.maxX; x++) {
            for (int y = (int) area.minY; y <= (int) area.maxY; y++) {
                for (int z = (int) area.minZ; z <= (int) area.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = serverWorld.getBlockState(pos);
                    if (state.contains(Properties.LIT) && state.contains(WatheProperties.ACTIVE)) {
                        lights.add(pos);
                    }
                }
            }
        }
        return lights;
    }

    /** 每新增一个破坏点随机损坏 ⌈总灯数/3⌉ 盏尚好电灯（3 个点时自然全坏）。 */
    private void damageRandomLights(ServerWorld serverWorld, PointState point) {
        if (lightCache == null) {
            lightCache = scanLights(serverWorld);
        }
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : lightCache) {
            if (!isLightDamaged(pos)) {
                candidates.add(pos);
            }
        }
        Collections.shuffle(candidates, new java.util.Random(serverWorld.random.nextLong()));
        int quota = (lightCache.size() + POINT_CAP - 1) / POINT_CAP;
        int count = Math.min(quota, candidates.size());
        for (int i = 0; i < count; i++) {
            BlockPos pos = candidates.get(i);
            BlockState state = serverWorld.getBlockState(pos);
            if (!state.contains(Properties.LIT) || !state.contains(WatheProperties.ACTIVE)) continue;
            damagedLights.add(new DamagedLight(pos, state.get(Properties.LIT), state.get(WatheProperties.ACTIVE), point.id));
            serverWorld.setBlockState(pos, state.with(Properties.LIT, false).with(WatheProperties.ACTIVE, false), Block.NOTIFY_ALL);
        }
    }

    private boolean isLightDamaged(BlockPos pos) {
        for (DamagedLight damaged : damagedLights) {
            if (damaged.pos.equals(pos)) return true;
        }
        return false;
    }

    /** 定期重申损坏状态，防止原版关灯恢复或玩家手动开关把灯“救活”。 */
    private void reassertDamagedLights(ServerWorld serverWorld) {
        if (damagedLights.isEmpty()) return;
        for (DamagedLight damaged : damagedLights) {
            BlockState state = serverWorld.getBlockState(damaged.pos);
            if (!state.contains(Properties.LIT) || !state.contains(WatheProperties.ACTIVE)) continue;
            if (state.get(Properties.LIT) || state.get(WatheProperties.ACTIVE)) {
                serverWorld.setBlockState(damaged.pos,
                        state.with(Properties.LIT, false).with(WatheProperties.ACTIVE, false), Block.NOTIFY_ALL);
            }
        }
    }

    private void restoreLightsFor(ServerWorld serverWorld, PointState point) {
        for (int i = damagedLights.size() - 1; i >= 0; i--) {
            DamagedLight damaged = damagedLights.get(i);
            if (damaged.pointId != point.id) continue;
            damagedLights.remove(i);
            BlockState state = serverWorld.getBlockState(damaged.pos);
            if (!state.contains(Properties.LIT) || !state.contains(WatheProperties.ACTIVE)) continue;
            serverWorld.setBlockState(damaged.pos,
                    state.with(Properties.LIT, damaged.originalLit).with(WatheProperties.ACTIVE, damaged.originalActive),
                    Block.NOTIFY_ALL);
        }
    }

    // ==================== 修复小游戏会话 ====================

    /** 右键破坏点标记：进入或恢复该点某一等级的舒尔特方格。 */
    public void startOrJoinRepair(ServerPlayerEntity repairer, int pointId) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);

        if (RoleUtils.isActualKillerRole(game.getRole(repairer))) {
            repairer.sendMessage(Text.translatable("tip.breacher.repair.killer"), true);
            return;
        }
        BreacherPlayerComponent repairerComp = BreacherPlayerComponent.KEY.get(repairer);
        if (repairerComp.getRepairCooldownTicks() > 0) {
            repairer.sendMessage(Text.translatable("tip.breacher.repair.cooldown",
                    (repairerComp.getRepairCooldownTicks() + 19) / 20), true);
            return;
        }

        PointState point = findPoint(pointId);
        if (point == null) return;

        // 同一玩家同时只能占一个槽位
        for (PointState other : points) {
            for (int level = 0; level < other.slots.length; level++) {
                Slot slot = other.slots[level];
                if (slot == null || other.levelDone[level]) continue;
                if (!slot.occupant.equals(repairer.getUuid())) continue;
                if (other.id == point.id) {
                    sendStartPacket(repairer, point, level); // 恢复当前局面
                    return;
                }
                repairer.sendMessage(Text.translatable("tip.breacher.repair.busy"), true);
                return;
            }
        }

        int freeLevel = -1;
        boolean anyOccupied = false;
        for (int level = 0; level < point.slots.length; level++) {
            if (point.levelDone[level]) continue;
            if (point.slots[level] == null) {
                freeLevel = level;
                break;
            }
            anyOccupied = true;
        }
        if (freeLevel < 0) {
            if (anyOccupied) {
                repairer.sendMessage(Text.translatable("tip.breacher.repair.full", POINT_CAP), true);
            }
            return;
        }

        int size = LEVEL_SIZE[freeLevel];
        int[] sequence = shuffledSequence(size * size, serverWorld.random);
        point.slots[freeLevel] = new Slot(repairer.getUuid(), sequence, LEVEL_TIME_TICKS[freeLevel]);
        sendStartPacket(repairer, point, freeLevel);
    }

    private void sendStartPacket(ServerPlayerEntity repairer, PointState point, int level) {
        Slot slot = point.slots[level];
        if (slot == null) return;
        ServerPlayNetworking.send(repairer, new SchulteStartS2CPacket(
                point.id, level, LEVEL_SIZE[level], LEVEL_TIME_TICKS[level],
                Math.max(0, slot.ticksRemaining), slot.next, slot.sequence));
    }

    public void handleRepairClick(ServerPlayerEntity player, int pointId, int level, int number) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (level < 0 || level >= LEVEL_SIZE.length) return;
        PointState point = findPoint(pointId);
        if (point == null || level >= point.slots.length) return;
        Slot slot = point.slots[level];
        if (slot == null || point.levelDone[level] || !slot.occupant.equals(player.getUuid())) return;
        if (slot.next >= slot.sequence.length) return;

        // 舒尔特方格规则：按数字顺序点击，第 next+1 次的正确值是数字 next+1（sequence 只是版面摆位）
        if (number != slot.next + 1) {
            failSlot(serverWorld, GameWorldComponent.KEY.get(serverWorld), point, level, player,
                    SchulteResultS2CPacket.OUTCOME_FAIL_WRONG, "tip.breacher.repair.fail_wrong");
            return;
        }

        slot.next++;
        if (slot.next >= slot.sequence.length) {
            point.levelDone[level] = true;
            point.slots[level] = null;
            point.completedBy.add(player.getUuid());
            recordLevelRepaired(player, level, point.pos);
            ServerPlayNetworking.send(player,
                    new SchulteResultS2CPacket(point.id, level, SchulteResultS2CPacket.OUTCOME_LEVEL_DONE));
            player.sendMessage(Text.translatable("tip.breacher.repair.level_done",
                            LEVEL_SIZE[level] + "x" + LEVEL_SIZE[level])
                    .withColor(Noellesroles.BREACHER.color() & 0xFFFFFF), true);
            if (point.allLevelsDone()) {
                completePoint(serverWorld, point);
            }
        }
    }

    public void handleRepairExit(ServerPlayerEntity player, int pointId, int level) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (level < 0 || level >= LEVEL_SIZE.length) return;
        PointState point = findPoint(pointId);
        if (point == null) return;
        Slot slot = point.slots[level];
        if (slot == null || point.levelDone[level] || !slot.occupant.equals(player.getUuid())) return;
        failSlot(serverWorld, GameWorldComponent.KEY.get(serverWorld), point, level, player,
                -1, "tip.breacher.repair.fail_exit");
    }

    private void failSlot(ServerWorld serverWorld, GameWorldComponent game, PointState point,
                          int level, ServerPlayerEntity player, int outcome, String messageKey) {
        point.slots[level] = null;
        BreacherPlayerComponent.KEY.get(player).setRepairCooldownTicks(REPAIR_COOLDOWN_TICKS);
        player.sendMessage(Text.translatable(messageKey), true);
        if (outcome >= 0) {
            ServerPlayNetworking.send(player,
                    new SchulteResultS2CPacket(point.id, level, outcome));
        }
    }

    private void completePoint(ServerWorld serverWorld, PointState point) {
        restoreLightsFor(serverWorld, point);
        removeMarker(serverWorld, point);
        points.remove(point);
        // 破坏点被全部修复 → 移除所有玩家的「去修复破坏点。」任务
        if (points.isEmpty()) {
            removeRepairTask(serverWorld);
        }

        PlayerEntity owner = serverWorld.getPlayerByUuid(point.owner);
        if (owner instanceof ServerPlayerEntity serverOwner) {
            BreacherPlayerComponent.KEY.get(serverOwner).changeOwnActivePointCount(-1);
            if (GameFunctions.isPlayerPlayingAndAlive(serverOwner)) {
                serverOwner.sendMessage(Text.translatable("tip.breacher.repaired_owner")
                        .withColor(Noellesroles.BREACHER.color() & 0xFFFFFF), true);
            }
        }

    }

    /** 任意玩家完成某一等级修复即记录回放；不再单独记录破坏点被完全修复。 */
    private void recordLevelRepaired(ServerPlayerEntity completer, int level, BlockPos pos) {
        NbtCompound extra = new NbtCompound();
        extra.putString("action", "level_repair");
        extra.putInt("level", level);
        extra.putInt("x", pos.getX());
        extra.putInt("y", pos.getY());
        extra.putInt("z", pos.getZ());
        GameRecordManager.recordSkillUse(completer, Noellesroles.BREACHER_ID, null, extra);
    }

    // ==================== 查询与清理 ====================

    public int activePointCount() {
        return points.size();
    }

    public PointState findPoint(int pointId) {
        for (PointState point : points) {
            if (point.id == pointId) return point;
        }
        return null;
    }

    public PointState findPointByMarker(UUID markerUuid) {
        if (markerUuid == null) return null;
        for (PointState point : points) {
            if (markerUuid.equals(point.markerEntityUuid)) return point;
        }
        return null;
    }

    private void removeMarker(ServerWorld serverWorld, PointState point) {
        if (point.markerEntityUuid == null) return;
        Entity entity = serverWorld.getEntity(point.markerEntityUuid);
        if (entity != null) {
            entity.discard();
        }
        point.markerEntityUuid = null;
    }

    public void clearAll(ServerWorld serverWorld) {
        if (breachBlackout) {
            breachBlackout = false;
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                player.removeStatusEffect(StatusEffects.BLINDNESS);
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
        for (PointState point : points) {
            removeMarker(serverWorld, point);
            PlayerEntity owner = serverWorld.getPlayerByUuid(point.owner);
            if (owner instanceof ServerPlayerEntity serverOwner) {
                BreacherPlayerComponent.KEY.get(serverOwner).changeOwnActivePointCount(-1);
            }
        }
        for (DamagedLight damaged : damagedLights) {
            BlockState state = serverWorld.getBlockState(damaged.pos);
            if (!state.contains(Properties.LIT) || !state.contains(WatheProperties.ACTIVE)) continue;
            serverWorld.setBlockState(damaged.pos,
                    state.with(Properties.LIT, damaged.originalLit).with(WatheProperties.ACTIVE, damaged.originalActive),
                    Block.NOTIFY_ALL);
        }
        points.clear();
        damagedLights.clear();
        lightCache = null;
        nextPointId = 1;
        removeRepairTask(serverWorld);
    }

    public static void cleanupMarkers(ServerWorld serverWorld) {
        for (BreacherMarkerEntity marker : serverWorld.getEntitiesByType(
                TypeFilter.instanceOf(BreacherMarkerEntity.class), marker -> true)) {
            marker.discard();
        }
    }

    public static boolean isBreacherMarker(Entity entity) {
        return entity instanceof BreacherMarkerEntity marker
                && marker.getCommandTags().contains(MARKER_TAG);
    }

    private static String ownerMarkerTag(UUID ownerUuid) {
        return MARKER_OWNER_TAG_PREFIX + ownerUuid;
    }

    private static int[] shuffledSequence(int total, net.minecraft.util.math.random.Random random) {
        int[] values = new int[total];
        for (int i = 0; i < total; i++) {
            values[i] = i + 1;
        }
        for (int i = total - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
        return values;
    }

    public static String levelLabel(int level) {
        return LEVEL_SIZE[level] + "x" + LEVEL_SIZE[level];
    }

    // ==================== 序列化 ====================

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("nextPointId", nextPointId);
        tag.putBoolean("breachBlackout", breachBlackout);
        NbtList pointList = new NbtList();
        for (PointState point : points) {
            NbtCompound pointTag = new NbtCompound();
            pointTag.putInt("id", point.id);
            pointTag.putUuid("owner", point.owner);
            pointTag.putInt("x", point.pos.getX());
            pointTag.putInt("y", point.pos.getY());
            pointTag.putInt("z", point.pos.getZ());
            if (point.markerEntityUuid != null) {
                pointTag.putUuid("marker", point.markerEntityUuid);
            }
            pointTag.putInt("payoutTicksRemaining", point.payoutTicksRemaining);
            NbtList doneList = new NbtList();
            for (boolean done : point.levelDone) {
                doneList.add(NbtByte.of(done));
            }
            pointTag.put("levelDone", doneList);
            NbtList slotList = new NbtList();
            for (int level = 0; level < point.slots.length; level++) {
                Slot slot = point.slots[level];
                if (slot == null || point.levelDone[level]) continue;
                NbtCompound slotTag = new NbtCompound();
                slotTag.putInt("level", level);
                slotTag.putUuid("occupant", slot.occupant);
                slotTag.putInt("next", slot.next);
                slotTag.putInt("ticksRemaining", slot.ticksRemaining);
                NbtList seqList = new NbtList();
                for (int value : slot.sequence) {
                    seqList.add(net.minecraft.nbt.NbtInt.of(value));
                }
                slotTag.put("sequence", seqList);
                slotList.add(slotTag);
            }
            pointTag.put("slots", slotList);
            NbtList completedList = new NbtList();
            for (UUID completer : point.completedBy) {
                completedList.add(NbtHelper.fromUuid(completer));
            }
            pointTag.put("completedBy", completedList);
            pointList.add(pointTag);
        }
        tag.put("points", pointList);

        NbtList damagedList = new NbtList();
        for (DamagedLight damaged : damagedLights) {
            NbtCompound damagedTag = new NbtCompound();
            damagedTag.putInt("x", damaged.pos.getX());
            damagedTag.putInt("y", damaged.pos.getY());
            damagedTag.putInt("z", damaged.pos.getZ());
            damagedTag.putBoolean("originalLit", damaged.originalLit);
            damagedTag.putBoolean("originalActive", damaged.originalActive);
            damagedTag.putInt("pointId", damaged.pointId);
            damagedList.add(damagedTag);
        }
        tag.put("damagedLights", damagedList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        points.clear();
        damagedLights.clear();
        lightCache = null;
        nextPointId = tag.getInt("nextPointId");
        breachBlackout = tag.getBoolean("breachBlackout");

        NbtList pointList = tag.getList("points", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < pointList.size(); i++) {
            NbtCompound pointTag = pointList.getCompound(i);
            PointState point = new PointState();
            point.id = pointTag.getInt("id");
            point.owner = pointTag.getUuid("owner");
            point.pos = new BlockPos(pointTag.getInt("x"), pointTag.getInt("y"), pointTag.getInt("z"));
            point.markerEntityUuid = pointTag.containsUuid("marker") ? pointTag.getUuid("marker") : null;
            point.payoutTicksRemaining = pointTag.getInt("payoutTicksRemaining");
            if (point.payoutTicksRemaining <= 0) {
                point.payoutTicksRemaining = INCOME_INTERVAL_TICKS;
            }
            NbtList doneList = pointTag.getList("levelDone", NbtElement.BYTE_TYPE);
            for (int level = 0; level < point.levelDone.length && level < doneList.size(); level++) {
                point.levelDone[level] = ((NbtByte) doneList.get(level)).byteValue() != 0;
            }
            NbtList slotList = pointTag.getList("slots", NbtElement.COMPOUND_TYPE);
            for (int s = 0; s < slotList.size(); s++) {
                NbtCompound slotTag = slotList.getCompound(s);
                int level = slotTag.getInt("level");
                if (level < 0 || level >= point.slots.length) continue;
                NbtList seqList = slotTag.getList("sequence", NbtElement.INT_TYPE);
                int[] sequence = new int[seqList.size()];
                for (int k = 0; k < sequence.length; k++) {
                    sequence[k] = seqList.getInt(k);
                }
                Slot slot = new Slot();
                slot.occupant = slotTag.getUuid("occupant");
                slot.sequence = sequence;
                slot.next = slotTag.getInt("next");
                slot.ticksRemaining = slotTag.getInt("ticksRemaining");
                point.slots[level] = slot;
            }
            NbtList completedList = pointTag.getList("completedBy", NbtElement.INT_ARRAY_TYPE);
            for (int c = 0; c < completedList.size(); c++) {
                point.completedBy.add(net.minecraft.nbt.NbtHelper.toUuid(completedList.get(c)));
            }
            points.add(point);
        }

        NbtList damagedList = tag.getList("damagedLights", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < damagedList.size(); i++) {
            NbtCompound damagedTag = damagedList.getCompound(i);
            damagedLights.add(new DamagedLight(
                    new BlockPos(damagedTag.getInt("x"), damagedTag.getInt("y"), damagedTag.getInt("z")),
                    damagedTag.getBoolean("originalLit"),
                    damagedTag.getBoolean("originalActive"),
                    damagedTag.getInt("pointId")));
        }
    }
}

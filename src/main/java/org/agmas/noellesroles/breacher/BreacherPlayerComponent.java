package org.agmas.noellesroles.breacher;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.RoleUtils;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BreacherPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BreacherPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "breacher"), BreacherPlayerComponent.class);

    private static final int SKILL_COOLDOWN_TICKS = GameConstants.getInTicks(3, 0);
    private static final int POINT_CONFIRM_WINDOW_TICKS = GameConstants.getInTicks(1, 0);
    private static final int POINT_DURATION_TICKS = GameConstants.getInTicks(2, 0);
    private static final int MAINTAIN_TICKS_REQUIRED = GameConstants.getInTicks(0, 10);
    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final int REWARD_TOTAL = 100;
    private static final String MARKER_TAG = "noellesroles_breacher_marker";
    private static final String MARKER_OWNER_TAG_PREFIX = "noellesroles_breacher_owner_";

    private final PlayerEntity player;

    private int cooldownTicks = 0;
    private boolean pointPending = false;
    private BlockPos pendingPointPos = BlockPos.ORIGIN;
    private int pendingPointTicksRemaining = 0;

    private boolean pointActive = false;
    private BlockPos pointPos = BlockPos.ORIGIN;
    private int pointTicksRemaining = 0;
    private int maintainTicks = 0;
    private UUID markerEntityUuid = null;

    private int checkTick = 0;

    public BreacherPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        removeMarkerIfPresent();
        this.cooldownTicks = 0;
        this.pointPending = false;
        this.pendingPointPos = BlockPos.ORIGIN;
        this.pendingPointTicksRemaining = 0;
        this.pointActive = false;
        this.pointPos = BlockPos.ORIGIN;
        this.pointTicksRemaining = 0;
        this.maintainTicks = 0;
        this.markerEntityUuid = null;
        this.checkTick = 0;
        sync();
    }

    public boolean canUseSkill() {
        return this.cooldownTicks <= 0 && !this.pointActive;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
        sync();
    }

    public boolean tryCreatePoint() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return false;
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) return false;
        if (!GameFunctions.isPlayerPlayingAndAlive(this.player)) return false;
        if (SwallowedPlayerComponent.isPlayerSwallowed(this.player)) return false;
        if (!canUseSkill()) return false;

        if (this.pointPending) {
            return createPointAt(this.pendingPointPos);
        }

        BlockPos center = this.player.getBlockPos();
        if (Wathe.isSkyVisibleAdjacent(this.player)) {
            serverPlayer.sendMessage(Text.translatable("tip.breacher.fail_not_indoor"), true);
            return false;
        }
        if (!isStandableAir3x3(serverWorld, center)) {
            serverPlayer.sendMessage(Text.translatable("tip.breacher.fail_not_enough_space"), true);
            return false;
        }

        this.pointPending = true;
        this.pendingPointPos = center.toImmutable();
        this.pendingPointTicksRemaining = POINT_CONFIRM_WINDOW_TICKS;
        serverPlayer.sendMessage(Text.translatable("tip.breacher.marked", POINT_CONFIRM_WINDOW_TICKS / 20), true);
        sync();
        return true;
    }

    private boolean createPointAt(BlockPos center) {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return false;
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) return false;
        if (!isPlayerWithinPointArea(serverPlayer, center)) {
            clearPendingPoint();
            serverPlayer.sendMessage(Text.translatable("tip.breacher.fail_too_far"), true);
            sync();
            return false;
        }
        if (Wathe.isSkyVisibleAdjacent(this.player)) {
            clearPendingPoint();
            serverPlayer.sendMessage(Text.translatable("tip.breacher.fail_not_indoor"), true);
            sync();
            return false;
        }
        if (!isStandableAir3x3(serverWorld, center)) {
            clearPendingPoint();
            serverPlayer.sendMessage(Text.translatable("tip.breacher.fail_not_enough_space"), true);
            sync();
            return false;
        }

        ItemEntity marker = new ItemEntity(
                serverWorld,
                center.getX() + 0.5D,
                center.getY(),
                center.getZ() + 0.5D,
                new ItemStack(Items.DIAMOND_PICKAXE)
        );
        marker.setPickupDelay(Integer.MAX_VALUE);
        marker.setNeverDespawn();
        marker.setNoGravity(true);
        marker.setVelocity(0.0D, 0.0D, 0.0D);
        marker.setInvulnerable(true);
        marker.setCustomNameVisible(true);
        marker.setCustomName(Text.translatable("label.breacher.break_point"));
        marker.setGlowing(true);
        marker.setSilent(true);
        marker.addCommandTag(MARKER_TAG);
        marker.addCommandTag(ownerMarkerTag(this.player.getUuid()));
        if (!serverWorld.spawnEntity(marker)) {
            return false;
        }

        this.pointPending = false;
        this.pendingPointPos = BlockPos.ORIGIN;
        this.pendingPointTicksRemaining = 0;
        this.pointActive = true;
        this.pointPos = center.toImmutable();
        this.pointTicksRemaining = POINT_DURATION_TICKS;
        this.maintainTicks = 0;
        this.markerEntityUuid = marker.getUuid();
        playBreakPointCreatedSound(serverWorld);
        recordBreakPointPlaced(serverPlayer, center);
        sync();
        return true;
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

    @Override
    public void serverTick() {
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) return;

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            if (this.cooldownTicks == 0) {
                sync();
            }
        }

        if (this.pointPending) {
            this.pendingPointTicksRemaining--;
            if (this.pendingPointTicksRemaining <= 0) {
                this.pointPending = false;
                this.pendingPointPos = BlockPos.ORIGIN;
                this.pendingPointTicksRemaining = 0;
                sync();
            } else if (this.pendingPointTicksRemaining % 20 == 0) {
                sync();
            }
        }

        if (!this.pointActive) {
            if (this.cooldownTicks > 0 && this.cooldownTicks % 20 == 0) {
                sync();
            }
            return;
        }

        if (!isActivePointStillValid(serverWorld)) {
            clearPoint(serverWorld);
            if (shouldStartCooldownAfterCancel(serverWorld)) {
                startCooldown();
            }
            sync();
            return;
        }

        if (!pinMarkerInPlace(serverWorld)) {
            if (serverWorld.isChunkLoaded(this.pointPos)) {
                clearPoint(serverWorld);
                startCooldown();
                sync();
            }
            return;
        }

        this.pointTicksRemaining--;
        if (this.pointTicksRemaining <= 0) {
            grantRewardToKillerSide(serverWorld);
            recordBreakPointReward(serverWorld);
            clearPoint(serverWorld);
            startCooldown();
            sync();
            return;
        }

        if (++checkTick >= CHECK_INTERVAL_TICKS) {
            checkTick = 0;
            List<ServerPlayerEntity> maintainingPlayers = getMaintainingInnocents(serverWorld);
            if (!maintainingPlayers.isEmpty()) {
                this.maintainTicks += CHECK_INTERVAL_TICKS;
                sendMaintainProgressTip(maintainingPlayers);
                if (this.maintainTicks >= MAINTAIN_TICKS_REQUIRED) {
                    recordBreakPointMaintained(maintainingPlayers);
                    clearPoint(serverWorld);
                    startCooldown();
                    sync();
                    return;
                }
            } else {
                this.maintainTicks = 0;
            }
        }

        if (this.pointTicksRemaining % 20 == 0) {
            sync();
        }
    }

    private void grantRewardToKillerSide(ServerWorld serverWorld) {
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        List<ServerPlayerEntity> killerPlayers = new ArrayList<>();
        for (UUID uuid : game.getAllPlayers()) {
            PlayerEntity target = serverWorld.getPlayerByUuid(uuid);
            if (!(target instanceof ServerPlayerEntity serverPlayer)) continue;
            if (!GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) continue;
            if (SwallowedPlayerComponent.isPlayerSwallowed(serverPlayer)) continue;
            if (!RoleUtils.isActualKillerRole(game.getRole(serverPlayer))) continue;
            killerPlayers.add(serverPlayer);
        }
        if (killerPlayers.isEmpty()) return;

        int each = REWARD_TOTAL / killerPlayers.size();
        int remainder = REWARD_TOTAL % killerPlayers.size();
        for (int i = 0; i < killerPlayers.size(); i++) {
            int reward = each + (i < remainder ? 1 : 0);
            PlayerShopComponent.KEY.get(killerPlayers.get(i)).addToBalance(reward);
        }
    }

    private List<ServerPlayerEntity> getMaintainingInnocents(ServerWorld serverWorld) {
        List<ServerPlayerEntity> maintainingPlayers = new ArrayList<>();
        BlockPos min = this.pointPos.add(-1, 0, -1);
        BlockPos max = this.pointPos.add(1, 1, 1);
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        for (ServerPlayerEntity serverPlayer : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) continue;
            if (SwallowedPlayerComponent.isPlayerSwallowed(serverPlayer)) continue;
            if (RoleUtils.isActualKillerRole(game.getRole(serverPlayer))) continue;
            BlockPos pos = serverPlayer.getBlockPos();
            if (pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                maintainingPlayers.add(serverPlayer);
            }
        }
        return maintainingPlayers;
    }

    private void sendMaintainProgressTip(List<ServerPlayerEntity> maintainingPlayers) {
        int secondsRemaining = Math.max(0, (MAINTAIN_TICKS_REQUIRED - this.maintainTicks + 19) / 20);
        Text tip = Text.translatable("tip.breacher.maintaining", secondsRemaining)
                .withColor(Noellesroles.BREACHER.color() & 0xFFFFFF);
        for (ServerPlayerEntity maintainingPlayer : maintainingPlayers) {
            maintainingPlayer.sendMessage(tip, true);
        }
    }

    private void clearPendingPoint() {
        this.pointPending = false;
        this.pendingPointPos = BlockPos.ORIGIN;
        this.pendingPointTicksRemaining = 0;
    }

    private boolean isActivePointStillValid(ServerWorld serverWorld) {
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        return game.isRunning()
                && game.isRole(this.player, Noellesroles.BREACHER)
                && GameFunctions.isPlayerPlayingAndAlive(this.player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(this.player);
    }

    private boolean shouldStartCooldownAfterCancel(ServerWorld serverWorld) {
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        return game.isRunning()
                && game.isRole(this.player, Noellesroles.BREACHER)
                && GameFunctions.isPlayerPlayingAndAlive(this.player);
    }

    private boolean isPlayerWithinPointArea(PlayerEntity player, BlockPos center) {
        BlockPos min = center.add(-1, 0, -1);
        BlockPos max = center.add(1, 1, 1);
        BlockPos pos = player.getBlockPos();
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private boolean isStandableAir3x3(ServerWorld world, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos feet = center.add(dx, 0, dz);
                BlockPos head = feet.up();
                BlockPos below = feet.down();
                if (!world.isAir(feet) || !world.isAir(head)) return false;
                BlockState belowState = world.getBlockState(below);
                if (!belowState.isSideSolidFullSquare(world, below, net.minecraft.util.math.Direction.UP)) return false;
            }
        }
        return true;
    }

    private boolean pinMarkerInPlace(ServerWorld world) {
        if (this.markerEntityUuid == null) return false;
        var entity = world.getEntity(this.markerEntityUuid);
        if (entity == null) return false;
        entity.setPosition(this.pointPos.getX() + 0.5D, this.pointPos.getY(), this.pointPos.getZ() + 0.5D);
        entity.setVelocity(0.0D, 0.0D, 0.0D);
        return true;
    }

    private void removeMarkerIfPresent() {
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) return;
        removeOwnedMarkers(serverWorld, this.player.getUuid(), this.markerEntityUuid);
    }

    private void clearPoint(ServerWorld serverWorld) {
        removeOwnedMarkers(serverWorld, this.player.getUuid(), this.markerEntityUuid);
        this.pointActive = false;
        this.pointTicksRemaining = 0;
        this.maintainTicks = 0;
        this.markerEntityUuid = null;
        this.checkTick = 0;
    }

    public static void cleanupMarkers(ServerWorld serverWorld) {
        for (ItemEntity marker : serverWorld.getEntitiesByType(TypeFilter.equals(ItemEntity.class),
                entity -> entity.getCommandTags().contains(MARKER_TAG))) {
            marker.discard();
        }
    }

    private static void removeOwnedMarkers(ServerWorld serverWorld, UUID ownerUuid, UUID markerUuid) {
        if (markerUuid != null) {
            var entity = serverWorld.getEntity(markerUuid);
            if (entity != null) {
                entity.discard();
            }
        }
        String ownerTag = ownerMarkerTag(ownerUuid);
        for (ItemEntity marker : serverWorld.getEntitiesByType(TypeFilter.equals(ItemEntity.class),
                entity -> entity.getCommandTags().contains(ownerTag))) {
            marker.discard();
        }
    }

    private static String ownerMarkerTag(UUID ownerUuid) {
        return MARKER_OWNER_TAG_PREFIX + ownerUuid;
    }

    private void recordBreakPointPlaced(ServerPlayerEntity breacher, BlockPos pos) {
        NbtCompound extra = createPositionExtra("place", pos);
        GameRecordManager.recordSkillUse(breacher, Noellesroles.BREACHER_ID, null, extra);
    }

    private void recordBreakPointMaintained(List<ServerPlayerEntity> maintainingPlayers) {
        if (!(this.player instanceof ServerPlayerEntity breacher) || maintainingPlayers.isEmpty()) return;

        NbtCompound extra = createPositionExtra("maintain", this.pointPos);
        extra.putInt("maintainerCount", maintainingPlayers.size());
        GameRecordManager.recordSkillUse(breacher, Noellesroles.BREACHER_ID, maintainingPlayers.get(0), extra);
    }

    private void recordBreakPointReward(ServerWorld serverWorld) {
        if (!(this.player instanceof ServerPlayerEntity breacher)) return;

        NbtCompound extra = createPositionExtra("reward", this.pointPos);
        extra.putInt("reward", REWARD_TOTAL);
        GameRecordManager.recordSkillUse(breacher, Noellesroles.BREACHER_ID, null, extra);
    }

    private NbtCompound createPositionExtra(String action, BlockPos pos) {
        NbtCompound extra = new NbtCompound();
        extra.putString("action", action);
        extra.putInt("x", pos.getX());
        extra.putInt("y", pos.getY());
        extra.putInt("z", pos.getZ());
        return extra;
    }

    private void startCooldown() {
        this.cooldownTicks = SKILL_COOLDOWN_TICKS;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public boolean hasPendingPoint() {
        return this.pointPending;
    }

    public int getPendingPointTicksRemaining() {
        return this.pendingPointTicksRemaining;
    }

    public boolean isPointActive() {
        return this.pointActive;
    }

    public int getPointTicksRemaining() {
        return this.pointTicksRemaining;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("cooldownTicks", this.cooldownTicks);
        tag.putBoolean("pointPending", this.pointPending);
        if (this.pointPending) {
            tag.putInt("pendingPointX", this.pendingPointPos.getX());
            tag.putInt("pendingPointY", this.pendingPointPos.getY());
            tag.putInt("pendingPointZ", this.pendingPointPos.getZ());
            tag.putInt("pendingPointTicksRemaining", this.pendingPointTicksRemaining);
        }
        tag.putBoolean("pointActive", this.pointActive);
        if (this.pointActive) {
            tag.putInt("pointX", this.pointPos.getX());
            tag.putInt("pointY", this.pointPos.getY());
            tag.putInt("pointZ", this.pointPos.getZ());
            tag.putInt("pointTicksRemaining", this.pointTicksRemaining);
            tag.putInt("maintainTicks", this.maintainTicks);
        }
        if (this.markerEntityUuid != null) {
            tag.putUuid("markerEntityUuid", this.markerEntityUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.cooldownTicks = tag.getInt("cooldownTicks");
        this.pointPending = tag.getBoolean("pointPending");
        if (this.pointPending) {
            this.pendingPointPos = new BlockPos(tag.getInt("pendingPointX"), tag.getInt("pendingPointY"), tag.getInt("pendingPointZ"));
            this.pendingPointTicksRemaining = tag.getInt("pendingPointTicksRemaining");
        } else {
            this.pendingPointPos = BlockPos.ORIGIN;
            this.pendingPointTicksRemaining = 0;
        }
        this.pointActive = tag.getBoolean("pointActive");
        if (this.pointActive) {
            this.pointPos = new BlockPos(tag.getInt("pointX"), tag.getInt("pointY"), tag.getInt("pointZ"));
            this.pointTicksRemaining = tag.getInt("pointTicksRemaining");
            this.maintainTicks = tag.getInt("maintainTicks");
        } else {
            this.pointPos = BlockPos.ORIGIN;
            this.pointTicksRemaining = 0;
            this.maintainTicks = 0;
        }
        this.markerEntityUuid = tag.containsUuid("markerEntityUuid") ? tag.getUuid("markerEntityUuid") : null;
    }
}

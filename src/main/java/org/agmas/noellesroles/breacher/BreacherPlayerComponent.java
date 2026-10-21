package org.agmas.noellesroles.breacher;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class BreacherPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BreacherPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "breacher"), BreacherPlayerComponent.class);

    public static final int SKILL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    private static final int POINT_CONFIRM_WINDOW_TICKS = GameConstants.getInTicks(1, 0);

    private final PlayerEntity player;

    private int cooldownTicks = 0;
    private boolean pointPending = false;
    private BlockPos pendingPointPos = BlockPos.ORIGIN;
    private int pendingPointTicksRemaining = 0;

    /** 个人修复冷却（修复失败/退出小游戏后 15 秒内不能开始任何舒尔特方格）。 */
    private int repairCooldownTicks = 0;

    /** 本人当前存在的破坏点数（真相在世界组件，这里只为 HUD 同步）。 */
    private int ownActivePointCount = 0;

    public BreacherPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.cooldownTicks = 0;
        this.pointPending = false;
        this.pendingPointPos = BlockPos.ORIGIN;
        this.pendingPointTicksRemaining = 0;
        this.repairCooldownTicks = 0;
        this.ownActivePointCount = 0;
        sync();
    }

    public boolean canUseSkill() {
        return this.cooldownTicks <= 0;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
        sync();
    }

    public void changeOwnActivePointCount(int delta) {
        this.ownActivePointCount = Math.max(0, this.ownActivePointCount + delta);
        sync();
    }

    public boolean tryCreatePoint() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return false;
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) return false;
        if (!GameFunctions.isPlayerPlayingAndAlive(this.player)) return false;
        if (SwallowedPlayerComponent.isPlayerSwallowed(this.player)) return false;
        if (!canUseSkill()) return false;

        if (this.pointPending) {
            return createPointAt(this.pendingPointPos, serverWorld);
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

    private boolean createPointAt(BlockPos center, ServerWorld serverWorld) {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return false;
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

        clearPendingPoint();
        if (!BreacherWorldComponent.KEY.get(serverWorld).createPoint(serverPlayer, center)) {
            sync();
            return false;
        }

        // 技能 CD 固定 90 秒，自生成破坏点起算（与破坏点是否消失无关）
        this.cooldownTicks = SKILL_COOLDOWN_TICKS;
        sync();
        return true;
    }

    @Override
    public void serverTick() {
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) return;

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            if (this.cooldownTicks == 0 || this.cooldownTicks % 20 == 0) {
                sync();
            }
        }

        if (this.repairCooldownTicks > 0) {
            this.repairCooldownTicks--;
            if (this.repairCooldownTicks == 0 || this.repairCooldownTicks % 20 == 0) {
                sync();
            }
        }

        if (this.pointPending) {
            this.pendingPointTicksRemaining--;
            if (this.pendingPointTicksRemaining <= 0) {
                clearPendingPoint();
                sync();
            } else if (this.pendingPointTicksRemaining % 20 == 0) {
                sync();
            }
        }
    }

    private void clearPendingPoint() {
        this.pointPending = false;
        this.pendingPointPos = BlockPos.ORIGIN;
        this.pendingPointTicksRemaining = 0;
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
                if (!belowState.isSideSolidFullSquare(world, below, Direction.UP)) return false;
            }
        }
        return true;
    }

    public void setRepairCooldownTicks(int repairCooldownTicks) {
        this.repairCooldownTicks = Math.max(0, repairCooldownTicks);
        sync();
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public int getRepairCooldownTicks() {
        return this.repairCooldownTicks;
    }

    public int getOwnActivePointCount() {
        return this.ownActivePointCount;
    }

    public boolean hasPendingPoint() {
        return this.pointPending;
    }

    public int getPendingPointTicksRemaining() {
        return this.pendingPointTicksRemaining;
    }

    public void sync() {
        KEY.sync(this.player);
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
        tag.putInt("repairCooldownTicks", this.repairCooldownTicks);
        tag.putInt("ownActivePointCount", this.ownActivePointCount);
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
        this.repairCooldownTicks = tag.getInt("repairCooldownTicks");
        this.ownActivePointCount = tag.getInt("ownActivePointCount");
    }
}

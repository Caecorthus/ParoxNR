package org.agmas.noellesroles.recaller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 回溯者锚点的世界级状态：全场同时只存在一个锚点（后放置的取代先放置的）。
 * 锚点实体状态全部存于本组件（实体 NBT 留空），世界重载后实体丢失由 serverTick 按存储坐标重建。
 * 同步给所有客户端（不重写 shouldSyncWith）：客户端 HUD 需要读“是否有锚点/是不是我放的”，
 * 渲染器/描边门控需要读实体与放置者信息。
 */
public class RecallerAnchorWorldComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<RecallerAnchorWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "recaller_anchor"), RecallerAnchorWorldComponent.class);

    private final World world;

    private boolean placed = false;
    private UUID ownerUuid = null;
    private double x = 0;
    private double y = 0;
    private double z = 0;
    private UUID markerEntityUuid = null;

    public RecallerAnchorWorldComponent(World world) {
        this.world = world;
    }

    // ==================== 操作 ====================

    /** 在玩家当前位置精确坐标放置锚点；已有锚点则先移除旧实体（取代语义）。 */
    public void placeAnchor(ServerPlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        discardMarker(serverWorld);
        spawnMarker(serverWorld, player.getX(), player.getY(), player.getZ());
        this.ownerUuid = player.getUuid();
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.placed = true;
        KEY.sync(world);
    }

    /**
     * 回收锚点：移除实体并清状态（返还物品由调用方处理）。回收资格（仅回溯者职业）
     * 由 RecallerAnchorInteractHandler 把关，本方法只做状态校验；无锚点返回 false。
     */
    public boolean retrieveAnchor() {
        if (!(world instanceof ServerWorld serverWorld)) return false;
        if (!placed) return false;
        discardMarker(serverWorld);
        clearState();
        KEY.sync(world);
        return true;
    }

    /** 碎片传送：先把玩家送到锚点坐标（调用方应先取好坐标再传送）。 */
    public void teleportShardUser(ServerPlayerEntity player) {
        // 传送前先下车（例如从沙发上站起来）
        if (player.hasVehicle()) {
            player.stopRiding();
        }
        // 无条件传送（不走 LivingEntity.teleport 的安全检查，同旧技能写法）
        player.requestTeleport(x, y, z);
    }

    // ==================== 查询 ====================

    public boolean isPlaced() {
        return placed;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /** 实体 UUID 与登记值比对，防过期实体（世界重载后遗留的旧实体）触发交互。 */
    public boolean matchesMarker(UUID entityUuid) {
        return placed && markerEntityUuid != null && markerEntityUuid.equals(entityUuid);
    }

    // ==================== tick ====================

    @Override
    public void serverTick() {
        if (!(world instanceof ServerWorld serverWorld)) return;
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        if (!game.isRunning()) {
            if (placed) {
                // 回合结束/重置：自动清锚点
                discardMarker(serverWorld);
                clearState();
                KEY.sync(world);
            }
            return;
        }
        if (placed) {
            pinMarkerInPlace(serverWorld);
        }
    }

    private void pinMarkerInPlace(ServerWorld serverWorld) {
        net.minecraft.entity.Entity entity = markerEntityUuid == null
                ? null : serverWorld.getEntity(markerEntityUuid);
        if (entity == null || entity.isRemoved()) {
            // 实体意外丢失（地图重置等）：区块已加载时按存储坐标原地重建
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            if (entity != null || !serverWorld.isChunkLoaded(pos)) return;
            spawnMarker(serverWorld, x, y, z);
            KEY.sync(world);
            return;
        }
        entity.setPosition(x, y, z);
        entity.setVelocity(0.0D, 0.0D, 0.0D);
    }

    private void spawnMarker(ServerWorld serverWorld, double px, double py, double pz) {
        RecallerAnchorEntity marker = new RecallerAnchorEntity(
                NoellesRolesEntities.RECALLER_ANCHOR_ENTITY, serverWorld);
        marker.refreshPositionAndAngles(px, py, pz, 0.0F, 0.0F);
        marker.setCustomName(Text.translatable("label.recaller.anchor"));
        if (!serverWorld.spawnEntity(marker)) {
            markerEntityUuid = null;
            return;
        }
        markerEntityUuid = marker.getUuid();
    }

    private void discardMarker(ServerWorld serverWorld) {
        if (markerEntityUuid == null) return;
        net.minecraft.entity.Entity entity = serverWorld.getEntity(markerEntityUuid);
        if (entity != null) {
            entity.discard();
        }
        markerEntityUuid = null;
    }

    private void clearState() {
        placed = false;
        ownerUuid = null;
        x = 0;
        y = 0;
        z = 0;
        markerEntityUuid = null;
    }

    // ==================== 同步 ====================

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, @NotNull ServerPlayerEntity recipient) {
        buf.writeBoolean(placed);
        if (placed) {
            buf.writeUuid(ownerUuid);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.placed = buf.readBoolean();
        if (this.placed) {
            this.ownerUuid = buf.readUuid();
            this.x = buf.readDouble();
            this.y = buf.readDouble();
            this.z = buf.readDouble();
        } else {
            clearState();
        }
    }

    // ==================== 序列化 ====================

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("placed", placed);
        if (placed) {
            tag.putUuid("owner", ownerUuid);
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            if (markerEntityUuid != null) {
                tag.putUuid("marker", markerEntityUuid);
            }
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        clearState();
        this.placed = tag.contains("placed") && tag.getBoolean("placed");
        if (!this.placed) return;
        if (tag.containsUuid("owner")) {
            this.ownerUuid = tag.getUuid("owner");
        }
        this.x = tag.getDouble("x");
        this.y = tag.getDouble("y");
        this.z = tag.getDouble("z");
        // marker 实体丢失时保持原 UUID 让 serverTick 的 pinMarkerInPlace 走重建路径
        if (tag.containsUuid("marker")) {
            this.markerEntityUuid = tag.getUuid("marker");
        }
    }
}

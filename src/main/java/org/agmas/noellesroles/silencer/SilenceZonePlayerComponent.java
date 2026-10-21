package org.agmas.noellesroles.silencer;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 无声区域锚点组件（寂静装置）。
 * 挂在被指定的目标玩家身上：以该玩家为圆心、半径 {@link #ZONE_RADIUS} 格内
 * 的玩家听不到任何声音、也无法通过语音被他人听到。区域随目标移动。
 * 剩余 tick 由服务端倒计时并同步到所有客户端。
 */
public class SilenceZonePlayerComponent implements Component, AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SilenceZonePlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "silence_zone"),
            SilenceZonePlayerComponent.class
    );

    /** 无声区域持续 25 秒 */
    public static final int ZONE_DURATION_TICKS = GameConstants.getInTicks(0, 25);
    /** 无声区域半径（格） */
    public static final double ZONE_RADIUS = 15.0;
    public static final double ZONE_RADIUS_SQUARED = ZONE_RADIUS * ZONE_RADIUS;

    private final PlayerEntity player;
    private int zoneTicks = 0;
    private UUID zoneBy = null;

    public SilenceZonePlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /** 在本玩家周围创造（或刷新）无声区域 */
    public void applyZone(UUID silencerUuid) {
        this.zoneTicks = ZONE_DURATION_TICKS;
        this.zoneBy = silencerUuid;
        sync();
    }

    public boolean isZoneActive() {
        return this.zoneTicks > 0;
    }

    public int getZoneTicks() {
        return this.zoneTicks;
    }

    public UUID getZoneBy() {
        return this.zoneBy;
    }

    public void reset() {
        this.zoneTicks = 0;
        this.zoneBy = null;
        sync();
    }

    /** 判断玩家是否处于任何激活的无声区域内（区域锚点本人也在区域内）。 */
    public static boolean isPlayerInSilenceZone(PlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerPlayingAndAlive(player)) return false;
        for (PlayerEntity anchor : player.getWorld().getPlayers()) {
            SilenceZonePlayerComponent comp = KEY.get(anchor);
            if (comp.isZoneActive() && player.squaredDistanceTo(anchor) <= ZONE_RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void serverTick() {
        if (this.zoneTicks > 0) {
            // 锚点死亡/离场则区域提前结束
            if (!GameFunctions.isPlayerPlayingAndAlive(this.player)) {
                this.zoneTicks = 0;
                this.zoneBy = null;
                sync();
                return;
            }
            this.zoneTicks--;
            // 每 2 tick 同步一次，保证区域结束时刻客户端表现基本即时
            if (this.zoneTicks % 2 == 0) {
                sync();
            }
            if (this.zoneTicks <= 0) {
                this.zoneBy = null;
                sync();
            }
        }
    }

    public void sync() {
        if (this.player instanceof ServerPlayerEntity) {
            KEY.sync(this.player);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.zoneTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.zoneTicks = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("zoneTicks", this.zoneTicks);
        if (this.zoneBy != null) {
            tag.putUuid("zoneBy", this.zoneBy);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.zoneTicks = tag.contains("zoneTicks") ? tag.getInt("zoneTicks") : 0;
        this.zoneBy = tag.containsUuid("zoneBy") ? tag.getUuid("zoneBy") : null;
    }
}

package org.agmas.noellesroles.taotie;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * Component for players who have been swallowed by Taotie.
 * 消化倒计时由被吞者自己的组件推进：只依赖被吞者自身的实体与组件，不需要反向查找饕餮。
 */
public class SwallowedPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SwallowedPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "swallowed"), SwallowedPlayerComponent.class);

    // 被吞者从入腹到被消化死亡的时长
    public static final int DIGESTION_TICKS = GameConstants.getInTicks(1, 0);

    private final PlayerEntity player;
    private boolean isSwallowed = false;
    private UUID swallowedBy = null;
    private GameMode previousGameMode = GameMode.ADVENTURE;
    // 剩余消化时间（ticks），仅服务端推进；同步给客户端供饕餮 HUD 显示
    private int digestTicks = 0;

    public static boolean isPlayerSwallowed(PlayerEntity player)
    {
        return KEY.get(player).isSwallowed;
    }

    public SwallowedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        // 重置摄像机到玩家自己
        if (this.isSwallowed && player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.setCameraEntity(serverPlayer);
            serverPlayer.noClip = false;
            serverPlayer.setInvisible(false);
        }
        this.isSwallowed = false;
        this.swallowedBy = null;
        this.previousGameMode = GameMode.ADVENTURE;
        this.digestTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // Sync to all players so they know who is swallowed
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.isSwallowed);
        buf.writeBoolean(this.swallowedBy != null);
        if (this.swallowedBy != null) {
            buf.writeUuid(this.swallowedBy);
        }
        buf.writeEnumConstant(this.previousGameMode);
        buf.writeInt(this.digestTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.isSwallowed = buf.readBoolean();
        if (buf.readBoolean()) {
            this.swallowedBy = buf.readUuid();
        } else {
            this.swallowedBy = null;
        }
        this.previousGameMode = buf.readEnumConstant(GameMode.class);
        this.digestTicks = buf.readInt();
    }

    /**
     * 剩余消化时间（秒），客户端 HUD 用
     */
    public int getDigestSeconds() {
        return this.digestTicks / 20;
    }

    @Override
    public void serverTick() {
        if (!this.isSwallowed || this.digestTicks <= 0) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (GameWorldComponent.KEY.get(serverPlayer.getServerWorld()).getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) return;

        this.digestTicks--;
        if (this.digestTicks % 20 == 0) {
            this.sync();
        }

        if (this.digestTicks <= 0) {
            digest(serverPlayer);
        }
    }

    /**
     * 消化结束：被吞者死亡，并让饕餮回补饥饿值（在死亡回调中按死亡原因结算）
     */
    private void digest(ServerPlayerEntity serverPlayer) {
        PlayerEntity taotieEntity = this.swallowedBy == null
                ? null
                : serverPlayer.getServerWorld().getPlayerByUuid(this.swallowedBy);
        ServerPlayerEntity taotie = taotieEntity instanceof ServerPlayerEntity serverTaotie ? serverTaotie : null;
        System.out.println("[TAOTIE-DEBUG] digest kill victim=" + serverPlayer.getName().getString()
                + " taotieResolved=" + (taotie != null));
        GameFunctions.killPlayer(serverPlayer, false, taotie, Noellesroles.DEATH_REASON_DIGESTED, true);
    }

    /**
     * Mark this player as swallowed by the given Taotie
     * Keeps the player in a fake spectator state and teleports to Taotie's position
     */
    public void setSwallowed(UUID taotieUuid) {
        this.isSwallowed = true;
        this.swallowedBy = taotieUuid;
        this.digestTicks = DIGESTION_TICKS;
        System.out.println("[TAOTIE-DEBUG] swallow start victim=" + player.getName().getString()
                + " taotie=" + taotieUuid + " ticks=" + DIGESTION_TICKS);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            this.previousGameMode = serverPlayer.interactionManager.getGameMode();
            serverPlayer.noClip = true;
            serverPlayer.setInvisible(true);
            serverPlayer.setOnGround(true);
            serverPlayer.setVelocity(Vec3d.ZERO);
            serverPlayer.fallDistance = 0.0F;

            // Teleport to Taotie's position
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity taotie = serverWorld.getPlayerByUuid(taotieUuid);
                if (taotie != null) {
                    serverPlayer.teleport(serverWorld, taotie.getX(), taotie.getY(), taotie.getZ(),
                            serverPlayer.getYaw(), serverPlayer.getPitch());
                    // Set camera to follow Taotie
                    serverPlayer.setCameraEntity(taotie);
                }
            }
        }

        this.sync();
    }

    /**
     * Release this player from Taotie's stomach
     * Teleports to release position and restores a playable game mode
     */
    public void release(Vec3d position) {
        if (!isSwallowed) return;
        this.isSwallowed = false;
        this.swallowedBy = null;
        this.digestTicks = 0;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Reset camera to self first
            serverPlayer.setCameraEntity(serverPlayer);
            serverPlayer.noClip = false;
            serverPlayer.setInvisible(false);
            serverPlayer.setOnGround(true);
            serverPlayer.setVelocity(Vec3d.ZERO);
            serverPlayer.fallDistance = 0.0F;

            GameMode restoredGameMode = this.previousGameMode == GameMode.SPECTATOR
                    ? GameMode.ADVENTURE
                    : this.previousGameMode;
            serverPlayer.changeGameMode(restoredGameMode);

            // Teleport to release position
            serverPlayer.teleport((ServerWorld) serverPlayer.getWorld(),
                    position.x, position.y, position.z,
                    serverPlayer.getYaw(), serverPlayer.getPitch());
        }
        this.previousGameMode = GameMode.ADVENTURE;
        this.sync();
    }


    public boolean isSwallowed() {
        return isSwallowed;
    }

    public UUID getSwallowedBy() {
        return swallowedBy;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("isSwallowed", this.isSwallowed);
        if (this.swallowedBy != null) {
            tag.putUuid("swallowedBy", this.swallowedBy);
        }
        tag.putString("previousGameMode", this.previousGameMode.getName());
        tag.putInt("digestTicks", this.digestTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.isSwallowed = tag.getBoolean("isSwallowed");
        if (tag.contains("swallowedBy")) {
            this.swallowedBy = tag.getUuid("swallowedBy");
        } else {
            this.swallowedBy = null;
        }
        this.previousGameMode = GameMode.byName(tag.getString("previousGameMode"), GameMode.ADVENTURE);
        this.digestTicks = tag.getInt("digestTicks");
    }
}

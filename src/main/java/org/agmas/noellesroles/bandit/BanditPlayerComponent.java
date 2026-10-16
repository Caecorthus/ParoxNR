package org.agmas.noellesroles.bandit;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 强盗玩家当前世界里可回收的飞斧数（同步给本人，供 HUD 显示；回收购买在 BanditShopHandler）。
 */
public class BanditPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BanditPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "bandit"), BanditPlayerComponent.class);

    private static final int SYNC_INTERVAL_TICKS = 20;

    private final PlayerEntity player;
    private int recoverableAxes = 0;
    private int scanTimer = 0;

    public BanditPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.recoverableAxes = 0;
        this.scanTimer = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.recoverableAxes);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.recoverableAxes = buf.readInt();
    }

    @Override
    public void serverTick() {
        if (--this.scanTimer <= 0) {
            this.scanTimer = SYNC_INTERVAL_TICKS;
            int count = 0;
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
            if (gameWorld.isRole(this.player, Noellesroles.BANDIT)
                    && GameFunctions.isPlayerPlayingAndAlive(this.player)
                    && this.player instanceof ServerPlayerEntity serverPlayer) {
                count = BanditSkillHelper.findRecoverableAxes(serverPlayer).size();
            }
            if (count != this.recoverableAxes) {
                this.recoverableAxes = count;
                this.sync();
            }
        }
    }

    public int getRecoverableAxes() {
        return this.recoverableAxes;
    }

    public void setRecoverableAxes(int count) {
        this.recoverableAxes = count;
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }
}

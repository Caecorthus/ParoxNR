package org.agmas.noellesroles.baosang;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class BaosangPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BaosangPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "baosang"), BaosangPlayerComponent.class);

    public static final int PENDING_DURATION = GameConstants.getInTicks(0, 10);
    public static final int COOLDOWN_DURATION = GameConstants.getInTicks(0, 30);

    private final PlayerEntity player;
    private int pendingTicks = 0;
    private String victimName = "";

    public BaosangPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.pendingTicks = 0;
        this.victimName = "";
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void startPending(String victimName) {
        this.pendingTicks = PENDING_DURATION;
        this.victimName = victimName;
        this.sync();
    }

    public int getPendingTicks() {
        return this.pendingTicks;
    }

    public String getVictimName() {
        return this.victimName;
    }

    @Override
    public void serverTick() {
        if (this.pendingTicks > 0) {
            this.pendingTicks--;
            if (this.pendingTicks <= 0) {
                if (this.player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.translatable("tip.baosang.announcement", this.victimName), true);
                }
                AbilityPlayerComponent.KEY.get(this.player).setCooldown(COOLDOWN_DURATION);
                this.victimName = "";
            }
            if (this.pendingTicks % 20 == 0 || this.pendingTicks <= 0) {
                this.sync();
            }
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.pendingTicks);
        buf.writeString(this.victimName);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.pendingTicks = buf.readInt();
        this.victimName = buf.readString();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("pendingTicks", this.pendingTicks);
        tag.putString("victimName", this.victimName);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.pendingTicks = tag.contains("pendingTicks") ? tag.getInt("pendingTicks") : 0;
        this.victimName = tag.contains("victimName") ? tag.getString("victimName") : "";
    }
}

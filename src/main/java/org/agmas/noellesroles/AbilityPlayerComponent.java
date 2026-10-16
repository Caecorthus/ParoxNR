package org.agmas.noellesroles;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class AbilityPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<AbilityPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "ability"), AbilityPlayerComponent.class);
    private final PlayerEntity player;
    public int cooldown = 0;
    private int pendingKnifeCooldownOverride = 0;
    private int knifeCooldownOverrideTicks = 0;

    public void reset() {
        this.cooldown = 0;
        this.pendingKnifeCooldownOverride = 0;
        this.knifeCooldownOverrideTicks = 0;
        this.sync();
    }

    public AbilityPlayerComponent(PlayerEntity player) {
        this.player = player;
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
        buf.writeInt(this.cooldown);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.cooldown = buf.readInt();
    }

    public void clientTick() {
        if (this.cooldown > 1) {
            this.cooldown--;
        }
    }

    public void serverTick() {
        if (this.cooldown > 0) {
            --this.cooldown;
            if (this.cooldown % 20 == 0) {
                this.sync();
            }
        }

        if (this.pendingKnifeCooldownOverride > 0) {
            this.player.getItemCooldownManager().set(dev.doctor4t.wathe.index.WatheItems.KNIFE, this.pendingKnifeCooldownOverride);
            this.pendingKnifeCooldownOverride = 0;
        }

        // 粘性覆写:每 tick 递减重写,使 Wathe 刺杀收尾写入的默认长冷却无法盖掉短冷却
        if (this.knifeCooldownOverrideTicks > 0) {
            this.player.getItemCooldownManager().set(dev.doctor4t.wathe.index.WatheItems.KNIFE, this.knifeCooldownOverrideTicks);
            --this.knifeCooldownOverrideTicks;
        }
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(int ticks) {
        this.cooldown = ticks;
        this.sync();
    }

    public void markKnifeCooldownOverride(int ticks) {
        this.pendingKnifeCooldownOverride = ticks;
    }

    public void startKnifeCooldownOverride(int ticks) {
        this.knifeCooldownOverrideTicks = Math.max(this.knifeCooldownOverrideTicks, ticks);
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("cooldown", this.cooldown);
        tag.putInt("pendingKnifeCooldownOverride", this.pendingKnifeCooldownOverride);
        tag.putInt("knifeCooldownOverrideTicks", this.knifeCooldownOverrideTicks);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.cooldown = tag.contains("cooldown") ? tag.getInt("cooldown") : 0;
        this.pendingKnifeCooldownOverride = tag.contains("pendingKnifeCooldownOverride") ? tag.getInt("pendingKnifeCooldownOverride") : 0;
        this.knifeCooldownOverrideTicks = tag.contains("knifeCooldownOverrideTicks") ? tag.getInt("knifeCooldownOverrideTicks") : 0;
    }
}

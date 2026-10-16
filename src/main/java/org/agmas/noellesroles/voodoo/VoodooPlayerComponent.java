package org.agmas.noellesroles.voodoo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

public class VoodooPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<VoodooPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "voodoo"), VoodooPlayerComponent.class);
    private final PlayerEntity player;
    public UUID target;
    private int dollPurchases = 0;

    public void reset() {
        this.target = player.getUuid();
        this.dollPurchases = 0;
        this.sync();
    }

    public VoodooPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void setTarget(UUID target) {
        this.target = target;
        this.sync();
    }

    public int getDollPurchases() {
        return this.dollPurchases;
    }

    public void addDollPurchase() {
        this.dollPurchases++;
        this.sync();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putUuid("target", this.target != null ? this.target : player.getUuid());
        tag.putInt("dollPurchases", this.dollPurchases);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.target = tag.contains("target") ? tag.getUuid("target") : player.getUuid();
        this.dollPurchases = tag.contains("dollPurchases") ? tag.getInt("dollPurchases") : 0;
    }
}

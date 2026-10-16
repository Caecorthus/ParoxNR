package org.agmas.noellesroles.toxicologist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
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

/**
 * 解毒剂预防护状态，挂在被保护玩家身上。
 * 一次性：挡住下一次中毒后消失；回合重置（ResetPlayer）时清除。
 * 仅同步给毒理学家，用于在客户端渲染明绿色描边。
 */
public class AntidoteShieldPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<AntidoteShieldPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "antidote_shield"),
            AntidoteShieldPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean shielded = false;

    public AntidoteShieldPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void applyShield() {
        this.shielded = true;
        this.sync();
    }

    public void consumeShield() {
        this.shielded = false;
        this.sync();
    }

    public boolean hasShield() {
        return this.shielded;
    }

    public void reset() {
        this.shielded = false;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(recipient.getWorld());
        return gameWorld.isRole(recipient, Noellesroles.TOXICOLOGIST);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.shielded);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.shielded = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("shielded", this.shielded);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.shielded = tag.contains("shielded") && tag.getBoolean("shielded");
    }
}

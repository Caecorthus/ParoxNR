package org.agmas.noellesroles.toxicologist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashSet;
import java.util.Set;

/**
 * 解毒剂对餐盘/饮品托盘的预防护状态（世界级，一次性）。
 * 护盾挡下一次下毒后消失；回合结束时 reset。
 * 仅同步给毒理学家，用于在客户端渲染明绿色粒子提示。
 */
public class AntidotePlateShieldComponent implements AutoSyncedComponent {
    public static final ComponentKey<AntidotePlateShieldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "antidote_plate_shield"),
            AntidotePlateShieldComponent.class
    );

    private final Set<BlockPos> shieldedPlates = new HashSet<>();

    public AntidotePlateShieldComponent(World world) {
    }

    public boolean hasShield(BlockPos pos) {
        return this.shieldedPlates.contains(pos);
    }

    public void applyShield(BlockPos pos) {
        this.shieldedPlates.add(pos.toImmutable());
    }

    public boolean consumeShield(BlockPos pos) {
        return this.shieldedPlates.remove(pos);
    }

    public void reset() {
        this.shieldedPlates.clear();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(recipient.getWorld());
        return gameWorld.isRole(recipient, Noellesroles.TOXICOLOGIST);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.shieldedPlates.size());
        for (BlockPos pos : this.shieldedPlates) {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.shieldedPlates.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            this.shieldedPlates.add(new BlockPos(x, y, z));
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (BlockPos pos : this.shieldedPlates) {
            NbtCompound entry = new NbtCompound();
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            list.add(entry);
        }
        tag.put("shieldedPlates", list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.shieldedPlates.clear();
        NbtList list = tag.getList("shieldedPlates", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            this.shieldedPlates.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
        }
    }
}

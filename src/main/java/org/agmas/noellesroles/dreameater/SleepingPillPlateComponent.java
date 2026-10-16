package org.agmas.noellesroles.dreameater;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 被安眠药下毒的餐盘/饮品托盘状态（世界级）。
 * 记录 餐盘坐标 -> 下毒者UUID：用于按「下毒物品」区分安眠药与普通毒药、
 * 蓝粒子渲染、以及取餐时把安眠药标记转移到食物上。
 * 仅同步给可能看到毒粒子的玩家（杀手阵营/旁观/毒理学家，毒理学家用于抑制红粒子），回合结束 reset。
 */
public class SleepingPillPlateComponent implements AutoSyncedComponent {
    public static final ComponentKey<SleepingPillPlateComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "sleeping_pill_plate"),
            SleepingPillPlateComponent.class
    );

    private final Map<BlockPos, String> pillPlates = new HashMap<>();

    public SleepingPillPlateComponent(World world) {
    }

    /** 该餐盘是否为安眠药盘（坐标与当前下毒者 UUID 均需吻合）。 */
    public boolean isPillPlate(BlockPos pos, String poisoner) {
        return poisoner != null && poisoner.equals(this.pillPlates.get(pos));
    }

    public void markPlate(BlockPos pos, String poisoner) {
        this.pillPlates.put(pos.toImmutable(), poisoner);
    }

    public void clearPlate(BlockPos pos) {
        this.pillPlates.remove(pos);
    }

    public void reset() {
        this.pillPlates.clear();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(recipient.getWorld());
        return gameWorld.canUseKillerFeatures(recipient)
                || !GameFunctions.isPlayerPlayingAndAlive(recipient)
                || gameWorld.isRole(recipient, Noellesroles.TOXICOLOGIST);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(this.pillPlates.size());
        for (Map.Entry<BlockPos, String> entry : this.pillPlates.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            buf.writeString(entry.getValue());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.pillPlates.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            this.pillPlates.put(buf.readBlockPos(), buf.readString());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Map.Entry<BlockPos, String> entry : this.pillPlates.entrySet()) {
            NbtCompound entryTag = new NbtCompound();
            entryTag.putInt("x", entry.getKey().getX());
            entryTag.putInt("y", entry.getKey().getY());
            entryTag.putInt("z", entry.getKey().getZ());
            entryTag.putString("poisoner", entry.getValue());
            list.add(entryTag);
        }
        tag.put("pillPlates", list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.pillPlates.clear();
        NbtList list = tag.getList("pillPlates", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entryTag = list.getCompound(i);
            this.pillPlates.put(
                    new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z")),
                    entryTag.getString("poisoner"));
        }
    }
}

package org.agmas.noellesroles.ferryman;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 世界级组件：摆渡人技能二“不腐处理”过的尸体（尸体实体UUID → 冻结的age）。
 * 尸体age由 {@link FerrymanHelper#updatePreservedBodies} 每tick钳制回冻结值，永不白骨化。
 * “哪些尸体被处理过”的名单仅同步给验尸官、杀手与中立玩家（乘客不可见）。
 */
public class FerrymanPreserveWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<FerrymanPreserveWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "ferryman_preserve"), FerrymanPreserveWorldComponent.class);

    private static final int MAX_PRESERVED_SYNC = 4096;

    private final World world;
    private final Map<UUID, Integer> preservedAges = new HashMap<>();

    public FerrymanPreserveWorldComponent(World world) {
        this.world = world;
    }

    /** 登记一具不腐尸体；已登记则保留首次冻结的age。 */
    public void preserve(UUID bodyUuid, int frozenAge) {
        if (bodyUuid == null) {
            return;
        }
        if (this.preservedAges.putIfAbsent(bodyUuid, frozenAge) == null) {
            KEY.sync(this.world);
        }
    }

    public boolean isPreserved(UUID bodyUuid) {
        return bodyUuid != null && this.preservedAges.containsKey(bodyUuid);
    }

    public void unpreserve(UUID bodyUuid) {
        if (this.preservedAges.remove(bodyUuid) != null) {
            KEY.sync(this.world);
        }
    }

    /** 当前tick钳制逻辑使用的快照，避免遍历中并发修改。 */
    public Map<UUID, Integer> snapshot() {
        return new HashMap<>(this.preservedAges);
    }

    public boolean isEmpty() {
        return this.preservedAges.isEmpty();
    }

    public void reset() {
        if (!this.preservedAges.isEmpty()) {
            this.preservedAges.clear();
            KEY.sync(this.world);
        }
    }

    /** 涂蜡特效与不腐名单仅验尸官、杀手与中立玩家可见。 */
    public static boolean canSeePreserveEffects(World world, net.minecraft.entity.player.PlayerEntity viewer) {
        if (viewer == null) {
            return false;
        }
        Role role = GameWorldComponent.KEY.get(world).getRole(viewer);
        if (role == null) {
            return false;
        }
        return Noellesroles.CORONER_ID.equals(role.identifier()) || role.canUseKiller() || role.isNeutral();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        if (this.preservedAges.isEmpty()) {
            return true; // 空名单没有信息量，任何人收到都无所谓
        }
        return canSeePreserveEffects(this.world, recipient);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.preservedAges.size());
        for (Map.Entry<UUID, Integer> entry : this.preservedAges.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.preservedAges.clear();
        int count = buf.readInt();
        if (count < 0 || count > MAX_PRESERVED_SYNC) {
            throw new io.netty.handler.codec.DecoderException("Invalid preservedAges size: " + count);
        }
        for (int i = 0; i < count; i++) {
            this.preservedAges.put(buf.readUuid(), buf.readInt());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Integer> entry : this.preservedAges.entrySet()) {
            NbtCompound bodyTag = new NbtCompound();
            bodyTag.putUuid("body", entry.getKey());
            bodyTag.putInt("age", entry.getValue());
            list.add(bodyTag);
        }
        tag.put("preserved", list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.preservedAges.clear();
        if (!tag.contains("preserved", NbtElement.LIST_TYPE)) {
            return;
        }
        NbtList list = tag.getList("preserved", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound bodyTag = list.getCompound(i);
            if (bodyTag.containsUuid("body")) {
                this.preservedAges.put(bodyTag.getUuid("body"), bodyTag.getInt("age"));
            }
        }
    }
}

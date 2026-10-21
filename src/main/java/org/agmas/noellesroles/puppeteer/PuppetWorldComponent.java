package org.agmas.noellesroles.puppeteer;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 世界级组件：本局所有存活傀儡（生前玩家 UUID → 傀儡状态），以及做过傀儡、不能再转化的尸体实体 UUID。
 * 同步给全员：库存中的傀儡只同步给其主人；场内傀儡供选人面板与本能透视使用。
 */
public class PuppetWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<PuppetWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "puppets"), PuppetWorldComponent.class);

    private final World world;
    private final Map<UUID, PuppetRecord> puppets = new LinkedHashMap<>();
    private final Set<UUID> spentBodies = new HashSet<>();

    public PuppetWorldComponent(World world) {
        this.world = world;
    }

    public static PuppetWorldComponent get(World world) {
        return KEY.get(world);
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Nullable
    public PuppetRecord get(@Nullable UUID sourceUuid) {
        return sourceUuid == null ? null : this.puppets.get(sourceUuid);
    }

    public void add(PuppetRecord record) {
        this.puppets.put(record.getSourceUuid(), record);
    }

    @Nullable
    public PuppetRecord remove(UUID sourceUuid) {
        return this.puppets.remove(sourceUuid);
    }

    public Collection<PuppetRecord> all() {
        return this.puppets.values();
    }

    public List<PuppetRecord> getOwnedBy(UUID ownerUuid) {
        List<PuppetRecord> result = new ArrayList<>();
        for (PuppetRecord record : this.puppets.values()) {
            if (record.getOwnerUuid().equals(ownerUuid)) {
                result.add(record);
            }
        }
        return result;
    }

    /** 该 UUID 是否对应一具在场内（闲置或被操纵）的傀儡。 */
    public boolean isPuppetInWorld(@Nullable UUID sourceUuid) {
        PuppetRecord record = get(sourceUuid);
        return record != null && record.isInWorld();
    }

    @Nullable
    public PuppetRecord findByEntity(UUID entityUuid) {
        for (PuppetRecord record : this.puppets.values()) {
            if (entityUuid.equals(record.getEntityUuid())) {
                return record;
            }
        }
        return null;
    }

    @Nullable
    public PuppetRecord findControlledBy(UUID ownerUuid) {
        for (PuppetRecord record : this.puppets.values()) {
            if (record.getState() == PuppetRecord.State.CONTROLLED && record.getOwnerUuid().equals(ownerUuid)) {
                return record;
            }
        }
        return null;
    }

    public void markSpentBody(UUID bodyUuid) {
        this.spentBodies.add(bodyUuid);
    }

    public boolean isSpentBody(UUID bodyUuid) {
        return this.spentBodies.contains(bodyUuid);
    }

    public void reset() {
        if (!this.puppets.isEmpty() || !this.spentBodies.isEmpty()) {
            this.puppets.clear();
            this.spentBodies.clear();
            sync();
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        List<PuppetRecord> visible = new ArrayList<>();
        for (PuppetRecord record : this.puppets.values()) {
            if (record.getState() != PuppetRecord.State.STORED || record.getOwnerUuid().equals(recipient.getUuid())) {
                visible.add(record);
            }
        }
        buf.writeVarInt(visible.size());
        for (PuppetRecord record : visible) {
            buf.writeUuid(record.getSourceUuid());
            buf.writeUuid(record.getOwnerUuid());
            buf.writeByte(record.getState().ordinal());
            buf.writeBoolean(record.getEntityUuid() != null);
            if (record.getEntityUuid() != null) {
                buf.writeUuid(record.getEntityUuid());
            }
        }
        buf.writeVarInt(this.spentBodies.size());
        for (UUID uuid : this.spentBodies) {
            buf.writeUuid(uuid);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.puppets.clear();
        int count = buf.readVarInt();
        PuppetRecord.State[] states = PuppetRecord.State.values();
        for (int i = 0; i < count; i++) {
            UUID source = buf.readUuid();
            UUID owner = buf.readUuid();
            int stateOrdinal = buf.readByte();
            UUID entity = buf.readBoolean() ? buf.readUuid() : null;
            PuppetRecord record = new PuppetRecord(source, owner, GameConstants.DeathReasons.GENERIC, 0L);
            record.setState(stateOrdinal >= 0 && stateOrdinal < states.length ? states[stateOrdinal] : PuppetRecord.State.STORED);
            record.setEntityUuid(entity);
            this.puppets.put(source, record);
        }
        this.spentBodies.clear();
        int spentCount = buf.readVarInt();
        for (int i = 0; i < spentCount; i++) {
            this.spentBodies.add(buf.readUuid());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (PuppetRecord record : this.puppets.values()) {
            list.add(record.writeNbt(registryLookup));
        }
        tag.put("puppets", list);
        NbtList spent = new NbtList();
        for (UUID uuid : this.spentBodies) {
            spent.add(NbtString.of(uuid.toString()));
        }
        tag.put("spentBodies", spent);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.puppets.clear();
        NbtList list = tag.getList("puppets", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            PuppetRecord record = PuppetRecord.readNbt(list.getCompound(i), registryLookup);
            if (record != null) {
                this.puppets.put(record.getSourceUuid(), record);
            }
        }
        this.spentBodies.clear();
        NbtList spent = tag.getList("spentBodies", NbtElement.STRING_TYPE);
        for (int i = 0; i < spent.size(); i++) {
            try {
                this.spentBodies.add(UUID.fromString(spent.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}

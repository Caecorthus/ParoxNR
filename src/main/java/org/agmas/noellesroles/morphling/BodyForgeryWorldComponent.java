package org.agmas.noellesroles.morphling;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 世界级组件：赝造药水伪装过的尸体（尸体实体UUID → 指定玩家的皮肤UUID），同步给所有客户端用于渲染。 */
public class BodyForgeryWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<BodyForgeryWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "body_forgery"), BodyForgeryWorldComponent.class);

    private final World world;
    private final Map<UUID, UUID> forgedSkins = new HashMap<>();

    public BodyForgeryWorldComponent(World world) {
        this.world = world;
    }

    public void setForgedSkin(UUID bodyUuid, UUID fakeSkinUuid) {
        this.forgedSkins.put(bodyUuid, fakeSkinUuid);
        KEY.sync(this.world);
    }

    public @Nullable UUID getForgedSkin(UUID bodyUuid) {
        return this.forgedSkins.get(bodyUuid);
    }

    public void reset() {
        if (!this.forgedSkins.isEmpty()) {
            this.forgedSkins.clear();
            KEY.sync(this.world);
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.forgedSkins.size());
        for (Map.Entry<UUID, UUID> entry : this.forgedSkins.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeUuid(entry.getValue());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.forgedSkins.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            this.forgedSkins.put(buf.readUuid(), buf.readUuid());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound forgedTag = new NbtCompound();
        int i = 0;
        for (Map.Entry<UUID, UUID> entry : this.forgedSkins.entrySet()) {
            String prefix = "forgery_" + i;
            forgedTag.putUuid(prefix + "_body", entry.getKey());
            forgedTag.putUuid(prefix + "_skin", entry.getValue());
            i++;
        }
        forgedTag.putInt("count", this.forgedSkins.size());
        tag.put("forgedSkins", forgedTag);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.forgedSkins.clear();
        if (!tag.contains("forgedSkins", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        NbtCompound forgedTag = tag.getCompound("forgedSkins");
        int count = forgedTag.getInt("count");
        for (int i = 0; i < count; i++) {
            String prefix = "forgery_" + i;
            if (forgedTag.containsUuid(prefix + "_body") && forgedTag.containsUuid(prefix + "_skin")) {
                this.forgedSkins.put(forgedTag.getUuid(prefix + "_body"), forgedTag.getUuid(prefix + "_skin"));
            }
        }
    }
}

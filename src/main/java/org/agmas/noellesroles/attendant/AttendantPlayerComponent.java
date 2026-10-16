package org.agmas.noellesroles.attendant;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 乘务员登记表数据：每位乘客的登记条目 + 登记后的短暂高亮计时。 */
public class AttendantPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<AttendantPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "attendant"), AttendantPlayerComponent.class);

    public static final byte UNKNOWN = 0;
    public static final byte YES = 1;
    public static final byte NO = 2;

    public static final int HIGHLIGHT_TICKS = 20;

    private final PlayerEntity player;
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private int highlightEntityId = -1;
    private int highlightTicks = 0;

    public AttendantPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.entries.clear();
        this.highlightEntityId = -1;
        this.highlightTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    // ==== 登记条目 ====

    /** 从房间分配与玩家档案补全缺失条目；只填空缺，不覆盖已有（含手动修改的）信息。 */
    public void ensureEntries() {
        if (this.player.getWorld().isClient()) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        HashMap<Integer, GameWorldComponent.RoomData> rooms = gameWorld.getRooms();
        HashMap<UUID, GameProfile> profiles = gameWorld.getGameProfiles();
        boolean changed = false;

        for (UUID uuid : gameWorld.getAllPlayers()) {
            Entry entry = this.entries.get(uuid);
            if (entry == null) {
                entry = new Entry(uuid);
                // 乘务员自己的信息开局自动登记：见面、存活、职业为乘务员
                if (uuid.equals(this.player.getUuid())) {
                    entry.met = YES;
                    entry.alive = YES;
                    entry.roleId = Noellesroles.ATTENDANT_ID.toString();
                }
                this.entries.put(uuid, entry);
                changed = true;
            }
            if (entry.name.isEmpty()) {
                GameProfile profile = profiles.get(uuid);
                if (profile != null && profile.getName() != null) {
                    entry.name = profile.getName();
                    changed = true;
                }
            }
            if (entry.roomName.isEmpty()) {
                int roomIndex = gameWorld.getPlayerRoomIndex(uuid);
                GameWorldComponent.RoomData room = rooms.get(roomIndex);
                if (room != null) {
                    entry.roomIndex = roomIndex;
                    entry.roomName = room.getName();
                    changed = true;
                }
            }
        }

        if (changed) {
            this.sync();
        }
    }

    public List<Entry> getEntryList() {
        return new ArrayList<>(this.entries.values());
    }

    public Entry getEntry(UUID uuid) {
        return this.entries.get(uuid);
    }

    /** @return 是否从非"是"状态变更为"是"（已是则不变更，也不高亮） */
    public boolean markMet(UUID target) {
        Entry entry = this.entries.get(target);
        if (entry == null || entry.met == YES) {
            return false;
        }
        entry.met = YES;
        this.sync();
        return true;
    }

    /** @return 是否从非"是"状态变更为"是"（已是则不变更） */
    public boolean markAlive(UUID target) {
        Entry entry = this.entries.get(target);
        if (entry == null || entry.alive == YES) {
            return false;
        }
        entry.alive = YES;
        this.sync();
        return true;
    }

    /** @return 是否从非"否"状态变更为"否"（已是则不变更，也不高亮） */
    public boolean markDead(UUID target) {
        Entry entry = this.entries.get(target);
        if (entry == null || entry.alive == NO) {
            return false;
        }
        entry.alive = NO;
        this.sync();
        return true;
    }

    public boolean setField(UUID target, int field, String value) {
        Entry entry = this.entries.get(target);
        if (entry == null) {
            return false;
        }
        switch (field) {
            case 0 -> entry.met = parseTriState(value, entry.met);
            case 1 -> entry.alive = parseTriState(value, entry.alive);
            case 2 -> entry.roleId = value;
            default -> {
                return false;
            }
        }
        this.sync();
        return true;
    }

    private static byte parseTriState(String value, byte fallback) {
        return switch (value) {
            case "0" -> UNKNOWN;
            case "1" -> YES;
            case "2" -> NO;
            default -> fallback;
        };
    }

    // ==== 高亮 ====

    public void highlight(Entity entity, int ticks) {
        this.highlightEntityId = entity.getId();
        this.highlightTicks = Math.max(ticks, 0);
        this.sync();
    }

    public int getHighlightEntityId() {
        return this.highlightEntityId;
    }

    public int getHighlightTicks() {
        return this.highlightTicks;
    }

    @Override
    public void clientTick() {
        if (this.highlightTicks > 0) {
            this.highlightTicks--;
        }
    }

    @Override
    public void serverTick() {
        if (this.highlightTicks > 0) {
            this.highlightTicks--;
            if (this.highlightTicks == 0) {
                this.sync();
            }
        }
    }

    // ==== 序列化 ====

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Entry entry : this.entries.values()) {
            NbtCompound compound = new NbtCompound();
            compound.putString("uuid", entry.uuid.toString());
            compound.putString("name", entry.name);
            compound.putInt("roomIndex", entry.roomIndex);
            compound.putString("roomName", entry.roomName);
            compound.putByte("met", entry.met);
            compound.putByte("alive", entry.alive);
            compound.putString("roleId", entry.roleId);
            list.add(compound);
        }
        tag.put("entries", list);
        tag.putInt("highlightEntityId", this.highlightEntityId);
        tag.putInt("highlightTicks", this.highlightTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.entries.clear();
        NbtList list = tag.getList("entries", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound compound = list.getCompound(i);
            Entry entry = new Entry(UUID.fromString(compound.getString("uuid")));
            entry.name = compound.getString("name");
            entry.roomIndex = compound.contains("roomIndex") ? compound.getInt("roomIndex") : -1;
            entry.roomName = compound.getString("roomName");
            entry.met = compound.contains("met") ? compound.getByte("met") : UNKNOWN;
            entry.alive = compound.contains("alive") ? compound.getByte("alive") : UNKNOWN;
            entry.roleId = compound.getString("roleId");
            this.entries.put(entry.uuid, entry);
        }
        this.highlightEntityId = tag.contains("highlightEntityId") ? tag.getInt("highlightEntityId") : -1;
        this.highlightTicks = tag.contains("highlightTicks") ? tag.getInt("highlightTicks") : 0;
    }

    public static final class Entry {
        private final UUID uuid;
        private String name = "";
        private int roomIndex = -1;
        private String roomName = "";
        private byte met = UNKNOWN;
        private byte alive = UNKNOWN;
        private String roleId = "";

        private Entry(UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public String getName() {
            return this.name;
        }

        public String getRoomName() {
            return this.roomName;
        }

        public int getRoomIndex() {
            return this.roomIndex;
        }

        public byte getMet() {
            return this.met;
        }

        public byte getAlive() {
            return this.alive;
        }

        public String getRoleId() {
            return this.roleId;
        }
    }
}

package org.agmas.noellesroles.junker;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 世界级组件：记录死者掉落结算后留在身上的物品，以及已被搜刮过的尸体。
 * 只把"已搜刮"集合同步给客户端（用于准星提示），尸体物品快照仅保留在服务端。
 */
public class JunkerLootWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<JunkerLootWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "junker_loot"),
            JunkerLootWorldComponent.class
    );

    private final World world;
    private final Map<UUID, List<ItemStack>> lootSnapshots = new HashMap<>();
    private final Set<UUID> searchedCorpses = new HashSet<>();

    public JunkerLootWorldComponent(World world) {
        this.world = world;
    }

    /** 记录死者主物品栏中剩下的物品；需在死亡掉落结算之后调用。 */
    public void recordDeath(ServerPlayerEntity victim) {
        List<ItemStack> loot = new ArrayList<>();
        PlayerInventory inventory = victim.getInventory();
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty()) {
                loot.add(stack.copy());
            }
        }
        this.lootSnapshots.put(victim.getUuid(), loot);
        // 再次死亡意味着出现新尸体（如死斗场复活），旧的"已搜刮"记录作废
        this.searchedCorpses.remove(victim.getUuid());
    }

    public boolean hasLoot(UUID deadPlayerUuid) {
        List<ItemStack> loot = this.lootSnapshots.get(deadPlayerUuid);
        return loot != null && !loot.isEmpty();
    }

    @Nullable
    public ItemStack randomLoot(UUID deadPlayerUuid, Random random) {
        List<ItemStack> loot = this.lootSnapshots.get(deadPlayerUuid);
        if (loot == null || loot.isEmpty()) {
            return null;
        }
        return loot.get(random.nextInt(loot.size())).copy();
    }

    public boolean wasSearched(UUID deadPlayerUuid) {
        return this.searchedCorpses.contains(deadPlayerUuid);
    }

    public void markSearched(UUID deadPlayerUuid) {
        if (this.searchedCorpses.add(deadPlayerUuid)) {
            KEY.sync(this.world);
        }
    }

    public void reset() {
        this.lootSnapshots.clear();
        if (!this.searchedCorpses.isEmpty()) {
            this.searchedCorpses.clear();
            KEY.sync(this.world);
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.searchedCorpses.size());
        for (UUID uuid : this.searchedCorpses) {
            buf.writeUuid(uuid);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.searchedCorpses.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            this.searchedCorpses.add(buf.readUuid());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound lootTag = new NbtCompound();
        for (Map.Entry<UUID, List<ItemStack>> entry : this.lootSnapshots.entrySet()) {
            NbtList list = new NbtList();
            for (ItemStack stack : entry.getValue()) {
                list.add(stack.encode(registryLookup));
            }
            lootTag.put(entry.getKey().toString(), list);
        }
        tag.put("lootSnapshots", lootTag);

        NbtList searchedTag = new NbtList();
        for (UUID uuid : this.searchedCorpses) {
            searchedTag.add(NbtString.of(uuid.toString()));
        }
        tag.put("searchedCorpses", searchedTag);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.lootSnapshots.clear();
        if (tag.contains("lootSnapshots")) {
            NbtCompound lootTag = tag.getCompound("lootSnapshots");
            for (String key : lootTag.getKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    NbtList list = lootTag.getList(key, NbtElement.COMPOUND_TYPE);
                    List<ItemStack> loot = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        ItemStack stack = ItemStack.fromNbtOrEmpty(registryLookup, list.getCompound(i));
                        if (!stack.isEmpty()) {
                            loot.add(stack);
                        }
                    }
                    this.lootSnapshots.put(uuid, loot);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        this.searchedCorpses.clear();
        if (tag.contains("searchedCorpses")) {
            NbtList searchedTag = tag.getList("searchedCorpses", NbtString.STRING_TYPE);
            for (int i = 0; i < searchedTag.size(); i++) {
                try {
                    this.searchedCorpses.add(UUID.fromString(searchedTag.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}

package org.agmas.noellesroles.puppeteer;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一具傀儡的全部状态。傀儡以「生前玩家 UUID」为标识（一具尸体只会被做成一次傀儡）。
 * 物品栏只保存在服务端，同步给客户端时只带主人、状态与实体 UUID。
 */
public final class PuppetRecord {
    public enum State {
        /** 收在傀儡师身上，不在场内 */
        STORED,
        /** 静止留在场内，由 PuppetEntity 表示 */
        IDLE,
        /** 正被傀儡师操纵：傀儡师本人就是这具傀儡 */
        CONTROLLED
    }

    public static final int INVENTORY_SIZE = 41; // PlayerInventory: 36 主栏 + 4 护甲 + 1 副手

    private final UUID sourceUuid;
    private final UUID ownerUuid;
    private final Identifier deathReason;
    private final long deathGameTime;
    private State state = State.STORED;
    @Nullable private UUID entityUuid;
    private final List<ItemStack> inventory = new ArrayList<>();
    private int selectedSlot = 0;

    public PuppetRecord(UUID sourceUuid, UUID ownerUuid, Identifier deathReason, long deathGameTime) {
        this.sourceUuid = sourceUuid;
        this.ownerUuid = ownerUuid;
        this.deathReason = deathReason;
        this.deathGameTime = deathGameTime;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            this.inventory.add(ItemStack.EMPTY);
        }
    }

    public UUID getSourceUuid() {
        return this.sourceUuid;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public Identifier getDeathReason() {
        return this.deathReason;
    }

    public long getDeathGameTime() {
        return this.deathGameTime;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Nullable
    public UUID getEntityUuid() {
        return this.entityUuid;
    }

    public void setEntityUuid(@Nullable UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    /** 场内可被其他技能选中的傀儡（闲置或被操纵中）。 */
    public boolean isInWorld() {
        return this.state == State.IDLE || this.state == State.CONTROLLED;
    }

    /** 把玩家当前物品栏整体存进这具傀儡（并清空玩家物品栏）。 */
    public void takeInventoryFrom(PlayerInventory playerInventory) {
        int size = Math.min(INVENTORY_SIZE, playerInventory.size());
        for (int i = 0; i < size; i++) {
            this.inventory.set(i, playerInventory.getStack(i).copy());
            playerInventory.setStack(i, ItemStack.EMPTY);
        }
        this.selectedSlot = playerInventory.selectedSlot;
    }

    /** 把这具傀儡的物品栏放到玩家身上（玩家物品栏须已清空）。 */
    public void giveInventoryTo(PlayerInventory playerInventory) {
        int size = Math.min(INVENTORY_SIZE, playerInventory.size());
        for (int i = 0; i < size; i++) {
            playerInventory.setStack(i, this.inventory.get(i).copy());
        }
        playerInventory.selectedSlot = this.selectedSlot;
    }

    public void clearInventory() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            this.inventory.set(i, ItemStack.EMPTY);
        }
        this.selectedSlot = 0;
    }

    public NbtCompound writeNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("source", this.sourceUuid);
        tag.putUuid("owner", this.ownerUuid);
        tag.putString("deathReason", this.deathReason.toString());
        tag.putLong("deathGameTime", this.deathGameTime);
        tag.putString("state", this.state.name());
        if (this.entityUuid != null) {
            tag.putUuid("entity", this.entityUuid);
        }
        NbtList items = new NbtList();
        for (ItemStack stack : this.inventory) {
            items.add(stack.encodeAllowEmpty(registryLookup));
        }
        tag.put("inventory", items);
        tag.putInt("selectedSlot", this.selectedSlot);
        return tag;
    }

    @Nullable
    public static PuppetRecord readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (!tag.containsUuid("source") || !tag.containsUuid("owner")) {
            return null;
        }
        Identifier reason = Identifier.tryParse(tag.getString("deathReason"));
        PuppetRecord record = new PuppetRecord(tag.getUuid("source"), tag.getUuid("owner"),
                reason != null ? reason : GameConstants.DeathReasons.GENERIC, tag.getLong("deathGameTime"));
        try {
            record.state = State.valueOf(tag.getString("state"));
        } catch (IllegalArgumentException ignored) {
            record.state = State.STORED;
        }
        record.entityUuid = tag.containsUuid("entity") ? tag.getUuid("entity") : null;
        NbtList items = tag.getList("inventory", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < Math.min(INVENTORY_SIZE, items.size()); i++) {
            record.inventory.set(i, ItemStack.fromNbtOrEmpty(registryLookup, items.getCompound(i)));
        }
        record.selectedSlot = tag.getInt("selectedSlot");
        return record;
    }
}

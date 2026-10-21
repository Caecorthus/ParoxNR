package org.agmas.noellesroles.puppeteer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 傀儡师的个人状态：制造名额、傀儡术冷却、操纵状态（本体替身、暂存的本体物品栏与位置）。
 * 傀儡本身的数据在 {@link PuppetWorldComponent}。
 * 「正在操纵哪具傀儡」同步给全员（用于把傀儡师渲染成死者模样），其余字段只同步给本人。
 */
public class PuppeteerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PuppeteerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "puppeteer"), PuppeteerPlayerComponent.class);

    private final PlayerEntity player;
    private int maxPuppets = 0;
    private int madeCount = 0;
    private int puppetryCooldown = 0;
    private int switchCooldown = 0;

    @Nullable private UUID controlledSource;
    @Nullable private UUID shellUuid;
    private final List<ItemStack> bodyInventory = new ArrayList<>();
    private int bodySelectedSlot = 0;
    private Vec3d bodyPos = Vec3d.ZERO;
    private float bodyYaw = 0.0F;
    private float bodyPitch = 0.0F;
    /** 身上炸弹被安放时所处的傀儡形态（生前玩家 UUID），null 表示安放在本体上。仅服务端使用。 */
    @Nullable private UUID bombedSource;

    public PuppeteerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public static PuppeteerPlayerComponent get(PlayerEntity player) {
        return KEY.get(player);
    }

    /** 该玩家此刻是否正以傀儡形态行动。 */
    public static boolean isControllingPuppet(@Nullable PlayerEntity player) {
        return player != null && KEY.get(player).isControlling();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.maxPuppets = 0;
        this.madeCount = 0;
        this.puppetryCooldown = 0;
        this.switchCooldown = 0;
        this.controlledSource = null;
        this.shellUuid = null;
        this.bodyInventory.clear();
        this.bodySelectedSlot = 0;
        this.bodyPos = Vec3d.ZERO;
        this.bodyYaw = 0.0F;
        this.bodyPitch = 0.0F;
        this.bombedSource = null;
        sync();
    }

    public void initializeForGame(int killerCount) {
        reset();
        this.maxPuppets = PuppeteerRules.maxPuppets(killerCount);
        sync();
    }

    public int getMaxPuppets() {
        return this.maxPuppets;
    }

    public int getMadeCount() {
        return this.madeCount;
    }

    public boolean hasQuotaLeft() {
        return this.madeCount < this.maxPuppets;
    }

    public void incrementMadeCount() {
        this.madeCount++;
    }

    public int getPuppetryCooldown() {
        return this.puppetryCooldown;
    }

    public void setPuppetryCooldown(int ticks) {
        this.puppetryCooldown = Math.max(0, ticks);
    }

    public int getSwitchCooldown() {
        return this.switchCooldown;
    }

    public void setSwitchCooldown(int ticks) {
        this.switchCooldown = Math.max(0, ticks);
    }

    public boolean isControlling() {
        return this.controlledSource != null;
    }

    /** 正在操纵的傀儡（即其生前玩家 UUID，也是渲染所用的皮肤）。 */
    @Nullable
    public UUID getControlledSource() {
        return this.controlledSource;
    }

    @Nullable
    public UUID getShellUuid() {
        return this.shellUuid;
    }

    /** 炸弹客安放 / 传递炸弹到该玩家身上时调用：记下炸弹落在哪个形态上。 */
    public void onBombAttached() {
        this.bombedSource = this.controlledSource;
    }

    @Nullable
    public UUID getBombedSource() {
        return this.bombedSource;
    }

    public void clearBombedSource() {
        this.bombedSource = null;
    }

    public Vec3d getBodyPos() {
        return this.bodyPos;
    }

    public float getBodyYaw() {
        return this.bodyYaw;
    }

    public float getBodyPitch() {
        return this.bodyPitch;
    }

    /** 进入操纵：暂存本体物品栏与位置。 */
    void beginControl(UUID source, UUID shell, Vec3d pos, float yaw, float pitch) {
        PlayerInventory inventory = this.player.getInventory();
        this.bodyInventory.clear();
        for (int i = 0; i < PuppetRecord.INVENTORY_SIZE && i < inventory.size(); i++) {
            this.bodyInventory.add(inventory.getStack(i).copy());
            inventory.setStack(i, ItemStack.EMPTY);
        }
        this.bodySelectedSlot = inventory.selectedSlot;
        this.controlledSource = source;
        this.shellUuid = shell;
        this.bodyPos = pos;
        this.bodyYaw = yaw;
        this.bodyPitch = pitch;
    }

    /** 结束操纵：把暂存的本体物品栏放回玩家身上（玩家物品栏须已清空）。 */
    void endControl() {
        PlayerInventory inventory = this.player.getInventory();
        for (int i = 0; i < this.bodyInventory.size() && i < inventory.size(); i++) {
            inventory.setStack(i, this.bodyInventory.get(i).copy());
        }
        inventory.selectedSlot = this.bodySelectedSlot;
        this.bodyInventory.clear();
        this.controlledSource = null;
        this.shellUuid = null;
    }

    @Override
    public void serverTick() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        boolean changed = false;
        if (this.puppetryCooldown > 0) {
            this.puppetryCooldown--;
            changed = this.puppetryCooldown % 20 == 0;
        }
        if (this.switchCooldown > 0) {
            this.switchCooldown--;
            changed |= this.switchCooldown == 0;
        }

        if (this.isControlling()) {
            PuppeteerControl.tickController(serverPlayer, this);
        } else if (GameFunctions.isPlayerPlayingAndAlive(serverPlayer)
                && GameWorldComponent.KEY.get(serverPlayer.getWorld()).isRole(serverPlayer, Noellesroles.PUPPETEER)
                && !PuppetWorldComponent.get(serverPlayer.getWorld()).getOwnedBy(serverPlayer.getUuid()).isEmpty()
                && serverPlayer.isSprinting()) {
            // 拥有傀儡时本体无法疾跑（客户端同时松开疾跑键）
            serverPlayer.setSprinting(false);
        }

        if (changed) {
            sync();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.controlledSource != null);
        if (this.controlledSource != null) {
            buf.writeUuid(this.controlledSource);
        }
        boolean self = recipient == this.player;
        buf.writeBoolean(self);
        if (self) {
            buf.writeVarInt(this.maxPuppets);
            buf.writeVarInt(this.madeCount);
            buf.writeVarInt(this.puppetryCooldown);
            buf.writeVarInt(this.switchCooldown);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.controlledSource = buf.readBoolean() ? buf.readUuid() : null;
        if (buf.readBoolean()) {
            this.maxPuppets = buf.readVarInt();
            this.madeCount = buf.readVarInt();
            this.puppetryCooldown = buf.readVarInt();
            this.switchCooldown = buf.readVarInt();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("maxPuppets", this.maxPuppets);
        tag.putInt("madeCount", this.madeCount);
        tag.putInt("puppetryCooldown", this.puppetryCooldown);
        tag.putInt("switchCooldown", this.switchCooldown);
        if (this.controlledSource != null) {
            tag.putUuid("controlledSource", this.controlledSource);
        }
        if (this.shellUuid != null) {
            tag.putUuid("shell", this.shellUuid);
        }
        NbtList items = new NbtList();
        for (ItemStack stack : this.bodyInventory) {
            items.add(stack.encodeAllowEmpty(registryLookup));
        }
        tag.put("bodyInventory", items);
        tag.putInt("bodySelectedSlot", this.bodySelectedSlot);
        tag.putDouble("bodyX", this.bodyPos.x);
        tag.putDouble("bodyY", this.bodyPos.y);
        tag.putDouble("bodyZ", this.bodyPos.z);
        tag.putFloat("bodyYaw", this.bodyYaw);
        tag.putFloat("bodyPitch", this.bodyPitch);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.maxPuppets = tag.getInt("maxPuppets");
        this.madeCount = tag.getInt("madeCount");
        this.puppetryCooldown = tag.getInt("puppetryCooldown");
        this.switchCooldown = tag.getInt("switchCooldown");
        this.controlledSource = tag.containsUuid("controlledSource") ? tag.getUuid("controlledSource") : null;
        this.shellUuid = tag.containsUuid("shell") ? tag.getUuid("shell") : null;
        this.bodyInventory.clear();
        NbtList items = tag.getList("bodyInventory", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < items.size(); i++) {
            this.bodyInventory.add(ItemStack.fromNbtOrEmpty(registryLookup, items.getCompound(i)));
        }
        this.bodySelectedSlot = tag.getInt("bodySelectedSlot");
        this.bodyPos = new Vec3d(tag.getDouble("bodyX"), tag.getDouble("bodyY"), tag.getDouble("bodyZ"));
        this.bodyYaw = tag.getFloat("bodyYaw");
        this.bodyPitch = tag.getFloat("bodyPitch");
    }
}

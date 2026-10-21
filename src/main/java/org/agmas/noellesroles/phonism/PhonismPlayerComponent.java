package org.agmas.noellesroles.phonism;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtShort;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.Arrays;
import java.util.UUID;

/**
 * 谎言音效师组件：每个音效独立冷却（按 PhonismSound ordinal 索引），
 * 外加声音收集器的安装/监听状态（见 SoundCollectorHelper）。
 * 同步仿 AbilityPlayerComponent 精简写法，只发给本人。
 */
public class PhonismPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<PhonismPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "phonism"), PhonismPlayerComponent.class);

    private final PlayerEntity player;
    private final int[] soundCooldowns = new int[PhonismSound.values().length];
    /** 收集器当前安装到的玩家（安装后目标死亡仍保留，等待空手右键尸体回收） */
    @Nullable
    private UUID collectorTarget;
    /** 监听中继是否开启（开启时目标听到的声音转发给音效师本人） */
    private boolean relayActive;

    public PhonismPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        Arrays.fill(this.soundCooldowns, 0);
        if (this.collectorTarget != null) {
            SoundCollectorHelper.uninstall(this.collectorTarget);
        }
        this.collectorTarget = null;
        this.relayActive = false;
        this.sync();
    }

    // ==== 每音效独立冷却 ====

    public int getSoundCooldown(PhonismSound sound) {
        return this.soundCooldowns[sound.ordinal()];
    }

    public void setSoundCooldown(PhonismSound sound, int ticks) {
        this.soundCooldowns[sound.ordinal()] = Math.max(0, ticks);
        this.sync();
    }

    public void setAllSoundCooldowns(int ticks) {
        Arrays.fill(this.soundCooldowns, Math.max(0, ticks));
        this.sync();
    }

    /** 所有音效是否都在冷却中（HUD 显示最短剩余时间用）。 */
    public boolean isAllCooling() {
        for (int cooldown : this.soundCooldowns) {
            if (cooldown <= 0) return false;
        }
        return true;
    }

    /** 全部音效里最短的剩余冷却（用于 HUD「全部冷却中」时提示下一个解禁时间）。 */
    public int getMinCooldown() {
        int min = Integer.MAX_VALUE;
        for (int cooldown : this.soundCooldowns) {
            min = Math.min(min, cooldown);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    // ==== 收集器状态 ====

    @Nullable
    public UUID getCollectorTarget() {
        return this.collectorTarget;
    }

    public boolean isRelayActive() {
        return this.relayActive;
    }

    /** 安装收集器到目标（仅记状态；中继开关走 setRelayActive）。 */
    public void installCollector(@Nullable UUID target) {
        this.collectorTarget = target;
        this.sync();
    }

    /** 回收：清空安装与中继，并注销静态表登记。 */
    public void recoverCollector() {
        if (this.collectorTarget != null) {
            SoundCollectorHelper.uninstall(this.collectorTarget);
        }
        this.collectorTarget = null;
        this.relayActive = false;
        this.sync();
    }

    /** 惰性校验失败时翻停中继标记（表项由调用方摘除），collectorTarget 保留等待回收。 */
    public void stopRelayIfTargets(UUID target) {
        if (this.relayActive && target.equals(this.collectorTarget)) {
            this.relayActive = false;
            this.sync();
        }
    }

    public void setRelayActive(boolean active) {
        this.relayActive = active;
        if (this.collectorTarget != null) {
            if (active && this.player instanceof ServerPlayerEntity serverPlayer) {
                SoundCollectorHelper.registerTap(this.collectorTarget, serverPlayer);
            } else {
                SoundCollectorHelper.unregisterTap(this.collectorTarget, this.player);
            }
        }
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        boolean dirty = false;
        for (int i = 0; i < this.soundCooldowns.length; i++) {
            if (this.soundCooldowns[i] > 0) {
                this.soundCooldowns[i]--;
                if (this.soundCooldowns[i] == 0 || this.soundCooldowns[i] % 20 == 0) {
                    dirty = true;
                }
            }
        }

        // 中继失效即摘除：音效师死亡/掉线（本组件不再 tick 时由查询入口惰性校验）、
        // 被吞噬、目标死亡/掉线、游戏结束。collectorTarget 保留，等待回收。
        if (this.relayActive && this.collectorTarget != null
                && !SoundCollectorHelper.isTapValid(this.player, this.collectorTarget)) {
            this.relayActive = false;
            SoundCollectorHelper.unregisterTap(this.collectorTarget, this.player);
            dirty = true;
        }

        if (dirty) {
            this.sync();
        }
    }

    @Override
    public void clientTick() {
        // 本地递减，让面板按钮的 CD 秒数显示平滑（服务端每 20 tick 或有值归零时校准）
        for (int i = 0; i < this.soundCooldowns.length; i++) {
            if (this.soundCooldowns[i] > 0) {
                this.soundCooldowns[i]--;
            }
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        for (int cooldown : this.soundCooldowns) {
            buf.writeInt(cooldown);
        }
        buf.writeBoolean(this.relayActive);
        buf.writeBoolean(this.collectorTarget != null);
        if (this.collectorTarget != null) {
            buf.writeUuid(this.collectorTarget);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        for (int i = 0; i < this.soundCooldowns.length; i++) {
            this.soundCooldowns[i] = buf.readInt();
        }
        this.relayActive = buf.readBoolean();
        this.collectorTarget = buf.readBoolean() ? buf.readUuid() : null;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (int cooldown : this.soundCooldowns) {
            list.add(NbtShort.of((short) Math.min(cooldown, Short.MAX_VALUE)));
        }
        tag.put("soundCooldowns", list);
        tag.putBoolean("relayActive", this.relayActive);
        if (this.collectorTarget != null) {
            tag.putUuid("collectorTarget", this.collectorTarget);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        Arrays.fill(this.soundCooldowns, 0);
        if (tag.contains("soundCooldowns")) {
            NbtList list = tag.getList("soundCooldowns", net.minecraft.nbt.NbtElement.SHORT_TYPE);
            for (int i = 0; i < Math.min(list.size(), this.soundCooldowns.length); i++) {
                this.soundCooldowns[i] = list.getShort(i);
            }
        }
        this.relayActive = tag.getBoolean("relayActive");
        this.collectorTarget = tag.containsUuid("collectorTarget") ? tag.getUuid("collectorTarget") : null;
    }
}

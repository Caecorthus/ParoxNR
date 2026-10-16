package org.agmas.noellesroles.dreameater;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.task.DreamSleepTask;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 噬梦者技能对被命中玩家的状态。
 * 命中后延迟10秒生效：1~2 次 -> 提示「困意袭来」+ 注入睡觉任务 + 理智下降速度固定+1级；
 * 第3次及以上 -> 提示「深深疲倦」，完成下一次睡觉任务后无视护盾死亡。
 * hasSleepTask 为派生字段，仅同步给噬梦者客户端，用于穿墙透视。
 */
public class DreamEaterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DreamEaterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "dream_eater"), DreamEaterPlayerComponent.class);

    private static final int MARK_DELAY_TICKS = 200; // 10秒
    private static final int DEATH_HIT_COUNT = 3;

    private final PlayerEntity player;
    private int hitCount = 0;
    private int pendingTicks = 0;
    private boolean sleepDebuff = false;
    private boolean sleepAwaiting = false;
    private boolean deathPending = false;
    @Nullable private UUID lastDreamEaterUuid = null;
    private boolean hasSleepTask = false;

    public DreamEaterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /** 施加一次噬梦者技能命中（技能直接命中或食用安眠药食物）。 */
    public void applySkillHit(@Nullable UUID applierUuid) {
        this.hitCount++;
        this.pendingTicks = MARK_DELAY_TICKS;
        this.lastDreamEaterUuid = applierUuid;
    }

    public void reset() {
        this.hitCount = 0;
        this.pendingTicks = 0;
        this.sleepDebuff = false;
        this.sleepAwaiting = false;
        this.deathPending = false;
        this.lastDreamEaterUuid = null;
        this.hasSleepTask = false;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public boolean hasSleepTask() {
        return this.hasSleepTask;
    }

    public boolean isSleepAwaiting() {
        return this.sleepAwaiting;
    }

    @Nullable
    public UUID getLastDreamEaterUuid() {
        return this.lastDreamEaterUuid;
    }

    /** 睡觉任务完成：解除理智加速；返回三击命中累积的死亡结算是否待执行。 */
    public boolean consumeSleepCompletion() {
        this.sleepDebuff = false;
        this.sleepAwaiting = false;
        boolean die = this.deathPending;
        this.deathPending = false;
        return die;
    }

    @Override
    public void serverTick() {
        if (!GameFunctions.isPlayerPlayingAndAlive(this.player)) return;

        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(this.player);
        // 「持有任一睡觉任务」：原版睡觉任务（Task.SLEEP）与噬梦者注入的睡觉任务（DREAM_SLEEP 槽位）都算，
        // 噬梦者的梦境窥视（穿墙高亮）按这个口径点亮。
        boolean sleepTaskNow = isHoldingAnySleepTask(mood);
        if (sleepTaskNow != this.hasSleepTask) {
            this.hasSleepTask = sleepTaskNow;
            sync(); // 仅同步给噬梦者客户端（穿墙透视）
        }

        if (this.pendingTicks > 0) {
            this.pendingTicks--;
            if (this.pendingTicks == 0) {
                resolveMark(mood);
            }
        }

        // 理智下降速度固定+1级（等价任务数+1，完成睡觉任务后解除）；对假心情角色 setMood 无实际效果
        if (this.sleepDebuff && !mood.tasks.isEmpty()) {
            mood.setMood(mood.getMood() - GameConstants.MOOD_DRAIN);
        }
    }

    private void resolveMark(PlayerMoodComponent mood) {
        if (this.hitCount >= DEATH_HIT_COUNT) {
            this.deathPending = true;
            sendTip("tip.dream_eater.exhausted");
        } else {
            sendTip("tip.dream_eater.drowsy");
        }
        this.sleepDebuff = true;
        this.sleepAwaiting = true;
        // 噬梦者睡觉任务占用本模组追加的独立枚举槽位（NoellesTaskTypes.DREAM_SLEEP），
        // 因此不再需要「SLEEP 槽位被原版任务占着就不注入」的封锁，也不会覆盖/顶掉原版睡觉任务：
        // 两者可以同时挂在同一个玩家身上，HUD 各占一行。同一槽位重复命中则刷新时长。
        mood.tasks.put(NoellesTaskTypes.DREAM_SLEEP.slot(),
                new DreamSleepTask(GameConstants.SLEEP_TASK_DURATION));
        mood.sync();
    }

    /** 玩家当前是否持有任一睡觉类任务（原版睡觉任务或噬梦者睡觉任务）。 */
    private static boolean isHoldingAnySleepTask(PlayerMoodComponent mood) {
        for (PlayerMoodComponent.TrainTask task : mood.tasks.values()) {
            if (NoellesTaskTypes.isSleepLike(task)) {
                return true;
            }
        }
        return false;
    }

    private void sendTip(String key) {
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable(key), true);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.DREAM_EATER);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.hasSleepTask);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hasSleepTask = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("hitCount", this.hitCount);
        tag.putInt("pendingTicks", this.pendingTicks);
        tag.putBoolean("sleepDebuff", this.sleepDebuff);
        tag.putBoolean("sleepAwaiting", this.sleepAwaiting);
        tag.putBoolean("deathPending", this.deathPending);
        if (this.lastDreamEaterUuid != null) tag.putUuid("lastDreamEater", this.lastDreamEaterUuid);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hitCount = tag.getInt("hitCount");
        this.pendingTicks = tag.getInt("pendingTicks");
        this.sleepDebuff = tag.getBoolean("sleepDebuff");
        this.sleepAwaiting = tag.getBoolean("sleepAwaiting");
        this.deathPending = tag.getBoolean("deathPending");
        this.lastDreamEaterUuid = tag.containsUuid("lastDreamEater") ? tag.getUuid("lastDreamEater") : null;
    }
}

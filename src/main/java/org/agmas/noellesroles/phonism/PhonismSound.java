package org.agmas.noellesroles.phonism;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.agmas.noellesroles.ModSounds;
import org.jetbrains.annotations.Nullable;

/**
 * 谎言音效师可选择的伪造音效。
 * 音量与音调取各音效在真实使用时（开枪、撬门、爆炸等）的数值，保证听起来与真的完全一致。
 * 每个条目带独立冷却（cooldownTicks，按枚举 ordinal 存入 PhonismPlayerComponent）；
 * 组合条目（刀/雷/炸弹）用 playSequence 跟随目标播放多段序列。
 */
public enum PhonismSound {
    REVOLVER("revolver", WatheSounds.ITEM_REVOLVER_SHOOT, SoundCategory.PLAYERS, 5.0F, 1.0F, GameConstants.getInTicks(0, 30)),
    SHOTGUN("shotgun", SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.6F, 1.7F, GameConstants.getInTicks(0, 30)),
    // 匕首出刀与刺杀：出刀声 10 tick 后接刺杀声，与真实持刀杀人的节奏一致
    KNIFE_COMBO("knife_combo", WatheSounds.ITEM_KNIFE_PREPARE, SoundCategory.PLAYERS, 1.0F, 1.0F, GameConstants.getInTicks(0, 20),
            WatheSounds.ITEM_KNIFE_STAB, SoundCategory.PLAYERS, 1.0F, 1.0F, 10),
    PLAYER_HURT("player_hurt", SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F, 10), // 特殊：仅 10 tick（0.5 秒），可连点
    BAT_HIT("bat_hit", WatheSounds.ITEM_BAT_HIT, SoundCategory.PLAYERS, 3.0F, 1.0F, GameConstants.getInTicks(0, 20)),
    AXE_THROW("axe_throw", SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.PLAYERS, 1.0F, 1.0F, GameConstants.getInTicks(0, 15)),
    // 手雷拉环与爆炸：拉环声 20 tick 后接爆炸声
    GRENADE_COMBO("grenade_combo", WatheSounds.ITEM_GRENADE_THROW, SoundCategory.NEUTRAL, 0.5F, 1.0F, GameConstants.getInTicks(0, 45),
            WatheSounds.ITEM_GRENADE_EXPLODE, SoundCategory.PLAYERS, 5.0F, 1.0F, 20),
    BURP("burp", SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.2F, GameConstants.getInTicks(0, 5)),
    IRON_MAN("iron_man", WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.PLAYERS, 1.0F, 1.0F, GameConstants.getInTicks(0, 30)),
    LOCKPICK("lockpick", WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1.0F, 1.0F, GameConstants.getInTicks(0, 10)),
    CROWBAR("crowbar", WatheSounds.ITEM_CROWBAR_PRY, SoundCategory.BLOCKS, 2.5F, 1.0F, GameConstants.getInTicks(0, 10)),
    // 参数与病原体感染后打喷嚏的那声一致，听起来就是同一个症状
    SNEEZE("sneeze", SoundEvents.ENTITY_PANDA_SNEEZE, SoundCategory.PLAYERS, 2.0F, 0.8F, GameConstants.getInTicks(0, 10)),
    // 疯魔模式BGM：原版是 107 秒的全局背景音（AMBIENT 类、音量 1.0），这里取同样参数按位置播 12 秒
    PSYCHO_BGM("psycho_bgm", ModSounds.PSYCHO_DRONE, SoundCategory.AMBIENT, 1.0F, 1.0F, GameConstants.getInTicks(1, 0), 240),
    // 定时炸弹：完全镜像真实炸弹的滴滴+爆炸序列（见 playSequence 的 BOMB_COMBO 分支）
    BOMB_COMBO("bomb_combo", ModSounds.BOMB_BEEP, SoundCategory.PLAYERS, 2.0F, 1.0F, GameConstants.getInTicks(1, 0)),
    // 摆渡人引渡尸体：与真实引渡同音（Noellesroles.java 引渡处的播放参数）
    FERRYMAN_FERRY("ferryman_ferry", SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.9F, 0.75F, GameConstants.getInTicks(0, 15));

    /** 真实炸弹：滴滴声每 6 tick 一次（BomberPlayerComponent.BEEP_INTERVAL_TICKS）。 */
    private static final int BOMB_BEEP_INTERVAL_TICKS = 6;
    /** 真实炸弹：滴滴声持续 15 秒后爆炸（BomberPlayerComponent.BEEP_DURATION_TICKS）。 */
    private static final int BOMB_TOTAL_TICKS = GameConstants.getInTicks(0, 15);

    private final String key;
    private final SoundEvent event;
    private final SoundCategory category;
    private final float volume;
    private final float pitch;
    /** 该音效独立冷却（tick） */
    private final int cooldownTicks;
    /** 大于 0 时播放这么多 tick 后就停掉（音效本身比这长得多时用） */
    private final int stopAfterTicks;
    /** 组合音效的延迟后继音（无后继音时为 null） */
    @Nullable
    private final SoundEvent followEvent;
    @Nullable
    private final SoundCategory followCategory;
    private final float followVolume;
    private final float followPitch;
    private final int followDelayTicks;

    PhonismSound(String key, SoundEvent event, SoundCategory category, float volume, float pitch, int cooldownTicks) {
        this(key, event, category, volume, pitch, cooldownTicks, 0);
    }

    PhonismSound(String key, SoundEvent event, SoundCategory category, float volume, float pitch, int cooldownTicks, int stopAfterTicks) {
        this(key, event, category, volume, pitch, cooldownTicks, stopAfterTicks, null, null, 0.0F, 0.0F, 0);
    }

    PhonismSound(String key, SoundEvent event, SoundCategory category, float volume, float pitch, int cooldownTicks,
                 SoundEvent followEvent, SoundCategory followCategory, float followVolume, float followPitch, int followDelayTicks) {
        this(key, event, category, volume, pitch, cooldownTicks, 0,
                followEvent, followCategory, followVolume, followPitch, followDelayTicks);
    }

    PhonismSound(String key, SoundEvent event, SoundCategory category, float volume, float pitch, int cooldownTicks, int stopAfterTicks,
                 @Nullable SoundEvent followEvent, @Nullable SoundCategory followCategory, float followVolume, float followPitch, int followDelayTicks) {
        this.key = key;
        this.event = event;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
        this.cooldownTicks = cooldownTicks;
        this.stopAfterTicks = stopAfterTicks;
        this.followEvent = followEvent;
        this.followCategory = followCategory;
        this.followVolume = followVolume;
        this.followPitch = followPitch;
        this.followDelayTicks = followDelayTicks;
    }

    public String key() {
        return this.key;
    }

    public SoundEvent event() {
        return this.event;
    }

    public SoundCategory category() {
        return this.category;
    }

    public float volume() {
        return this.volume;
    }

    public float pitch() {
        return this.pitch;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    /**
     * 在指定位置播放，附近所有玩家都能听到。
     * 带时长限制的音效（疯魔 BGM 整段 107 秒）到点后向该世界所有玩家发停止包；
     * 因为音效挂在本模组自己的 id 下，停止不会误伤真正的同名音效。
     */
    public void playAt(ServerWorld world, double x, double y, double z) {
        world.playSound(null, x, y, z, this.event, this.category, this.volume, this.pitch);

        if (this.stopAfterTicks <= 0) {
            return;
        }
        StopSoundS2CPacket stopPacket = new StopSoundS2CPacket(this.event.getId(), this.category);
        Scheduler.schedule(() -> {
            for (ServerPlayerEntity listener : world.getPlayers()) {
                listener.networkHandler.sendPacket(stopPacket);
            }
        }, this.stopAfterTicks);
    }

    /**
     * 以目标玩家为中心播放：首音在目标当前位置，组合音效的后继音跟随目标、
     * 到点时取目标当时坐标（目标中途死亡/移除则在最后已知坐标播放）。
     * 炸弹序列纯声音伪造：不生成粒子、不造成伤害。
     */
    public void playSequence(ServerWorld world, PlayerEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        if (this == BOMB_COMBO) {
            stepBomb(world, target, x, y, z, BOMB_TOTAL_TICKS);
            return;
        }

        playAt(world, x, y, z);
        if (this.followEvent != null) {
            stepFollow(world, target, x, y, z, this.followDelayTicks);
        }
    }

    /** 逐 tick 跟随目标记录坐标，倒数到 0 时在最后已知坐标播放后继音。 */
    private void stepFollow(ServerWorld world, PlayerEntity target, double x, double y, double z, int ticksRemaining) {
        if (isTrackable(world, target)) {
            x = target.getX();
            y = target.getY();
            z = target.getZ();
        }
        if (ticksRemaining <= 0) {
            world.playSound(null, x, y, z, this.followEvent, this.followCategory, this.followVolume, this.followPitch);
            return;
        }
        final double fx = x;
        final double fy = y;
        final double fz = z;
        Scheduler.schedule(() -> stepFollow(world, target, fx, fy, fz, ticksRemaining - 1), 1);
    }

    /**
     * 定时炸弹序列：每 6 tick 一次滴滴声、共 15 秒、跟随目标；
     * 目标中途死亡/移除则剩余滴滴与爆炸留在最后已知坐标完成（“炸弹留在尸体上”）。
     */
    private void stepBomb(ServerWorld world, PlayerEntity target, double x, double y, double z, int ticksRemaining) {
        if (isTrackable(world, target)) {
            x = target.getX();
            y = target.getY();
            z = target.getZ();
        }
        if (ticksRemaining <= 0) {
            world.playSound(null, x, y, z, ModSounds.BOMB_EXPLODE, SoundCategory.PLAYERS, 3.0F, 1.0F);
            return;
        }
        if (ticksRemaining % BOMB_BEEP_INTERVAL_TICKS == 0) {
            world.playSound(null, x, y, z, ModSounds.BOMB_BEEP, SoundCategory.PLAYERS, 2.0F, 1.0F);
        }
        final double fx = x;
        final double fy = y;
        final double fz = z;
        Scheduler.schedule(() -> stepBomb(world, target, fx, fy, fz, ticksRemaining - 1), 1);
    }

    private static boolean isTrackable(ServerWorld world, PlayerEntity target) {
        return !target.isRemoved() && target.getWorld() == world;
    }

    @Nullable
    public static PhonismSound byKey(String key) {
        for (PhonismSound sound : values()) {
            if (sound.key.equals(key)) {
                return sound;
            }
        }
        return null;
    }
}

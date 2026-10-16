package org.agmas.noellesroles.phonism;

import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.Scheduler;
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
 */
public enum PhonismSound {
    REVOLVER("revolver", WatheSounds.ITEM_REVOLVER_SHOOT, SoundCategory.PLAYERS, 5.0F, 1.0F),
    SHOTGUN("shotgun", SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.6F, 1.7F),
    KNIFE_PREPARE("knife_prepare", WatheSounds.ITEM_KNIFE_PREPARE, SoundCategory.PLAYERS, 1.0F, 1.0F),
    PLAYER_HURT("player_hurt", SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F),
    KNIFE_STAB("knife_stab", WatheSounds.ITEM_KNIFE_STAB, SoundCategory.PLAYERS, 1.0F, 1.0F),
    BAT_HIT("bat_hit", WatheSounds.ITEM_BAT_HIT, SoundCategory.PLAYERS, 3.0F, 1.0F),
    AXE_THROW("axe_throw", SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.PLAYERS, 1.0F, 1.0F),
    GRENADE_THROW("grenade_throw", WatheSounds.ITEM_GRENADE_THROW, SoundCategory.NEUTRAL, 0.5F, 1.0F),
    GRENADE_EXPLODE("grenade_explode", WatheSounds.ITEM_GRENADE_EXPLODE, SoundCategory.PLAYERS, 5.0F, 1.0F),
    BURP("burp", SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.2F),
    IRON_MAN("iron_man", WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.PLAYERS, 1.0F, 1.0F),
    LOCKPICK("lockpick", WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1.0F, 1.0F),
    CROWBAR("crowbar", WatheSounds.ITEM_CROWBAR_PRY, SoundCategory.BLOCKS, 2.5F, 1.0F),
    // 参数与病原体感染后打喷嚏的那声一致，听起来就是同一个症状
    SNEEZE("sneeze", SoundEvents.ENTITY_PANDA_SNEEZE, SoundCategory.PLAYERS, 2.0F, 0.8F),
    // 疯魔模式BGM：原版是 107 秒的全局背景音（AMBIENT 类、音量 1.0），这里取同样参数按位置播 5 秒
    PSYCHO_BGM("psycho_bgm", ModSounds.PSYCHO_DRONE, SoundCategory.AMBIENT, 1.0F, 1.0F, 100);

    private final String key;
    private final SoundEvent event;
    private final SoundCategory category;
    private final float volume;
    private final float pitch;
    /** 大于 0 时播放这么多 tick 后就停掉（音效本身比这长得多时用） */
    private final int stopAfterTicks;

    PhonismSound(String key, SoundEvent event, SoundCategory category, float volume, float pitch) {
        this(key, event, category, volume, pitch, 0);
    }

    PhonismSound(String key, SoundEvent event, SoundCategory category, float volume, float pitch, int stopAfterTicks) {
        this.key = key;
        this.event = event;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
        this.stopAfterTicks = stopAfterTicks;
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

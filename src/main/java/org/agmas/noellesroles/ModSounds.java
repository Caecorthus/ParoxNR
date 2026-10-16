package org.agmas.noellesroles;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent BOMB_BEEP = registerSound("item.bomb.beep");
    public static final SoundEvent BOMB_EXPLODE = registerSound("item.bomb.explode");
    public static final SoundEvent JESTER_LAUGH = registerSound("ambient.jester_laugh");
    public static final SoundEvent SAINT_BELL = registerSound("ambient.saint_bell");
    // 黑警时刻相关音效
    public static final SoundEvent CORRUPT_COP_MOMENT_1 = registerSound("music.corrupt_cop_moment_1");
    public static final SoundEvent CORRUPT_COP_MOMENT_2 = registerSound("music.corrupt_cop_moment_2");
    public static final SoundEvent CORRUPT_COP_EXECUTION = registerSound("ambient.corrupt_cop_execution");
    // 小丑时刻BGM
    public static final SoundEvent JESTER_MOMENT = registerSound("music.jester_moment");
    // 谎言音效师伪造的疯魔模式BGM：音频复用 wathe 的 psycho_drone，但挂在本模组的音效 id 下，
    // 这样按 id 发送停止包时不会误停真正的疯魔背景音
    public static final SoundEvent PSYCHO_DRONE = registerSound("ambient.psycho_drone");

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of(Noellesroles.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
        // 静态初始化时自动注册
    }
}

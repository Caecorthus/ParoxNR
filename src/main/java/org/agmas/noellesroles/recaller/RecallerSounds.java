package org.agmas.noellesroles.recaller;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/**
 * 回溯者音效工具：放置/回收/传送音效只发给操作者本人。
 * 定案：锚点对非持有者隐形，声音不得泄露位置；传送保持旧技能的静默风格，旁观者无提示。
 * 先例：BreacherWorldComponent#playAmbientBlackoutSound 的 PlaySoundS2CPacket 用法。
 */
public final class RecallerSounds {
    private RecallerSounds() {
    }

    public static void playToSelf(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(sound),
                SoundCategory.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch, player.getRandom().nextLong()));
    }
}

package org.agmas.noellesroles.mixin.phonism;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.phonism.SoundCollectorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 声音收集器：目标"自我音效"中继（脚步声等）。
 * 玩家自己的脚步/拍水等音效经 PlayerEntity#playSound 广播时服务端会排除本人
 * （本人靠客户端本地预测听到），所以这些声音永远不产生发给目标的包、
 * 世界音效中继（sendPacket hook）收不到——正是"听不到目标脚步声"的根因。
 * 这里在音效师监听期间把目标的自我音效单独复制一份 PlaySoundS2CPacket
 * 发给音效师（音效/音量/音调原样、坐标为音效师位置，仅其本人可闻），
 * 与目标本地预测听到的自己脚步完全一致。
 */
@Mixin(PlayerEntity.class)
public class SoundCollectorSelfSoundMixin {

    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"))
    private void noellesroles$relayWalkerSelfSound(SoundEvent event, float volume, float pitch, CallbackInfo ci) {
        if (!SoundCollectorHelper.hasActiveTaps()) {
            return;
        }
        PlayerEntity walker = (PlayerEntity) (Object) this;
        if (walker.getWorld().isClient) {
            return;
        }
        ServerPlayerEntity phonism = SoundCollectorHelper.getPhonismForTarget(walker.getUuid());
        if (phonism == null) {
            return;
        }
        phonism.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(event), walker.getSoundCategory(),
                phonism.getX(), phonism.getY(), phonism.getZ(),
                volume, pitch, walker.getRandom().nextLong()));
    }
}

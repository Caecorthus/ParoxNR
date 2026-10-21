package org.agmas.noellesroles.client.mixin.silencer;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.agmas.noellesroles.client.silencer.SilenceZoneClientGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 寂静装置无声区域：本地玩家位于区域内时拦截所有声音播放
 * （SoundManager.play 是所有游戏声音进入客户端播放管线的唯一收口）。
 */
@Mixin(SoundManager.class)
public abstract class SilenceZoneSoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$silenceZonePlay(SoundInstance sound, CallbackInfo ci) {
        if (SilenceZoneClientGate.shouldSuppressAllSounds()) {
            ci.cancel();
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$silenceZonePlayDelayed(SoundInstance sound, int delayTicks, CallbackInfo ci) {
        if (SilenceZoneClientGate.shouldSuppressAllSounds()) {
            ci.cancel();
        }
    }

    @Inject(method = "playNextTick(Lnet/minecraft/client/sound/TickableSoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$silenceZonePlayNextTick(net.minecraft.client.sound.TickableSoundInstance sound, CallbackInfo ci) {
        if (SilenceZoneClientGate.shouldSuppressAllSounds()) {
            ci.cancel();
        }
    }
}

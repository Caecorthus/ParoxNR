package org.agmas.noellesroles.client.mixin.taotie;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.agmas.noellesroles.client.util.TaotieIndicatorClientHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 罗盘开启时 action bar 由罗盘独占：屏蔽服务端每秒广播的饕餮时刻倒计时。
 * 注意 sendMessage(text, true) 走的是 GameMessageS2CPacket（overlay 标志），而非 OverlayMessageS2CPacket。
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class TaotieMomentCountdownMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void noellesroles$suppressTaotieMomentCountdown(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.overlay() && TaotieIndicatorClientHelper.shouldSuppressMomentCountdown(packet.content())) {
            ci.cancel();
        }
    }
}

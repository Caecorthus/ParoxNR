package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.util.WalkieTalkieChannelPayload;
import org.agmas.noellesroles.walkie.WalkieTalkieChannels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 对讲机频道上限由 Wathe 的 0~99 收窄为 0~33
 */
@Mixin(value = WalkieTalkieChannelPayload.Receiver.class, remap = false)
public class WalkieTalkieChannelPayloadReceiverMixin {

    @ModifyArg(
            method = "receive",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"),
            index = 0
    )
    private int noellesroles$limitChannel(int originalMax) {
        return WalkieTalkieChannels.MAX_CHANNEL;
    }
}

package org.agmas.noellesroles.client.mixin.walkie;

import dev.doctor4t.wathe.client.gui.screen.WalkieTalkieScreen;
import org.agmas.noellesroles.walkie.WalkieTalkieChannels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 对讲机调频界面的频道上限由 Wathe 的 0~99 收窄为 0~33
 * （输入框、增减按钮、滚轮三条路径）
 */
@Mixin(WalkieTalkieScreen.class)
public class WalkieTalkieScreenMixin {

    @ModifyArg(
            method = "onChannelInputChanged",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"),
            index = 2
    )
    private int noellesroles$limitTypedChannel(int originalMax) {
        return WalkieTalkieChannels.MAX_CHANNEL;
    }

    @ModifyArg(
            method = "adjustChannel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"),
            index = 2
    )
    private int noellesroles$limitAdjustedChannel(int originalMax) {
        return WalkieTalkieChannels.MAX_CHANNEL;
    }
}

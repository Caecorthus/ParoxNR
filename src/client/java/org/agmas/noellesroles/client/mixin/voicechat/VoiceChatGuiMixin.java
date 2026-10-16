package org.agmas.noellesroles.client.mixin.voicechat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.agmas.noellesroles.client.gui.SpectatorReplayToastOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.RenderEvents")
public abstract class VoiceChatGuiMixin {
    @Inject(method = "onRenderHUD(Lnet/minecraft/class_332;F)V", at = @At("TAIL"), require = 0)
    private void noellesroles$renderAboveVoiceChatHud(DrawContext context, float delta, CallbackInfo ci) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (textRenderer == null) {
            return;
        }
        SpectatorReplayToastOverlay.markVoiceChatRendered();
        SpectatorReplayToastOverlay.render(context, textRenderer);
    }
}

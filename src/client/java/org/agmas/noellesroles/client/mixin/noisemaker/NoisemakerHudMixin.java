package org.agmas.noellesroles.client.mixin.noisemaker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class NoisemakerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void noisemakerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.NOISEMAKER)) {
            int drawY = context.getScaledWindowHeight();

            Text line = Text.translatable("tip.noisemaker", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());

            // 检查是否正在广播
            NoisemakerPlayerComponent noisemakerComp = NoisemakerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
            if (noisemakerComp.isBroadcasting()) {
                // 广播中，显示剩余时间
                line = Text.translatable("tip.noisemaker.active", noisemakerComp.getBroadcastTicksRemaining() / 20);
            } else if (abilityPlayerComponent.cooldown > 0) {
                // 冷却中
                line = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.cooldown / 20);
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, Noellesroles.NOISEMAKER.color());
        }
    }
}

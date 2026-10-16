package org.agmas.noellesroles.client.mixin.taotie;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.gui.HallucinationHudRenderer;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.rolehud.TaotieHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(InGameHud.class)
public abstract class TaotieHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void taotieHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HallucinationHudRenderer.shouldSuppressSkillHintHud()) return;

        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorldComponent.isRole(player, Noellesroles.TAOTIE)) return;

        List<TaotieHudRenderer.HudLine> lines = TaotieHudRenderer.getLines(player);
        if (lines.isEmpty()) return;

        TextRenderer renderer = getTextRenderer();
        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        try {
            int drawY = context.getScaledWindowHeight();
            for (int i = 0; i < lines.size(); i++) {
                TaotieHudRenderer.HudLine line = lines.get(i);
                drawY -= renderer.getWrappedLinesHeight(line.text(), 999999);
                HudRenderHelper.drawRightAligned(context, renderer, line.text(), 0, drawY, line.color());
                if (i < lines.size() - 1) {
                    drawY -= HudRenderHelper.LINE_GAP;
                }
            }
        } finally {
            HudRenderHelper.popAboveVoiceChatHudLayer(context);
        }
    }
}

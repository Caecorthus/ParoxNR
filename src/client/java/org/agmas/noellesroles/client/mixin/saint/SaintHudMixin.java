package org.agmas.noellesroles.client.mixin.saint;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.SaintHudHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class SaintHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderSaintHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        Text line = SaintHudHelper.getHudLine(player);
        if (line == null) return;
        int lineHeight = getTextRenderer().getWrappedLinesHeight(line, 999999);
        int skillHudTopY = HudRenderHelper.getBottomRightSkillHudTopY(context, getTextRenderer(), player);
        int drawY = skillHudTopY < context.getScaledWindowHeight()
                ? skillHudTopY - lineHeight - 2
                : context.getScaledWindowHeight() - lineHeight;
        HudRenderHelper.drawRightAligned(context, getTextRenderer(), line,
                HudRenderHelper.getBottomRightSkillHudRightPadding(player), drawY, Noellesroles.SAINT.color());
    }
}

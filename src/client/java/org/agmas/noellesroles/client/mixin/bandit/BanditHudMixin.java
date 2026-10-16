package org.agmas.noellesroles.client.mixin.bandit;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.rolehud.BanditHudRenderer;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class BanditHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderBanditHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.BANDIT)) return;
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return;

        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        try {
            int drawY = context.getScaledWindowHeight();
            for (Text line : BanditHudRenderer.getLines(player)) {
                drawY = HudRenderHelper.drawBottomRight(context, getTextRenderer(), line, drawY, Noellesroles.BANDIT.color());
            }
        } finally {
            HudRenderHelper.popAboveVoiceChatHudLayer(context);
        }
    }
}

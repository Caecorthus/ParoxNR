package org.agmas.noellesroles.client.mixin.bodyguard;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.rolehud.BodyguardHudRenderer;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class BodyguardHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderBodyguardHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.BODYGUARD)) return;
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return;

        Text line = BodyguardHudRenderer.getLine(player);
        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        try {
            HudRenderHelper.drawBottomRight(context, getTextRenderer(), line, context.getScaledWindowHeight(), Noellesroles.BODYGUARD.color());
        } finally {
            HudRenderHelper.popAboveVoiceChatHudLayer(context);
        }
    }
}

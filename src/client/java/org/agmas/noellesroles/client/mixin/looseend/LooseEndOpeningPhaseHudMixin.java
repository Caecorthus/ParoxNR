package org.agmas.noellesroles.client.mixin.looseend;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class LooseEndOpeningPhaseHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderLooseEndOpeningPhaseHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorldComponent.isRole(player, dev.doctor4t.wathe.api.WatheRoles.LOOSE_END)) {
            return;
        }

        LooseEndPlayerComponent looseEndComponent = LooseEndPlayerComponent.KEY.get(player);
        if (!looseEndComponent.isOpeningPhased()) {
            return;
        }

        Text line = Text.translatable("tip.noellesroles.loose_end_opening_phase", (looseEndComponent.getOpeningPhaseTicks() + 19) / 20);
        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        try {
            int drawY = context.getScaledWindowHeight() - 2 - getTextRenderer().getWrappedLinesHeight(line, 999999);
            HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, dev.doctor4t.wathe.api.WatheRoles.LOOSE_END.color());
        } finally {
            HudRenderHelper.popAboveVoiceChatHudLayer(context);
        }
    }
}

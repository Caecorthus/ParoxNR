package org.agmas.noellesroles.client.mixin.recaller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.rolehud.RecallerHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 回溯者右下角 HUD；提示行与 RecallerHudRenderer.getTopY 共用 buildHintLine，绘制位置/约定沿用旧 mixin。 */
@Mixin(InGameHud.class)
public abstract class RecallerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void recallerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity local = MinecraftClient.getInstance().player;
        if (local == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(local)) return;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(local.getWorld());
        if (!gameWorldComponent.isRole(local, Noellesroles.RECALLER)) return;

        Text line = RecallerHudRenderer.buildHintLine(local);
        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(line, 999999);
        HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, Noellesroles.RECALLER.color());
    }
}

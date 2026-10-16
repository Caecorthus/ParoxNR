package org.agmas.noellesroles.client.mixin.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.agmas.noellesroles.client.gui.TopRightInfoHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 右上角信息块：绘制在金币行下方的模式/地图/按键提示。
 */
@Mixin(InGameHud.class)
public abstract class TopRightInfoHudMixin {
    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderTopRightInfo(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        TopRightInfoHudRenderer.render(context, getTextRenderer());
    }
}

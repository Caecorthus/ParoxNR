package org.agmas.noellesroles.client.mixin.baosang;

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
import org.agmas.noellesroles.baosang.BaosangPlayerComponent;
import org.agmas.noellesroles.client.gui.HallucinationHudRenderer;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class BaosangHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void baosangHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (HallucinationHudRenderer.shouldSuppressSkillHintHud()) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(
                MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(
                MinecraftClient.getInstance().player);
        BaosangPlayerComponent baosangComp = BaosangPlayerComponent.KEY.get(
                MinecraftClient.getInstance().player);

        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;
        if (!gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.BAOSANG)) return;

        int drawY = context.getScaledWindowHeight();
        Text line;
        if (abilityComp.cooldown > 0) {
            line = Text.translatable("tip.noellesroles.cooldown", abilityComp.cooldown / 20);
        } else {
            line = Text.translatable("tip.baosang.ready");
        }

        drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
        HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, Noellesroles.BAOSANG.color());
    }
}

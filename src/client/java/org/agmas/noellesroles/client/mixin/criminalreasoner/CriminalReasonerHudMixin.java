package org.agmas.noellesroles.client.mixin.criminalreasoner;

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
import org.agmas.noellesroles.client.gui.HallucinationHudRenderer;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.rolehud.CriminalReasonerHudRenderer;
import org.agmas.noellesroles.criminalreasoner.CriminalReasonerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 犯罪推理学家右下角技能提示：与其余角色一致，自底向上堆叠绘制
 * （技能行在下，推理进度行在其上），与 CriminalReasonerHudRenderer 量取结果严格一致，
 * 避免与其他右下角叠加行（如任务点提示）错位重叠。
 */
@Mixin(InGameHud.class)
public abstract class CriminalReasonerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderCriminalReasonerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (HallucinationHudRenderer.shouldSuppressSkillHintHud()) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        if (!gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.CRIMINAL_REASONER)) return;

        CriminalReasonerPlayerComponent criminalReasonerComponent = CriminalReasonerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        Text cooldownText;
        if (abilityPlayerComponent.getCooldown() > 0) {
            cooldownText = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.getCooldown() / 20);
        } else {
            String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            cooldownText = Text.translatable("hud.criminal_reasoner.press_key_hint", keyName);
        }

        int requiredReasoningCount = criminalReasonerComponent.getRequiredReasoningCount();
        Text progressText = Text.translatable(
                "hud.criminal_reasoner.progress",
                criminalReasonerComponent.getSuccessfulReasoningCount(),
                requiredReasoningCount
        );
        Text invisibilityText = CriminalReasonerHudRenderer.getInvisibilityLine(
                MinecraftClient.getInstance().player, criminalReasonerComponent);

        TextRenderer renderer = getTextRenderer();
        int color = Noellesroles.CRIMINAL_REASONER.color();

        // 技能行贴底，隐身行与推理进度行依次向上（与 CriminalReasonerHudRenderer 的堆叠顺序一致）
        int drawY = context.getScaledWindowHeight() - renderer.getWrappedLinesHeight(cooldownText, 999999);
        HudRenderHelper.drawRightAligned(context, renderer, cooldownText, 0, drawY, color);
        drawY -= renderer.getWrappedLinesHeight(invisibilityText, 999999);
        HudRenderHelper.drawRightAligned(context, renderer, invisibilityText, 0, drawY, color);
        drawY -= renderer.getWrappedLinesHeight(progressText, 999999);
        HudRenderHelper.drawRightAligned(context, renderer, progressText, 0, drawY, color);
    }
}

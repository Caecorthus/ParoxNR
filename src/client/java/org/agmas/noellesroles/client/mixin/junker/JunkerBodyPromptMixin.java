package org.agmas.noellesroles.client.mixin.junker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.junker.JunkerLootHelper;
import org.agmas.noellesroles.junker.JunkerLootWorldComponent;
import org.agmas.noellesroles.junker.JunkerPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 拾荒者准星提示：视角对准可搜刮的尸体时，在准星下方显示「按 [技能键] 搜索尸体」。
 */
@Mixin(RoleNameRenderer.class)
public abstract class JunkerBodyPromptMixin {

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void noellesroles$renderJunkerSearchPrompt(TextRenderer renderer, ClientPlayerEntity player,
                                                              DrawContext context, RenderTickCounter tickCounter,
                                                              CallbackInfo ci) {
        if (NoellesrolesClient.targetBody == null || !WatheClient.isPlayerAliveAndInSurvival()) {
            return;
        }
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return;
        }
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorldComponent.isRole(player, Noellesroles.JUNKER)) {
            return;
        }
        if (AbilityPlayerComponent.KEY.get(player).getCooldown() > 0) {
            return;
        }
        if (JunkerPlayerComponent.KEY.get(player).isSearching()) {
            return;
        }
        if (JunkerLootHelper.hasBlockingLoot(player) || !JunkerLootHelper.hasEmptyHotbarSlot(player)) {
            return;
        }
        UUID deadPlayerUuid = NoellesrolesClient.targetBody.getPlayerUuid();
        if (deadPlayerUuid != null
                && JunkerLootWorldComponent.KEY.get(player.getWorld()).wasSearched(deadPlayerUuid)) {
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate((float) context.getScaledWindowWidth() / 2.0F,
                (float) context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        Text prompt = Text.translatable("hud.junker.search",
                NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()).withColor(Noellesroles.JUNKER.color());
        context.drawTextWithShadow(renderer, prompt, -renderer.getWidth(prompt) / 2, 32, Colors.WHITE);
        context.getMatrices().pop();
    }
}

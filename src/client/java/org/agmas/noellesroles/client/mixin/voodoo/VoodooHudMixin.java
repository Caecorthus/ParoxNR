package org.agmas.noellesroles.client.mixin.voodoo;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(InGameHud.class)
public abstract class VoodooHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderVoodooHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(client.player)) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        boolean isVoodoo = gameWorld.isRole(client.player, Noellesroles.VOODOO);
        boolean isJunker = gameWorld.isRole(client.player, Noellesroles.JUNKER);
        if (!isVoodoo && !isJunker) return;

        VoodooPlayerComponent voodoo = VoodooPlayerComponent.KEY.get(client.player);
        UUID targetUuid = voodoo.target;
        boolean hasBound = targetUuid != null && !targetUuid.equals(client.player.getUuid());
        // 拾荒者的娃娃是搜刮获得的额外道具：未绑定时不占用 HUD
        if (isJunker && !hasBound) return;

        // 拾荒者画在自身搜刮技能行上方；巫毒师仍按原样从屏幕底部画
        int drawY = isJunker
                ? HudRenderHelper.getBottomRightSkillHudTopY(context, getTextRenderer(), client.player)
                : context.getScaledWindowHeight();
        int color = (isJunker ? Noellesroles.JUNKER : Noellesroles.VOODOO).color();

        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        try {
            if (hasBound) {
                PlayerEntity target = client.player.getWorld().getPlayerByUuid(targetUuid);
                if (target != null) {
                    Text line = Text.translatable("tip.voodoo.bound", target.getName());
                    drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
                    HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, color);
                    drawY -= 2;
                }
            }

            if (isJunker) {
                ConfigWorldComponent config = ConfigWorldComponent.KEY.get(client.player.getWorld());
                if (!config.naturalVoodoosAllowed) {
                    Text warning = Text.translatable("tip.voodoo.natural_death_disabled");
                    drawY -= getTextRenderer().getWrappedLinesHeight(warning, 999999);
                    HudRenderHelper.drawRightAligned(context, getTextRenderer(), warning, 0, drawY, color);
                }
                return;
            }

            Text line = Text.translatable("tip.voodoo.ready");
            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, color);
        } finally {
            HudRenderHelper.popAboveVoiceChatHudLayer(context);
        }
    }
}

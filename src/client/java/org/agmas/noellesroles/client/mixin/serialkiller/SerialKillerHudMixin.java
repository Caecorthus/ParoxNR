package org.agmas.noellesroles.client.mixin.serialkiller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 连环杀手右下角技能提示：死亡倒计时行在底（技能冷却行下方），技能冷却/按键提示行在其上。
 */
@Mixin(InGameHud.class)
public abstract class SerialKillerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderSerialKillerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.SERIAL_KILLER)) return;

        SerialKillerPlayerComponent component = SerialKillerPlayerComponent.KEY.get(player);
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(player);
        int color = Noellesroles.SERIAL_KILLER.color();

        Text doomText = null;
        if (component.getDoomTicks() > 0) {
            doomText = Text.translatable("hud.serial_killer.doom", (component.getDoomTicks() + 19) / 20);
        }
        Text skillText = abilityComp.getCooldown() > 0
                ? Text.translatable("tip.noellesroles.cooldown", abilityComp.getCooldown() / 20)
                : Text.translatable("hud.serial_killer.activate", HudRenderHelper.getAbilityKeyName());

        // 右下角技能提示需要压在 Simple Voice Chat 头像层之上，否则会被组队头像遮住。
        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        try {
            int drawY = context.getScaledWindowHeight();
            if (doomText != null) {
                drawY = HudRenderHelper.drawBottomRight(context, getTextRenderer(), doomText, drawY, color);
            }
            HudRenderHelper.drawBottomRight(context, getTextRenderer(), skillText, drawY, color);
        } finally {
            HudRenderHelper.popAboveVoiceChatHudLayer(context);
        }
    }
}

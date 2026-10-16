package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.criminalreasoner.CriminalReasonerPlayerComponent;

public final class CriminalReasonerHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        CriminalReasonerPlayerComponent comp = CriminalReasonerPlayerComponent.KEY.get(player);
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(player);

        Text cooldownText = abilityComp.getCooldown() > 0
                ? Text.translatable("tip.noellesroles.cooldown", abilityComp.getCooldown() / 20)
                : Text.translatable("hud.criminal_reasoner.press_key_hint", HudRenderHelper.getAbilityKeyName());

        Text invisibilityText = getInvisibilityLine(player, comp);
        Text progressText = Text.translatable(
                "hud.criminal_reasoner.progress",
                comp.getSuccessfulReasoningCount(),
                comp.getRequiredReasoningCount()
        );

        int drawY = HudRenderHelper.stackLine(bottom, renderer, cooldownText, 0);
        drawY = HudRenderHelper.stackLine(drawY, renderer, invisibilityText, 0);
        return HudRenderHelper.stackLine(drawY, renderer, progressText, 0);
    }

    /** 未隐身时显示按键与充能数，隐身中显示剩余秒数（与幽灵角色一致） */
    public static Text getInvisibilityLine(ClientPlayerEntity player, CriminalReasonerPlayerComponent comp) {
        StatusEffectInstance invisEffect = player.getStatusEffect(StatusEffects.INVISIBILITY);
        if (invisEffect != null) {
            return Text.translatable("hud.criminal_reasoner.invisibility_active", invisEffect.getDuration() / 20);
        }
        return Text.translatable(
                "hud.criminal_reasoner.invisibility",
                HudRenderHelper.getAbility2KeyText(),
                comp.getInvisibilityCharges()
        );
    }
}

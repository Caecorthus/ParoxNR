package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

public final class DreamEaterHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;
        return HudRenderHelper.stackLine(bottom, renderer, getLine(player), 0);
    }

    public static Text getLine(ClientPlayerEntity player) {
        AbilityPlayerComponent comp = AbilityPlayerComponent.KEY.get(player);
        if (comp.getCooldown() > 0) {
            return Text.translatable("tip.noellesroles.cooldown", comp.getCooldown() / 20);
        }
        return Text.translatable("tip.dream_eater.ready", HudRenderHelper.getAbilityKeyName());
    }
}

package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

public final class BodyguardHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;
        return HudRenderHelper.stackLine(bottom, renderer, getLine(player), 0);
    }

    public static Text getLine(ClientPlayerEntity player) {
        BodyguardPlayerComponent bodyguard = BodyguardPlayerComponent.KEY.get(player);
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);

        if (bodyguard.isStanceActive()) {
            return Text.translatable("tip.bodyguard.active", HudRenderHelper.getAbilityKeyName());
        }
        if (ability.getCooldown() > 0) {
            return Text.translatable("tip.noellesroles.cooldown", ability.getCooldown() / 20);
        }
        if (!player.getMainHandStack().isEmpty()) {
            return Text.translatable("tip.bodyguard.need_empty_hand");
        }
        return Text.translatable("tip.bodyguard.ready", HudRenderHelper.getAbilityKeyName());
    }
}

package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.breacher.BreacherPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

public final class BreacherHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;
        return HudRenderHelper.stackLine(bottom, renderer, getLine(player), 0);
    }

    public static Text getLine(ClientPlayerEntity player) {
        BreacherPlayerComponent comp = BreacherPlayerComponent.KEY.get(player);
        if (comp.hasPendingPoint()) {
            return Text.translatable(
                    "tip.breacher.confirm",
                    HudRenderHelper.getAbilityKeyName(),
                    comp.getPendingPointTicksRemaining() / 20);
        }
        if (comp.getCooldownTicks() > 0) {
            return Text.translatable("tip.noellesroles.cooldown", comp.getCooldownTicks() / 20);
        }
        MutableText base = Text.translatable("tip.breacher.ready", HudRenderHelper.getAbilityKeyName());
        int count = comp.getOwnActivePointCount();
        if (count > 0) {
            return base.append(Text.translatable("tip.breacher.count", count));
        }
        return base;
    }
}

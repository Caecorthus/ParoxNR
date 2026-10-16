package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;

import java.util.UUID;

public final class VoodooHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        VoodooPlayerComponent voodoo = VoodooPlayerComponent.KEY.get(player);
        int drawY = bottom;

        Text line1 = Text.translatable("tip.voodoo.ready");
        drawY = HudRenderHelper.stackLine(drawY, renderer, line1, 0);

        UUID currentTarget = voodoo.target;
        if (currentTarget != null && !currentTarget.equals(player.getUuid())) {
            PlayerEntity target = player.getWorld().getPlayerByUuid(currentTarget);
            if (target != null) {
                drawY = HudRenderHelper.stackLine(drawY, renderer, Text.translatable("tip.voodoo.bound", target.getName()), 0);
            }
        }

        ConfigWorldComponent config = ConfigWorldComponent.KEY.get(player.getWorld());
        if (!config.naturalVoodoosAllowed) {
            drawY = HudRenderHelper.stackLine(drawY, renderer, Text.translatable("tip.voodoo.natural_death_disabled"), 0);
        }

        return drawY;
    }
}

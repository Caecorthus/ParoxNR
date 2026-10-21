package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.ferryman.FerrymanPlayerComponent;
import org.agmas.noellesroles.ferryman.FerrymanPreserveWorldComponent;

public final class FerrymanHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        FerrymanPlayerComponent ferryman = FerrymanPlayerComponent.KEY.get(player);
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);

        Text line;
        if (ferryman.isReactionActive()) {
            line = Text.translatable("tip.ferryman.reaction_ready", HudRenderHelper.getAbilityKeyText());
        } else if (ability.getCooldown() > 0) {
            line = Text.translatable("tip.noellesroles.cooldown", ability.getCooldown() / 20);
        } else if (NoellesrolesClient.targetBody != null) {
            line = Text.translatable("tip.ferryman.ferry", HudRenderHelper.getAbilityKeyText());
        } else {
            line = Text.translatable("tip.ferryman.progress", ferryman.getFerriedCount(), ferryman.getFerriedRequired(), ferryman.getBlessingStacks());
        }

        int top = HudRenderHelper.stackLine(bottom, renderer, line, 0);
        if (canPromptPreserve(player, ferryman, ability)) {
            top = HudRenderHelper.stackLine(top, renderer,
                    Text.translatable("tip.ferryman.preserve", HudRenderHelper.getAbility2KeyText()), 0);
        }
        return top;
    }

    /** 技能2“不腐处理”提示行：与引渡提示一样需要无反应窗口、无冷却、正瞄准且尸体尚未处理。 */
    public static boolean canPromptPreserve(ClientPlayerEntity player, FerrymanPlayerComponent ferryman, AbilityPlayerComponent ability) {
        return !ferryman.isReactionActive()
                && ability.getCooldown() <= 0
                && NoellesrolesClient.targetBody != null
                && !FerrymanPreserveWorldComponent.KEY.get(player.getWorld()).isPreserved(NoellesrolesClient.targetBody.getUuid());
    }
}

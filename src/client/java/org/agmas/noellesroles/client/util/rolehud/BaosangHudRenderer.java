package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;

/**
 * BAOSANG 右下角技能提示行（与 BaosangHudMixin 绘制的行一致，仅用于量取堆栈顶部位置）。
 */
public final class BaosangHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        Text line = ability.cooldown > 0
                ? Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20)
                : Text.translatable("tip.baosang.ready");
        return HudRenderHelper.stackLine(bottom, renderer, line, 0);
    }
}

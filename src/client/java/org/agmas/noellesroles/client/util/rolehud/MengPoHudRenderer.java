package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.mengpo.MengPoHelper;
import org.agmas.noellesroles.mengpo.MengPoPlayerComponent;
import org.jetbrains.annotations.Nullable;

/**
 * MENGPO 右下角提示行（与 MengPoHudMixin 绘制的行一致，也用于量取堆栈顶部位置）。
 */
public final class MengPoHudRenderer implements RoleHudRenderer {
    @Nullable
    public static Text getLine(ClientPlayerEntity player) {
        MengPoPlayerComponent component = MengPoPlayerComponent.KEY.get(player);
        if (component.isChoosing()) {
            return Text.translatable("tip.mengpo.choosing", (int) Math.ceil(component.getChoiceTicks() / 20.0));
        }
        if (component.isDuren()) {
            return Text.translatable("tip.mengpo.duren", MengPoHelper.SOUP_PRICE);
        }
        return null;
    }

    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        Text line = getLine(player);
        return line == null ? bottom : HudRenderHelper.stackLine(bottom, renderer, line, 0);
    }
}

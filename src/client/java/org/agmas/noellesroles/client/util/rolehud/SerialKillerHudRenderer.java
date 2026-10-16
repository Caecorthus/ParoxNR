package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;

/**
 * 量取连环杀手右下角技能提示的占用高度：
 * 底行为死亡倒计时，其上一行为技能冷却/按键提示（与 SerialKillerHudMixin 的绘制顺序一致）。
 */
public final class SerialKillerHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        SerialKillerPlayerComponent component = SerialKillerPlayerComponent.KEY.get(player);
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(player);

        int drawY = bottom;
        if (component.getDoomTicks() > 0) {
            Text doomText = Text.translatable("hud.serial_killer.doom", (component.getDoomTicks() + 19) / 20);
            drawY = HudRenderHelper.stackLine(drawY, renderer, doomText, 0);
        }
        Text skillText = abilityComp.getCooldown() > 0
                ? Text.translatable("tip.noellesroles.cooldown", abilityComp.getCooldown() / 20)
                : Text.translatable("hud.serial_killer.activate", HudRenderHelper.getAbilityKeyName());
        return HudRenderHelper.stackLine(drawY, renderer, skillText, 0);
    }
}

package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.bandit.BanditPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;

public final class BanditHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;

        int drawY = bottom;
        for (Text line : getLines(player)) {
            drawY = HudRenderHelper.stackLine(drawY, renderer, line, 0);
        }
        return drawY;
    }

    /**
     * HUD 行，顺序为从下往上：齐掷状态行固定在底部（避免可回收行出现/消失时跳动），
     * 可回收行仅在世界中有自有飞斧时显示。
     */
    public static List<Text> getLines(ClientPlayerEntity player) {
        List<Text> lines = new ArrayList<>();

        int cooldown = AbilityPlayerComponent.KEY.get(player).getCooldown();
        if (cooldown > 0) {
            lines.add(Text.translatable("hud.noellesroles.bandit.volley_cooldown", cooldown / 20));
        } else {
            int axeCount = countAxes(player);
            lines.add(axeCount > 0
                    ? Text.translatable("hud.noellesroles.bandit.volley", HudRenderHelper.getAbilityKeyName(), axeCount)
                    : Text.translatable("hud.noellesroles.bandit.volley_none"));
        }

        int recoverable = BanditPlayerComponent.KEY.get(player).getRecoverableAxes();
        if (recoverable > 0) {
            lines.add(Text.translatable("hud.noellesroles.bandit.shop_recover", recoverable));
        }
        return lines;
    }

    private static int countAxes(ClientPlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.THROWING_AXE)) {
                count++;
            }
        }
        return count;
    }
}

package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.puppeteer.PuppetWorldComponent;
import org.agmas.noellesroles.puppeteer.PuppeteerPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 傀儡师右下角提示（自下而上）：操纵 / 切回提示、傀儡数量、傀儡术状态。
 */
public final class PuppeteerHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;
        int drawY = bottom;
        for (Text line : getLines(player)) {
            drawY = HudRenderHelper.stackLine(drawY, renderer, line, 0);
        }
        return drawY;
    }

    /** 自下而上的各行文字。 */
    public static List<Text> getLines(ClientPlayerEntity player) {
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.KEY.get(player);
        List<Text> lines = new ArrayList<>();

        if (comp.getSwitchCooldown() > 0) {
            lines.add(Text.translatable("tip.puppeteer.switch_cooldown", (comp.getSwitchCooldown() + 19) / 20));
        } else if (comp.isControlling()) {
            lines.add(Text.translatable("tip.puppeteer.controlling",
                    HudRenderHelper.getAbilityKeyName(), HudRenderHelper.getAbility2KeyText()));
        } else if (!PuppeteerClient.getAvailablePuppets(player).isEmpty()) {
            lines.add(Text.translatable("tip.puppeteer.control_ready", HudRenderHelper.getAbilityKeyName()));
        }

        int alive = PuppetWorldComponent.get(player.getWorld()).getOwnedBy(player.getUuid()).size();
        lines.add(Text.translatable("tip.puppeteer.count", comp.getMadeCount(), comp.getMaxPuppets(), alive));

        if (!comp.isControlling()) {
            if (!comp.hasQuotaLeft()) {
                lines.add(Text.translatable("tip.puppeteer.quota_used"));
            } else if (comp.getPuppetryCooldown() > 0) {
                lines.add(Text.translatable("tip.noellesroles.cooldown", (comp.getPuppetryCooldown() + 19) / 20));
            } else {
                lines.add(Text.translatable("tip.puppeteer.puppetry_ready", HudRenderHelper.getAbilityKeyName()));
            }
        }
        return lines;
    }
}

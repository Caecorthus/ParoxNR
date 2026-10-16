package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.TaotieIndicatorClientHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;

import java.util.ArrayList;
import java.util.List;

public final class TaotieHudRenderer implements RoleHudRenderer {
    public record HudLine(Text text, int color) {}

    private static final int HUNGER_PECKISH_COLOR = 0xFFFF55;
    private static final int HUNGER_STARVING_COLOR = 0xFF5555;

    /**
     * HUD 各行，按绘制顺序（自下而上）返回：
     * 饕餮时刻倒计时 → 各被吞玩家的剩余消化时间 → 饥饿值 → 冷却 → 吞噬提示
     */
    public static List<HudLine> getLines(ClientPlayerEntity player) {
        TaotiePlayerComponent comp = TaotiePlayerComponent.KEY.get(player);
        int color = Noellesroles.TAOTIE.color();
        List<HudLine> lines = new ArrayList<>();

        if (comp.isTaotieMomentActive()) {
            lines.add(new HudLine(Text.translatable("tip.taotie.moment_active", comp.getTaotieMomentTicks() / 20), 0xFF0000));
        }

        // 每个被吞玩家一行剩余消化时间（读被吞者自己同步过来的倒计时）
        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other == player) continue;
            SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(other);
            if (!swallowedComp.isSwallowed() || !player.getUuid().equals(swallowedComp.getSwallowedBy())) continue;
            if (swallowedComp.getDigestSeconds() <= 0) continue;
            lines.add(new HudLine(Text.translatable("tip.taotie.digesting",
                    other.getName().getString(), swallowedComp.getDigestSeconds()), color));
        }

        float hunger = comp.getHunger();
        lines.add(new HudLine(Text.translatable("tip.taotie.hunger", Math.round(hunger) + "%"), hungerColor(hunger, color)));

        if (comp.getSwallowCooldown() > 0) {
            lines.add(new HudLine(Text.translatable("tip.noellesroles.cooldown", comp.getSwallowCooldown() / 20), color));
        }

        // 指示器开关提示，位于冷却行上方一行
        if (TaotieIndicatorClientHelper.isUnlocked(comp)) {
            lines.add(new HudLine(Text.translatable(
                    TaotieIndicatorClientHelper.isEnabled()
                            ? "tip.taotie.indicator.close_hint"
                            : "tip.taotie.indicator.open_hint",
                    HudRenderHelper.getAbility2KeyName()), color));
        }

        if (NoellesrolesClient.crosshairTarget != null && NoellesrolesClient.crosshairTargetDistance <= 3.0) {
            SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(NoellesrolesClient.crosshairTarget);
            if (!swallowed.isSwallowed() && comp.getSwallowCooldown() <= 0) {
                lines.add(new HudLine(Text.translatable("tip.taotie.swallow",
                        HudRenderHelper.getAbilityKeyName(),
                        NoellesrolesClient.crosshairTarget.getName().getString()), color));
            }
        }

        return lines;
    }

    private static int hungerColor(float hunger, int roleColor) {
        if (hunger < TaotiePlayerComponent.HUNGER_STARVING_THRESHOLD) return HUNGER_STARVING_COLOR;
        if (hunger < TaotiePlayerComponent.HUNGER_PECKISH_THRESHOLD) return HUNGER_PECKISH_COLOR;
        return roleColor;
    }

    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        List<HudLine> lines = getLines(player);

        int drawY = bottom;
        for (int i = 0; i < lines.size(); i++) {
            int gap = i == lines.size() - 1 ? 0 : HudRenderHelper.LINE_GAP;
            drawY = HudRenderHelper.stackLine(drawY, renderer, lines.get(i).text(), gap);
        }
        return drawY;
    }
}

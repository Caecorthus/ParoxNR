package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.phonism.PhonismPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.UUID;

public final class PhonismHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;
        return HudRenderHelper.stackLine(bottom, renderer, getLine(player), 0);
    }

    public static Text getLine(ClientPlayerEntity player) {
        PhonismPlayerComponent component = PhonismPlayerComponent.KEY.get(player);

        // 1. 正在监听：显示收听中的目标名
        if (component.isRelayActive()) {
            UUID target = component.getCollectorTarget();
            Text targetName = target != null
                    ? RoleScreenHelper.getPlayerName(target, Text.translatable("screen.role_target.unknown_player"))
                    : Text.translatable("screen.role_target.unknown_player");
            return Text.translatable("tip.phonism.relay_active", targetName);
        }

        // 2. 所有音效都在冷却：提示最短剩余时间（下一个解禁的音效）
        if (component.isAllCooling()) {
            int seconds = (component.getMinCooldown() + 19) / 20;
            return Text.translatable("tip.noellesroles.cooldown", seconds);
        }

        // 3. 就绪
        return Text.translatable("tip.phonism.ready", HudRenderHelper.getAbilityKeyName());
    }
}

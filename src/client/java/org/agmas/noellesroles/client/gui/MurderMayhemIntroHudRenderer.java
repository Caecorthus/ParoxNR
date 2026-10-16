package org.agmas.noellesroles.client.gui;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.agmas.noellesroles.murdermayhem.MurderMayhemHelper;
import org.agmas.noellesroles.murdermayhem.MurderMayhemWorldComponent;
import org.jetbrains.annotations.NotNull;

public final class MurderMayhemIntroHudRenderer {
    private static final int DISPLAY_TICKS = 100;
    private static final int TITLE_Y_OFFSET = -18;
    private static final int SUBTITLE_Y_OFFSET = 10;
    private static final int TITLE_COLOR = 0xFFE6DCC6;
    private static final int SUBTITLE_COLOR = 0xFFD4C19B;
    private MurderMayhemIntroHudRenderer() {
    }

    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, @NotNull DrawContext context, float delta) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!MurderMayhemHelper.isMurderMayhem(gameWorld.getGameMode()) || !gameWorld.isRunning()) {
            return;
        }

        MurderMayhemWorldComponent component = MurderMayhemWorldComponent.KEY.get(player.getWorld());
        if (!component.isEventRolled()
                || component.isIntroShown()
                || component.getIntroTicksRemaining() <= 0
                || component.getIntroTicksRemaining() > DISPLAY_TICKS) {
            return;
        }

        int centerX = context.getScaledWindowWidth() / 2;
        int centerY = context.getScaledWindowHeight() / 2;
        Text title = Text.translatable(component.getCurrentEventDisplayNameKey());
        Text subtitle = Text.translatable(component.getCurrentEventDescriptionKey());

        context.getMatrices().push();
        float titleScale = 2.0F;
        context.getMatrices().scale(titleScale, titleScale, 1.0F);
        context.drawCenteredTextWithShadow(renderer, title, Math.round(centerX / titleScale), Math.round((centerY + TITLE_Y_OFFSET) / titleScale), TITLE_COLOR);
        context.getMatrices().pop();
        context.drawCenteredTextWithShadow(renderer, subtitle, centerX, centerY + SUBTITLE_Y_OFFSET, SUBTITLE_COLOR);
    }

    public static void tick(@NotNull ClientPlayerEntity player) {
        // 选择期间介绍倒计时暂停，客户端与服务端同步，避免介绍提前播完再重播
        if (DualRoleSelectionWorldComponent.isSelectionFreezeActive(player.getWorld())) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!MurderMayhemHelper.isMurderMayhem(gameWorld.getGameMode())) {
            return;
        }

        MurderMayhemWorldComponent component = MurderMayhemWorldComponent.KEY.get(player.getWorld());
        if (!component.isEventRolled() || component.isIntroShown() || component.getIntroTicksRemaining() <= 0) {
            return;
        }

        component.tickClientIntroWindow();
    }
}

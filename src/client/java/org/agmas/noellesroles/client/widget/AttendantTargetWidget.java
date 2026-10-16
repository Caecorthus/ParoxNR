package org.agmas.noellesroles.client.widget;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.client.screen.AttendantUi;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class AttendantTargetWidget extends MenuPlayerTargetWidget {
    private static final int NAME_MAX_WIDTH = 34;
    private static final int NAME_TEXT_COLOR = 0xFFFFFFFF;

    private final UUID targetUuid;
    private final String playerName;
    private final String roomName;
    private final byte met;
    private final byte alive;
    private final String roleId;

    public AttendantTargetWidget(int x, int y, UUID targetUuid, String playerName, String roomName,
                                 byte met, byte alive, String roleId, Consumer<UUID> onSelected,
                                 int clipLeft, int clipTop, int clipRight, int clipBottom) {
        super(x, y, Text.literal(playerName), button -> onSelected.accept(targetUuid),
                clipLeft, clipTop, clipRight, clipBottom);
        this.targetUuid = targetUuid;
        this.playerName = playerName;
        this.roomName = roomName;
        this.met = met;
        this.alive = alive;
        this.roleId = roleId;
    }

    @Override
    protected SkinTextures getSkinTextures() {
        return RoleScreenHelper.getPlayerSkinTextures(this.targetUuid);
    }

    @Override
    protected ShopEntry.Type getBackgroundType() {
        return this.alive == AttendantPlayerComponent.NO ? ShopEntry.Type.WEAPON : ShopEntry.Type.TOOL;
    }

    @Override
    protected Text getOverlayText() {
        return Text.literal(this.playerName);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        context.enableScissor(this.clipLeft, this.clipTop, this.clipRight, this.clipBottom);
        try {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            String name = textRenderer.trimToWidth(this.playerName, NAME_MAX_WIDTH);
            context.drawTextWithShadow(textRenderer, name,
                    this.getX() + 8 - textRenderer.getWidth(name) / 2, this.getY() + 25, NAME_TEXT_COLOR);
        } finally {
            context.disableScissor();
        }
    }

    @Override
    public void renderTopmostPlayerOverlay(DrawContext context, TextRenderer textRenderer) {
        List<Text> lines = List.of(
                Text.translatable("screen.attendant.tooltip.id", this.playerName),
                Text.translatable("screen.attendant.tooltip.met", AttendantUi.triStateText(this.met)),
                Text.translatable("screen.attendant.tooltip.alive", AttendantUi.triStateText(this.alive)),
                Text.translatable("screen.attendant.tooltip.role", AttendantUi.roleText(this.roleId)),
                Text.translatable("screen.attendant.tooltip.room", AttendantUi.roomText(this.roomName))
        );
        context.drawTooltip(textRenderer, lines, this.getX() + 8, this.getY() - 4);
    }
}

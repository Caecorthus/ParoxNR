package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.api.Role;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;

import java.util.UUID;

public class AssassinRoleWidget extends ButtonWidget {
    public final Role role;

    private static final int BG_COLOR_NORMAL = 0xAA000000;
    private static final int BG_COLOR_HOVER = 0xCC300000;

    private final int clipLeft;
    private final int clipTop;
    private final int clipRight;
    private final int clipBottom;

    public AssassinRoleWidget(int x, int y, Role role, UUID targetPlayer, boolean fromBook,
                              int clipLeft, int clipTop, int clipRight, int clipBottom) {
        super(x, y, 90, 20, Text.empty(),
                button -> {
                    ClientPlayNetworking.send(new AssassinGuessRoleC2SPacket(targetPlayer, role.identifier(), fromBook));
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.closeHandledScreen();
                    }
                }, DEFAULT_NARRATION_SUPPLIER);
        this.role = role;
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            boolean hovered = isHovered();
            int bgColor = hovered ? BG_COLOR_HOVER : BG_COLOR_NORMAL;
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            int borderColor = hovered ? 0xFFFF0000 : 0xFF444444;
            context.drawBorder(getX(), getY(), getWidth(), getHeight(), borderColor);

            int textColor = hovered ? 0xFFFFFF : role.color();
            Text roleName = Text.translatable("announcement.role." + role.identifier().getPath());
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    roleName,
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    textColor
            );

            if (hovered) {
                context.fill(getX(), getY(), getX() + 2, getY() + 2, 0xFFFF0000);
                context.fill(getX() + getWidth() - 2, getY() + getHeight() - 2, getX() + getWidth(), getY() + getHeight(), 0xFFFF0000);
            }
        } finally {
            context.disableScissor();
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return RoleScreenHelper.containsPoint(mouseX, mouseY, clipLeft, clipTop, clipRight, clipBottom)
                && super.isMouseOver(mouseX, mouseY);
    }
}

package org.agmas.noellesroles.client.widget;

import dev.doctor4t.wathe.api.Role;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.packet.AttendantEditInfoC2SPacket;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/** 登记面板编辑视图中的角色选择按钮；role 为 null 表示「未知」清除项。 */
public class AttendantRoleChoiceWidget extends ButtonWidget {
    private static final int BG_COLOR_NORMAL = 0xAA000000;
    private static final int BG_COLOR_HOVER = 0xCC243C5C;
    private static final int TEXT_COLOR_UNKNOWN = 0xFFAAAAAA;

    private final Role role;
    private final BooleanSupplier selectedSupplier;
    private final int accentColor;
    private final int clipLeft;
    private final int clipTop;
    private final int clipRight;
    private final int clipBottom;

    public AttendantRoleChoiceWidget(int x, int y, int width, Role role, String roleId, BooleanSupplier selectedSupplier,
                                     Runnable onChosen, UUID targetUuid, int accentColor,
                                     int clipLeft, int clipTop, int clipRight, int clipBottom) {
        super(x, y, width, 20, Text.empty(),
                button -> {
                    ClientPlayNetworking.send(new AttendantEditInfoC2SPacket(targetUuid, 2, roleId));
                    if (onChosen != null) {
                        onChosen.run();
                    }
                },
                DEFAULT_NARRATION_SUPPLIER);
        this.role = role;
        this.selectedSupplier = selectedSupplier;
        this.accentColor = accentColor;
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(this.clipLeft, this.clipTop, this.clipRight, this.clipBottom);
        try {
            boolean hovered = this.isHovered();
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                    hovered ? BG_COLOR_HOVER : BG_COLOR_NORMAL);

            boolean selected = this.selectedSupplier.getAsBoolean();
            int borderColor = selected ? this.accentColor : (hovered ? 0xFFFFFFFF : 0xFF444444);
            context.drawBorder(this.getX(), this.getY(), this.getWidth(), this.getHeight(), borderColor);

            Text label = this.role != null
                    ? Text.translatable("announcement.role." + this.role.identifier().getPath())
                    : Text.translatable("screen.attendant.value.unknown");
            int textColor = hovered ? 0xFFFFFF : (this.role != null ? this.role.color() : TEXT_COLOR_UNKNOWN);
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    label,
                    this.getX() + this.getWidth() / 2,
                    this.getY() + (this.getHeight() - 8) / 2,
                    textColor
            );
        } finally {
            context.disableScissor();
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return RoleScreenHelper.containsPoint(mouseX, mouseY, this.clipLeft, this.clipTop, this.clipRight, this.clipBottom)
                && super.isMouseOver(mouseX, mouseY);
    }
}

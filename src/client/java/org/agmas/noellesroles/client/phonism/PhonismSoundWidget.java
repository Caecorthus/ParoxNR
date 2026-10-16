package org.agmas.noellesroles.client.phonism;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.packet.PhonismC2SPacket;
import org.agmas.noellesroles.phonism.PhonismSound;

import java.util.UUID;

// 音效选择按钮：无图标，只有名称文字
public class PhonismSoundWidget extends ButtonWidget {
    public static final int WIDTH = 90;
    public static final int HEIGHT = 20;

    private static final int ACCENT_COLOR = 0xFF000000 | (Noellesroles.PHONISM.color() & 0x00FFFFFF);
    private static final int BG_COLOR_NORMAL = 0xAA000000;
    private static final int BG_COLOR_HOVER = 0xCC2A1A45;
    private static final int BORDER_COLOR_NORMAL = 0xFF444444;

    public final PhonismSound sound;

    private final int clipLeft;
    private final int clipTop;
    private final int clipRight;
    private final int clipBottom;

    public PhonismSoundWidget(int x, int y, PhonismSound sound, UUID targetPlayer,
                              int clipLeft, int clipTop, int clipRight, int clipBottom) {
        super(x, y, WIDTH, HEIGHT, Text.empty(),
                button -> {
                    ClientPlayNetworking.send(new PhonismC2SPacket(targetPlayer, sound.key()));
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.closeHandledScreen();
                    }
                }, DEFAULT_NARRATION_SUPPLIER);
        this.sound = sound;
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            boolean hovered = this.isHovered();
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    hovered ? BG_COLOR_HOVER : BG_COLOR_NORMAL);
            context.drawBorder(getX(), getY(), getWidth(), getHeight(),
                    hovered ? ACCENT_COLOR : BORDER_COLOR_NORMAL);
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    Text.translatable("screen.phonism.sound." + this.sound.key()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    hovered ? 0xFFFFFF : Noellesroles.PHONISM.color()
            );
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

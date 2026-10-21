package org.agmas.noellesroles.client.phonism;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.packet.PhonismC2SPacket;
import org.agmas.noellesroles.phonism.PhonismPlayerComponent;
import org.agmas.noellesroles.phonism.PhonismSound;

import java.util.UUID;

// 音效选择按钮：无图标，只有名称文字；每个音效独立冷却，冷却中灰化不可点并显示剩余秒数
public class PhonismSoundWidget extends ButtonWidget {
    public static final int WIDTH = 90;
    public static final int HEIGHT = 20;

    private static final int ACCENT_COLOR = 0xFF000000 | (Noellesroles.PHONISM.color() & 0x00FFFFFF);
    private static final int BG_COLOR_NORMAL = 0xAA000000;
    private static final int BG_COLOR_HOVER = 0xCC2A1A45;
    private static final int BG_COLOR_DISABLED = 0xAA16121F;
    private static final int BORDER_COLOR_NORMAL = 0xFF444444;
    private static final int TEXT_COLOR_DISABLED = 0xFF777777;

    public final PhonismSound sound;
    private final PhonismPlayerComponent component;

    private final int clipLeft;
    private final int clipTop;
    private final int clipRight;
    private final int clipBottom;

    public PhonismSoundWidget(int x, int y, PhonismSound sound, UUID targetPlayer,
                              PhonismPlayerComponent component,
                              int clipLeft, int clipTop, int clipRight, int clipBottom) {
        super(x, y, WIDTH, HEIGHT, Text.empty(),
                button -> {
                    ClientPlayNetworking.send(new PhonismC2SPacket(targetPlayer, sound.key()));
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.closeHandledScreen();
                    }
                }, DEFAULT_NARRATION_SUPPLIER);
        this.sound = sound;
        this.component = component;
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int cooldownTicks = this.component.getSoundCooldown(this.sound);
        boolean cooling = cooldownTicks > 0;
        this.active = !cooling;

        context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            boolean hovered = !cooling && this.isHovered();
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    cooling ? BG_COLOR_DISABLED : (hovered ? BG_COLOR_HOVER : BG_COLOR_NORMAL));
            context.drawBorder(getX(), getY(), getWidth(), getHeight(),
                    cooling ? BORDER_COLOR_NORMAL : (hovered ? ACCENT_COLOR : BORDER_COLOR_NORMAL));
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    Text.translatable("screen.phonism.sound." + this.sound.key()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    cooling ? TEXT_COLOR_DISABLED : (hovered ? 0xFFFFFF : Noellesroles.PHONISM.color())
            );
            if (cooling) {
                // 剩余秒数向上取整显示在按钮右侧
                String remaining = ((cooldownTicks + 19) / 20) + "s";
                context.drawTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        Text.literal(remaining),
                        getX() + getWidth() - 6 - MinecraftClient.getInstance().textRenderer.getWidth(remaining),
                        getY() + (getHeight() - 8) / 2,
                        Noellesroles.PHONISM.color()
                );
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

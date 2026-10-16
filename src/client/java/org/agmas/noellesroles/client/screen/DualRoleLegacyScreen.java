package org.agmas.noellesroles.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.packet.DualRoleChoiceC2SPacket;
import org.agmas.noellesroles.packet.DualRoleOptionsS2CPacket;

import java.util.List;

/**
 * 双角色选择屏（旧版风格）。两种模式：
 * - canChoose=true：显示"保留/切换"两个按钮；点击后发送选择但不关闭，等待 5 秒倒计时结束统一关闭
 * - canChoose=false：固定槽位/无备选玩家，显示"你的角色是 XXX，为了列车上的众人前进吧！"，5 秒后自动关闭
 * 倒计时结束前禁止手动关闭（ESC 无效），防止部分玩家提前行动。
 */
public class DualRoleLegacyScreen extends Screen {
    private static final int ROLE_COLOR = 0xFF4A90D9;
    private static final int COUNTDOWN_COLOR = 0xFFAA44;

    private final boolean canChoose;
    private final String firstRoleDisplayKey;
    private final String secondRoleDisplayKey;
    private final long endTimeMs;
    private boolean choiceMade;
    private ButtonWidget keepButton;
    private ButtonWidget switchButton;

    public DualRoleLegacyScreen(DualRoleOptionsS2CPacket payload) {
        super(Text.translatable("screen.dual_role.title"));
        this.canChoose = payload.canChoose();
        this.firstRoleDisplayKey = payload.firstRoleDisplayKey();
        this.secondRoleDisplayKey = payload.secondRoleDisplayKey();
        this.endTimeMs = System.currentTimeMillis() + (payload.totalTicks() * 50L);
        this.choiceMade = false;
    }

    @Override
    protected void init() {
        super.init();
        if (!canChoose) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        Text firstRoleText = Text.translatable("screen.dual_role.keep", Text.translatable(firstRoleDisplayKey));
        Text secondRoleText = Text.translatable("screen.dual_role.switch", Text.translatable(secondRoleDisplayKey));

        this.keepButton = addDrawableChild(ButtonWidget.builder(firstRoleText, button -> selectRole(false))
                .dimensions(centerX - 120, centerY - 40, 240, 24)
                .build());

        this.switchButton = addDrawableChild(ButtonWidget.builder(secondRoleText, button -> selectRole(true))
                .dimensions(centerX - 120, centerY + 10, 240, 24)
                .build());
    }

    /** 点击选择：发送数据包但不关闭 GUI，等待倒计时结束统一关闭，防止提前行动 */
    private void selectRole(boolean choseSecond) {
        if (choiceMade) return;
        choiceMade = true;
        ClientPlayNetworking.send(new DualRoleChoiceC2SPacket(choseSecond));
        if (this.keepButton != null) this.keepButton.active = false;
        if (this.switchButton != null) this.switchButton.active = false;
    }

    private boolean countdownDone() {
        return System.currentTimeMillis() >= endTimeMs;
    }

    @Override
    public void tick() {
        super.tick();
        if (!countdownDone()) return;
        if (canChoose && !choiceMade) {
            // 超时未选择：默认保留第一角色
            selectRole(false);
        }
        forceClose();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (canChoose) {
            RoleScreenHelper.drawCenteredTitle(context, font,
                    Text.translatable("screen.dual_role.title"), centerX,
                    centerY - 80);
            if (choiceMade) {
                context.drawCenteredTextWithShadow(font,
                        Text.translatable("screen.dual_role.chosen"), centerX, centerY + 46, 0x55FF55);
            }
        } else {
            // 宣言屏：居中显示"你的角色是 XXX，为了列车上的众人前进吧！"
            Text holdText = Text.translatable("screen.dual_role.hold", Text.translatable(firstRoleDisplayKey));
            List<net.minecraft.text.OrderedText> lines = font.wrapLines(holdText, this.width - 80);
            int lineY = centerY - (lines.size() * 12) / 2;
            for (net.minecraft.text.OrderedText line : lines) {
                int lineWidth = font.getWidth(line);
                context.drawText(font, line, centerX - lineWidth / 2, lineY, 0xFFFFFF, true);
                lineY += 12;
            }
        }

        long remainingMs = Math.max(0, endTimeMs - System.currentTimeMillis());
        int seconds = (int) Math.ceil(remainingMs / 1000.0);
        Text countdownText = Text.translatable(canChoose ? "screen.dual_role.countdown" : "screen.dual_role.hold_countdown", seconds);
        context.drawCenteredTextWithShadow(font, countdownText, centerX, centerY + 64, COUNTDOWN_COLOR);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RoleScreenHelper.renderRoleMenuBackground(context, this.width, this.height, ROLE_COLOR);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        // 倒计时结束前禁止关闭（含 ESC），防止提前行动
        if (!countdownDone()) return;
        forceClose();
    }

    private void forceClose() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }
}

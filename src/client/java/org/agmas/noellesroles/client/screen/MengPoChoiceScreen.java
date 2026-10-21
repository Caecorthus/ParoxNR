package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.mengpo.MengPoPlayerComponent;
import org.agmas.noellesroles.packet.MengPoChoiceC2SPacket;

import java.util.List;

/**
 * 孟婆开局「渡人 / 渡己」选择屏。倒计时读自孟婆组件，选择期间禁止关闭（含 ESC），
 * 选完后等服务端结算（组件不再处于选择中或职业已变）再关闭。
 */
public class MengPoChoiceScreen extends Screen {
    private static final int ACCENT_BAR_COLOR = 0xFF000000 | (Noellesroles.MENGPO.color() & 0x00FFFFFF);
    // 倒计时/提示文字按职业色显示
    private static final int PROMPT_COLOR = Noellesroles.MENGPO.color() & 0x00FFFFFF;
    private static final int DESCRIPTION_COLOR = 0xCCCCCC;
    private static final int BUTTON_WIDTH = 240;
    private static final int BUTTON_HEIGHT = 24;

    private boolean choiceMade;
    private ButtonWidget durenButton;
    private ButtonWidget dujiButton;

    public MengPoChoiceScreen() {
        super(Text.translatable("screen.mengpo.title"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.durenButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mengpo.duren"), button -> choose(false))
                .dimensions(centerX - BUTTON_WIDTH / 2, centerY - 50, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.dujiButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mengpo.duji"), button -> choose(true))
                .dimensions(centerX - BUTTON_WIDTH / 2, centerY + 14, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        updateButtons();
    }

    private void choose(boolean duji) {
        if (this.choiceMade) return;
        this.choiceMade = true;
        ClientPlayNetworking.send(new MengPoChoiceC2SPacket(duji));
        updateButtons();
    }

    private void updateButtons() {
        if (this.durenButton != null) this.durenButton.active = !this.choiceMade;
        if (this.dujiButton != null) this.dujiButton.active = !this.choiceMade;
    }

    /** 本地玩家仍是孟婆且处于开局选择中。 */
    public static boolean isChoosing(ClientPlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerPlayingAndAlive(player)) return false;
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.MENGPO)) return false;
        return MengPoPlayerComponent.KEY.get(player).isChoosing();
    }

    @Override
    public void tick() {
        super.tick();
        if (!isChoosing(MinecraftClient.getInstance().player)) {
            forceClose();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.mengpo.title"), centerX, centerY - 96);
        RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.mengpo.subtitle"), centerX, centerY - 76);
        drawDescription(context, font, Text.translatable("screen.mengpo.duren.desc"), centerX, centerY - 50 + BUTTON_HEIGHT + 4);
        drawDescription(context, font, Text.translatable("screen.mengpo.duji.desc"), centerX, centerY + 14 + BUTTON_HEIGHT + 4);

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        int seconds = player == null ? 0 : (int) Math.ceil(MengPoPlayerComponent.KEY.get(player).getChoiceTicks() / 20.0);
        Text bottom = this.choiceMade
                ? Text.translatable("screen.mengpo.chosen")
                : Text.translatable("screen.mengpo.countdown", seconds);
        context.drawCenteredTextWithShadow(font, bottom, centerX, centerY + 82, PROMPT_COLOR);
    }

    private void drawDescription(DrawContext context, TextRenderer font, Text text, int centerX, int y) {
        List<OrderedText> lines = font.wrapLines(text, BUTTON_WIDTH + 60);
        for (OrderedText line : lines) {
            context.drawText(font, line, centerX - font.getWidth(line) / 2, y, DESCRIPTION_COLOR, true);
            y += 10;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RoleScreenHelper.renderRoleMenuBackground(context, this.width, this.height, ACCENT_BAR_COLOR);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        // 选择结算前禁止关闭（含 ESC）
        if (isChoosing(MinecraftClient.getInstance().player)) return;
        forceClose();
    }

    private void forceClose() {
        if (this.client != null && this.client.currentScreen == this) {
            this.client.setScreen(null);
        }
    }
}

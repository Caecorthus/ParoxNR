package org.agmas.noellesroles.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.client.roleinfo.RoleInfoData;
import org.agmas.noellesroles.client.roleinfo.RoleInfoLookup;
import org.agmas.noellesroles.client.roleinfo.RoleInfoRegistry;
import org.agmas.noellesroles.client.screen.book.BookPaint;
import org.agmas.noellesroles.client.screen.book.BookPalette;
import org.agmas.noellesroles.client.screen.book.BookRect;
import org.agmas.noellesroles.client.screen.book.BookText;
import org.agmas.noellesroles.packet.DualRoleChoiceC2SPacket;
import org.agmas.noellesroles.packet.DualRoleOptionsS2CPacket;

import java.util.List;

/**
 * 双角色选择屏（书页风格）。两种模式：
 * - canChoose=true：两个职业选项卡片；点击后发送选择但不关闭，等待倒计时结束统一关闭
 * - canChoose=false：仅显示"你的角色是 XXX"宣言页，倒计时后自动关闭
 * 倒计时结束前禁止手动关闭（ESC 无效），防止部分玩家提前行动。
 */
public class DualRoleSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_MARGIN = 40;
    private static final int BAND_HEIGHT = 23;
    private static final int PAD = 8;
    private static final int ROW_GAP = 4;
    private static final int ROW_PAD = 5;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_INDENT = 19;
    private static final int MAX_SUMMARY_LINES = 2;

    private final boolean canChoose;
    private final Identifier firstRoleId;
    private final String firstRoleDisplayKey;
    private final Identifier secondRoleId;
    private final String secondRoleDisplayKey;
    private final long endTimeMs;
    private boolean choiceMade;
    private boolean choseSecond;

    public DualRoleSelectionScreen(DualRoleOptionsS2CPacket payload) {
        super(Text.translatable("screen.dual_role.title"));
        this.canChoose = payload.canChoose();
        this.firstRoleId = payload.firstRoleId();
        this.firstRoleDisplayKey = payload.firstRoleDisplayKey();
        this.secondRoleId = payload.secondRoleId();
        this.secondRoleDisplayKey = payload.secondRoleDisplayKey();
        this.endTimeMs = System.currentTimeMillis() + (payload.totalTicks() * 50L);
    }

    /** Opens the book style screen, or the legacy button screen when the config option is enabled. */
    public static Screen create(DualRoleOptionsS2CPacket payload) {
        return NoellesRolesConfig.HANDLER.instance().legacyGuiStyle ? new DualRoleLegacyScreen(payload) : new DualRoleSelectionScreen(payload);
    }

    public static boolean isSelectionScreenOpen() {
        Screen screen = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
        return screen instanceof DualRoleSelectionScreen || screen instanceof DualRoleLegacyScreen;
    }

    /** 点击选择：发送数据包但不关闭 GUI，等待倒计时结束统一关闭，防止提前行动 */
    private void selectRole(boolean choseSecond) {
        if (choiceMade) return;
        choiceMade = true;
        this.choseSecond = choseSecond;
        ClientPlayNetworking.send(new DualRoleChoiceC2SPacket(choseSecond));
    }

    private boolean countdownDone() {
        return System.currentTimeMillis() >= endTimeMs;
    }

    private int countdownSeconds() {
        return (int) Math.ceil(Math.max(0, endTimeMs - System.currentTimeMillis()) / 1000.0);
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
        if (canChoose) {
            renderChoose(context, mouseX, mouseY);
        } else {
            renderHold(context);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 双选期间保持全黑，开局渐亮在选完之后重放（见 DualRoleSelectionWorldComponent.applyResults）
        context.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    // ==================== Choose mode ====================

    private void renderChoose(DrawContext context, int mouseX, int mouseY) {
        TextRenderer font = this.textRenderer;
        OptionEntry first = buildEntry(firstRoleId, firstRoleDisplayKey, false);
        OptionEntry second = buildEntry(secondRoleId, secondRoleDisplayKey, true);
        ChooseLayout layout = chooseLayout(first, second);

        BookRect panel = layout.panel();
        BookPaint.panel(context, panel.x(), panel.y(), panel.width(), panel.height());
        BookPaint.titleBand(context, panel.x(), panel.y(), panel.width());
        BookPaint.gem(context, panel.x() + 7, panel.y() + 9, 0xFFBF49, false);
        context.drawText(font, this.title, panel.x() + 15, panel.y() + 7, BookPalette.TITLE, false);

        boolean firstHovered = !choiceMade && layout.firstRect().contains(mouseX, mouseY);
        boolean secondHovered = !choiceMade && layout.secondRect().contains(mouseX, mouseY);
        renderOptionRow(context, first, layout.firstRect(), firstHovered);
        renderOptionRow(context, second, layout.secondRect(), secondHovered);

        Text status = choiceMade
                ? Text.translatable("screen.dual_role.chosen")
                : Text.translatable("screen.dual_role.countdown", countdownSeconds());
        context.drawCenteredTextWithShadow(font, status, panel.x() + panel.width() / 2,
                layout.secondRect().bottom() + 6, BookPalette.COIN);
    }

    private void renderOptionRow(DrawContext context, OptionEntry entry, BookRect rect, boolean hovered) {
        TextRenderer font = this.textRenderer;
        boolean selected = choiceMade && entry.second() == choseSecond;
        boolean dimmed = choiceMade && !selected;
        BookPaint.rowState(context, rect.x(), rect.y(), rect.right(), rect.height(), hovered, selected);
        BookPaint.gem(context, rect.x() + 10, rect.y() + ROW_PAD, entry.gemColor(), hovered || selected);

        Text prefix = Text.translatable(entry.second() ? "screen.dual_role.switch_prefix" : "screen.dual_role.keep_prefix");
        int nameX = rect.x() + TEXT_INDENT;
        int prefixColor = dimmed ? BookPalette.FAINT : BookPalette.MUTED;
        int nameColor = dimmed ? BookPalette.FAINT : (hovered || selected ? BookPalette.TITLE : BookPalette.TEXT_HI);
        context.drawText(font, prefix, nameX, rect.y() + ROW_PAD, prefixColor, true);
        context.drawText(font, RoleInfoRegistry.resolveText(entry.displayKey()), nameX + font.getWidth(prefix), rect.y() + ROW_PAD, nameColor, true);

        int lineY = rect.y() + ROW_PAD + LINE_HEIGHT;
        int lineCount = Math.min(entry.summaryLines().size(), MAX_SUMMARY_LINES);
        int color = dimmed ? BookPalette.FAINT : BookPalette.MUTED;
        for (int i = 0; i < lineCount; i++) {
            List<BookText.Cell> line = entry.summaryLines().get(i);
            if (i == lineCount - 1 && entry.summaryLines().size() > lineCount) {
                String truncated = BookPaint.ellipsize(font, BookText.string(line) + "…", rect.width() - TEXT_INDENT - 8);
                context.drawText(font, truncated, nameX, lineY, color, true);
            } else {
                context.drawText(font, BookText.toOrderedText(line), nameX, lineY, color, true);
            }
            lineY += LINE_HEIGHT;
        }
    }

    private OptionEntry buildEntry(Identifier roleId, String displayKey, boolean second) {
        RoleInfoData info = RoleInfoRegistry.get(roleId.toString());
        List<List<BookText.Cell>> lines = List.of();
        if (info != null && info.summaryKey != null) {
            Text summary = RoleInfoRegistry.resolveText(info.summaryKey);
            if (!summary.getString().isBlank()) {
                lines = BookText.wrap(BookText.normalise(BookText.cells(summary, BookPalette.MUTED)),
                        panelWidth() - PAD * 2 - TEXT_INDENT - 8, this.textRenderer, true);
                if (lines.size() == 1 && lines.get(0).isEmpty()) {
                    lines = List.of();
                }
            }
        }
        Integer roleColor = RoleInfoLookup.roleColor(roleId.toString());
        int gemColor = roleColor == null ? BookPalette.BRASS_HI : roleColor & 0xFFFFFF;
        return new OptionEntry(displayKey, second, gemColor, lines);
    }

    private ChooseLayout chooseLayout(OptionEntry first, OptionEntry second) {
        int panelWidth = panelWidth();
        int rowWidth = panelWidth - PAD * 2;
        int panelHeight = BAND_HEIGHT + 6 + rowHeight(first) + ROW_GAP + rowHeight(second) + 6 + LINE_HEIGHT + 6;
        BookRect panel = centeredPanel(panelWidth, panelHeight);
        int rowY = panel.y() + BAND_HEIGHT + 6;
        BookRect firstRect = new BookRect(panel.x() + PAD, rowY, rowWidth, rowHeight(first));
        rowY += rowHeight(first) + ROW_GAP;
        BookRect secondRect = new BookRect(panel.x() + PAD, rowY, rowWidth, rowHeight(second));
        return new ChooseLayout(panel, firstRect, secondRect);
    }

    private static int rowHeight(OptionEntry entry) {
        return ROW_PAD * 2 + LINE_HEIGHT + Math.min(entry.summaryLines().size(), MAX_SUMMARY_LINES) * LINE_HEIGHT;
    }

    // ==================== Hold mode ====================

    private void renderHold(DrawContext context) {
        TextRenderer font = this.textRenderer;
        Text roleName = RoleInfoRegistry.resolveText(firstRoleDisplayKey);
        Integer roleColor = RoleInfoLookup.roleColor(firstRoleId.toString());
        int gemColor = roleColor == null ? BookPalette.BRASS_HI : roleColor & 0xFFFFFF;

        int panelWidth = panelWidth();
        Text message = Text.translatable("screen.dual_role.hold", roleName);
        List<OrderedText> messageLines = font.wrapLines(message, panelWidth - PAD * 2 - 24);
        int sheetHeight = 42 + messageLines.size() * LINE_HEIGHT;
        BookRect panel = centeredPanel(panelWidth, BAND_HEIGHT + 5 + sheetHeight + 5);

        BookPaint.panel(context, panel.x(), panel.y(), panel.width(), panel.height());
        BookPaint.titleBand(context, panel.x(), panel.y(), panel.width());
        BookPaint.gem(context, panel.x() + 7, panel.y() + 9, gemColor, false);
        context.drawText(font, roleName, panel.x() + 15, panel.y() + 7, BookPalette.HEADING, false);

        BookRect sheet = new BookRect(panel.x() + 4, panel.y() + 22, panel.width() - 8, panel.height() - 26);
        BookPaint.paper(context, sheet.x(), sheet.y(), sheet.width(), sheet.height());

        int centerX = panel.x() + panel.width() / 2;
        int textY = sheet.y() + 12;
        for (OrderedText line : messageLines) {
            context.drawText(font, line, centerX - font.getWidth(line) / 2, textY, BookPalette.INK, false);
            textY += LINE_HEIGHT;
        }
        Text countdown = Text.translatable("screen.dual_role.hold_countdown", countdownSeconds());
        context.drawText(font, countdown, centerX - font.getWidth(countdown) / 2, textY + 8, BookPalette.INK_MUTED, false);
    }

    // ==================== Layout ====================

    private int panelWidth() {
        return Math.max(180, Math.min(PANEL_WIDTH, this.width - PANEL_MARGIN));
    }

    private BookRect centeredPanel(int panelWidth, int panelHeight) {
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(4, (this.height - panelHeight) / 2);
        return new BookRect(x, y, panelWidth, panelHeight);
    }

    // ==================== Input ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && canChoose && !choiceMade) {
            OptionEntry first = buildEntry(firstRoleId, firstRoleDisplayKey, false);
            OptionEntry second = buildEntry(secondRoleId, secondRoleDisplayKey, true);
            ChooseLayout layout = chooseLayout(first, second);
            if (layout.firstRect().contains(mouseX, mouseY)) {
                selectRole(false);
                return true;
            }
            if (layout.secondRect().contains(mouseX, mouseY)) {
                selectRole(true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    private record OptionEntry(String displayKey, boolean second, int gemColor, List<List<BookText.Cell>> summaryLines) {
    }

    private record ChooseLayout(BookRect panel, BookRect firstRect, BookRect secondRect) {
    }
}

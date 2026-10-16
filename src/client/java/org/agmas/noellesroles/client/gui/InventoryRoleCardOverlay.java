package org.agmas.noellesroles.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.client.roleinfo.RoleInfoData;
import org.agmas.noellesroles.client.roleinfo.RoleInfoLookup;
import org.agmas.noellesroles.client.roleinfo.RoleInfoRegistry;
import org.agmas.noellesroles.client.screen.book.BookPage;
import org.agmas.noellesroles.client.screen.book.BookPaint;
import org.agmas.noellesroles.client.screen.book.BookPalette;
import org.agmas.noellesroles.client.screen.book.BookText;

import java.util.List;

/**
 * Book-styled role card shown in the bottom-left of the limited inventory:
 * current role, a short summary, and a hint pointing at the role info screen.
 */
public final class InventoryRoleCardOverlay {
    private static final int PANEL_WIDTH = 176;
    private static final int MAX_SUMMARY_LINES = 4;

    private InventoryRoleCardOverlay() {
    }

    public static void render(DrawContext context, TextRenderer font, int screenHeight) {
        if (NoellesRolesConfig.HANDLER.instance().legacyGuiStyle) {
            return;
        }
        String roleId = RoleInfoLookup.currentRoleId();
        if (roleId == null) {
            return;
        }
        RoleInfoData info = RoleInfoRegistry.get(roleId);
        if (info == null) {
            return;
        }

        Text summary = RoleInfoRegistry.resolveText(info.summaryKey != null ? info.summaryKey : info.descriptionKey);
        List<List<BookText.Cell>> wrapped = BookText.wrap(
                BookText.normalise(BookText.cells(summary, BookPalette.TEXT)), PANEL_WIDTH - 20, font, true);
        if (wrapped.size() == 1 && wrapped.get(0).isEmpty()) {
            wrapped = List.of();
        }
        int lineCount = Math.min(wrapped.size(), MAX_SUMMARY_LINES);
        // 杀手阵营额外展示一行本能透视提示
        boolean killer = RoleInfoRegistry.CATEGORY_KILLER.equals(RoleInfoRegistry.getFactionCategoryId(info));
        int instinctOffset = killer ? BookPage.LINE_HEIGHT + 2 : 0;

        int height = 50 + lineCount * BookPage.LINE_HEIGHT + instinctOffset;
        int x = 4;
        int y = Math.max(4, screenHeight / 4 - height);

        BookPaint.panel(context, x, y, PANEL_WIDTH, height);

        BookPaint.star(context, x + 10, y + 11, BookPalette.COIN);
        Text prefix = Text.translatable("roleinfo.current_role.prefix");
        int prefixX = x + 20;
        context.drawText(font, prefix, prefixX, y + 12, BookPalette.MUTED, true);
        Integer roleColor = RoleInfoLookup.roleColor(roleId);
        int nameColor = roleColor != null
                ? BookPaint.mix(0xFF000000 | (roleColor & 0xFFFFFF), BookPalette.TEXT_HI, 0.35)
                : BookPalette.TEXT_HI;
        context.drawText(font, RoleInfoRegistry.resolveText(info.nameKey), prefixX + font.getWidth(prefix), y + 12, nameColor, true);

        BookPaint.etched(context, x + 10, x + PANEL_WIDTH - 10, y + 25);

        int lineY = y + 29;
        for (int i = 0; i < lineCount; i++) {
            if (i == lineCount - 1 && wrapped.size() > lineCount) {
                String truncated = BookPaint.ellipsize(font, BookText.string(wrapped.get(i)) + "…", PANEL_WIDTH - 20);
                context.drawText(font, truncated, x + 10, lineY, BookPalette.TEXT, true);
            } else {
                context.drawText(font, BookText.toOrderedText(wrapped.get(i)), x + 10, lineY, BookPalette.TEXT, true);
            }
            lineY += BookPage.LINE_HEIGHT;
        }

        int hintY = y + 33 + lineCount * BookPage.LINE_HEIGHT;
        if (killer) {
            Text instinct = Text.translatable("roleinfo.inventory.instinct", RoleInfoRegistry.resolveKeybind("instinct"));
            context.drawText(font, BookPaint.ellipsize(font, instinct.getString(), PANEL_WIDTH - 20), x + 10, hintY, BookPalette.MUTED, true);
            hintY += BookPage.LINE_HEIGHT + 2;
        }
        BookPaint.bookIcon(context, x + 10, hintY + 1, BookPalette.BRASS_LO);
        Text hint = Text.translatable("roleinfo.inventory.hint", RoleInfoRegistry.resolveKeybind("role_info"));
        context.drawText(font, BookPaint.ellipsize(font, hint.getString(), PANEL_WIDTH - 40), x + 24, hintY, BookPalette.MUTED, true);
    }
}

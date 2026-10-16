package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.client.roleinfo.RoleInfoData;
import org.agmas.noellesroles.client.roleinfo.RoleInfoLookup;
import org.agmas.noellesroles.client.roleinfo.RoleInfoRegistry;
import org.agmas.noellesroles.client.screen.book.BookPage;
import org.agmas.noellesroles.client.screen.book.BookRect;
import org.agmas.noellesroles.client.screen.book.BookPaint;
import org.agmas.noellesroles.client.screen.book.BookPalette;
import org.agmas.noellesroles.client.screen.book.BookText;
import org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent;
import org.agmas.noellesroles.murdermayhem.MurderMayhemWorldComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RoleInfoScreen extends Screen {
    private static final int SPREAD_GAP = 8;
    private static final int READER_WIDTH = 284;
    private static final int TOP_LINE = 20;
    private static final int BAND_BOTTOM = 23;
    private static final int SEARCH_BOTTOM = 42;
    private static final int FOOTER_HEIGHT = 22;
    private static final int EMPTY_BODY = 44;
    private static final int ROOT_HEIGHT = 14;
    private static final int ROOT_GAP = 3;
    private static final int ROW_HEIGHT = 12;
    private static final int ROW_LABEL_PAD = 11;
    private static final int READER_HEADER_HEIGHT = 37;
    private static final int RUNNING_HEAD_SCROLL = 29;
    private static final int TEXT_PAD = 11;

    private final Set<String> expandedCategoryIds = new LinkedHashSet<>();
    private final Set<String> searchExpandedCategoryIds = new LinkedHashSet<>();
    private final List<TreeEntry> visibleEntries = new ArrayList<>();
    private final List<PaintRow> paintRows = new ArrayList<>();
    private final Map<String, Integer> subtreeCounts = new LinkedHashMap<>();
    private final Map<String, Set<String>> subtreeIds = new LinkedHashMap<>();

    private TextFieldWidget searchField;
    private boolean searchExpanded;
    private int treeScrollOffset;
    private int detailScrollOffset;
    private boolean draggingTreeScrollbar;
    private boolean draggingDetailScrollbar;
    private int treeDragOffset;
    private int detailDragOffset;
    private boolean compactReaderOpen;
    private String selectedEntryId = RoleInfoRegistry.CATEGORY_CLASSIC;
    private int directoryContentHeight;

    private BookPage.Layout cachedLayout = BookPage.Layout.EMPTY;
    private String cachedLayoutId;
    private int cachedLayoutWidth = -1;

    public RoleInfoScreen() {
        super(Text.translatable("roleinfo.screen.title"));
    }

    /** Opens the book style screen, or the legacy flat screen when the config option is enabled. */
    public static Screen create() {
        return NoellesRolesConfig.HANDLER.instance().legacyGuiStyle ? new RoleInfoLegacyScreen() : new RoleInfoScreen();
    }

    @Override
    protected void init() {
        super.init();
        String previousSearch = this.searchField != null ? this.searchField.getText() : "";

        clearChildren();
        initializeSelectionForContext();
        this.searchField = new TextFieldWidget(this.textRenderer, 0, 0, 1, 9, Text.translatable("roleinfo.search"));
        this.searchField.setMaxLength(64);
        this.searchField.setDrawsBackground(false);
        this.searchField.setEditableColor(BookPalette.TEXT_HI);
        this.searchField.setPlaceholder(null);
        this.searchField.setText(previousSearch);
        this.searchField.setChangedListener(value -> {
            treeScrollOffset = 0;
            refreshVisibleEntries();
        });
        this.searchField.setVisible(false);
        this.addDrawableChild(this.searchField);

        refreshVisibleEntries();
        ensureSelectedEntryVisible();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, BookPalette.SCRIM);

        Frames frames = getFrames();
        rebuildPaintRows();

        if (searchField != null && (!frames.directoryVisible() || !searchExpanded)) {
            searchField.setVisible(false);
        }

        Text tooltip = null;
        if (frames.directoryVisible()) {
            tooltip = renderDirectory(context, mouseX, mouseY, frames);
        }
        if (frames.readerVisible()) {
            Text readerTip = renderReader(context, mouseX, mouseY, frames);
            if (tooltip == null) {
                tooltip = readerTip;
            }
        }
        if (frames.directoryVisible() && searchExpanded && searchField != null) {
            searchField.render(context, mouseX, mouseY, delta);
        }
        if (tooltip != null) {
            drawBrassTooltip(context, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
    }

    @Override
    protected void applyBlur(float delta) {
    }

    // ==================== Directory ====================

    private Text renderDirectory(DrawContext context, int mouseX, int mouseY, Frames frames) {
        BookRect nav = frames.directory();
        BookPaint.panel(context, nav.x(), nav.y(), nav.width(), nav.height());
        BookPaint.titleBand(context, nav.x(), nav.y(), nav.width());

        boolean closeInBand = frames.closeInBand();
        BookRect close = closeBox(nav);
        BookRect search = searchToggle(nav, closeInBand);
        BookRect collapseAll = collapseAllBox(nav, closeInBand);
        BookRect expandAll = expandAllBox(nav, closeInBand);
        boolean closeHover = closeInBand && close.contains(mouseX, mouseY);
        boolean searchHover = search.contains(mouseX, mouseY);
        boolean collapseHover = collapseAll.contains(mouseX, mouseY);
        boolean expandHover = expandAll.contains(mouseX, mouseY);

        int titleRoom = search.x() - 4 - (nav.x() + 7);
        context.drawText(this.textRenderer,
                BookPaint.ellipsize(this.textRenderer, this.title.getString(), titleRoom),
                nav.x() + 7, nav.y() + 7, BookPalette.TITLE, false);

        if (closeInBand) {
            if (closeHover) {
                BookPaint.iconWash(context, close.x(), close.y(), BookPalette.ICON_HOVER);
            }
            BookPaint.close7(context, nav.right() - 14, nav.y() + 8, closeHover ? BookPalette.COIN : BookPalette.BRASS_HI);
        }
        if (searchHover || searchExpanded) {
            BookPaint.iconWash(context, search.x(), search.y(), searchExpanded ? BookPalette.ICON_ACTIVE : BookPalette.ICON_HOVER);
        }
        BookPaint.magnifier(context, search.x() + 2, search.y() + 2, searchExpanded ? BookPalette.COIN : (searchHover ? BookPalette.BRASS_HI : BookPalette.BRASS));
        if (collapseHover) {
            BookPaint.iconWash(context, collapseAll.x(), collapseAll.y(), BookPalette.ICON_HOVER);
        }
        BookPaint.foldUp(context, collapseAll.x() + 4, collapseAll.y() + 5, collapseHover ? BookPalette.BRASS_HI : BookPalette.BRASS);
        if (expandHover) {
            BookPaint.iconWash(context, expandAll.x(), expandAll.y(), BookPalette.ICON_HOVER);
        }
        BookPaint.foldDown(context, expandAll.x() + 4, expandAll.y() + 5, expandHover ? BookPalette.BRASS_HI : BookPalette.BRASS);

        if (searchExpanded) {
            updateSearchFieldLayout(nav);
            boolean clearHover = !searchField.getText().isEmpty()
                    && isWithin(mouseX, mouseY, nav.right() - 17, nav.y() + 26, 13, 14);
            renderSearchWell(context, nav, clearHover);
        } else if (searchField != null) {
            searchField.setVisible(false);
        }

        BookRect view = directoryViewport(nav, searchExpanded);
        int total = directoryContentHeight;
        boolean scrollable = total > view.height();
        int maxScroll = Math.max(0, total - view.height());
        treeScrollOffset = MathHelper.clamp(treeScrollOffset, 0, maxScroll);
        int rx0 = nav.x() + 4;
        int rx1 = nav.right() - 4 - (scrollable ? 5 : 0);

        if (view.height() > 0) {
            context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
            if (paintRows.isEmpty()) {
                renderNoResults(context, nav, view);
            }
            int origin = view.y() - treeScrollOffset;
            int hovered = rowIndexAt(mouseX, mouseY, view, origin);
            for (int i = 0; i < paintRows.size(); i++) {
                PaintRow row = paintRows.get(i);
                int ry = origin + row.bodyTop();
                if (ry + bodyHeight(row.depth()) <= view.y() || ry >= view.bottom()) {
                    continue;
                }
                renderRow(context, row, hovered == i, rx0, rx1, ry);
            }
            context.disableScissor();
        }

        if (scrollable && view.height() > 0) {
            if (treeScrollOffset > 0) {
                BookPaint.fadeTop(context, view.x(), view.y(), view.right() - 6, BookPalette.BODY);
            }
            if (treeScrollOffset < maxScroll) {
                BookPaint.fadeBottom(context, view.x(), view.bottom(), view.right() - 6, BookPalette.BODY);
            }
            int gx = nav.right() - 8;
            int thumb = BookPaint.thumbSize(view.height(), view.height(), total);
            int thumbY = BookPaint.thumbOffset(view.y(), view.height(), thumb, treeScrollOffset, view.height(), total);
            boolean thumbHot = draggingTreeScrollbar || isWithin(mouseX, mouseY, gx - 1, thumbY, 5, thumb);
            BookPaint.scrollbarDark(context, gx, view.y(), view.height(), thumbY, thumb, thumbHot);
        }

        renderDirectoryFooter(context, nav);

        if (closeHover) {
            return ScreenTexts.BACK;
        }
        if (searchHover) {
            return Text.translatable("roleinfo.search");
        }
        if (expandHover) {
            return Text.translatable("roleinfo.button.expand_all.tooltip");
        }
        if (collapseHover) {
            return Text.translatable("roleinfo.button.collapse_all.tooltip");
        }
        return null;
    }

    private void renderSearchWell(DrawContext context, BookRect nav, boolean clearHover) {
        BookRect well = searchWell(nav);
        if (searchField.isFocused()) {
            BookPaint.roundedOutline(context, well.x() - 1, well.y() - 1, well.width() + 2, well.height() + 2, BookPalette.BRASS_HI);
        }
        context.fill(well.x(), well.y(), well.right(), well.bottom(), BookPalette.WELL);
        context.fill(well.x(), well.y(), well.right(), well.y() + 1, BookPalette.WELL_SHADE);
        context.fill(well.x(), well.bottom() - 1, well.right(), well.bottom(), BookPalette.WELL_LIP);
        BookPaint.magnifier(context, well.x() + 3, well.y() + 3, BookPalette.FAINT);
        if (searchField.getText().isEmpty()) {
            context.drawText(this.textRenderer,
                    BookPaint.ellipsize(this.textRenderer, Text.translatable("roleinfo.search.placeholder").getString(), searchField.getWidth() - 8),
                    searchField.getX() + (searchField.isFocused() ? 7 : 0), searchField.getY(), BookPalette.FAINT, true);
        } else {
            BookPaint.clear5(context, nav.right() - 15, nav.y() + 28, clearHover ? BookPalette.BRASS_HI : BookPalette.FAINT);
        }
    }

    private void renderNoResults(DrawContext context, BookRect nav, BookRect view) {
        int cx = nav.x() + nav.width() / 2;
        Text label = Text.translatable("roleinfo.search.no_results");
        int textX = cx - this.textRenderer.getWidth(label) / 2;
        if (view.height() >= 36) {
            BookPaint.magnifier(context, cx - 4, view.y() + 12, BookPalette.FAINT);
            context.drawText(this.textRenderer, label, textX, view.y() + 26, BookPalette.MUTED, true);
        } else {
            context.drawText(this.textRenderer, label, textX, view.y() + Math.max(0, (view.height() - 9) / 2), BookPalette.MUTED, true);
        }
    }

    private void renderRow(DrawContext context, PaintRow row, boolean hovered, int rx0, int rx1, int ry) {
        int depth = row.depth();
        boolean selected = row.entry().id().equals(selectedEntryId);
        BookPaint.rowState(context, rx0, ry, rx1, bodyHeight(depth), hovered, selected);
        int labelX = rx0 + (depth == 0 ? ROW_LABEL_PAD : (depth == 1 ? 18 : 25));
        boolean star = row.containsCurrent() && (depth == 0 || depth == 1) && !row.expanded();
        if (depth == 0) {
            BookPaint.chevron(context, rx0 + 3, ry + 4, row.expanded(), hovered ? BookPalette.TITLE : BookPalette.BRASS_HI);
            Text label = Text.literal(BookPaint.ellipsize(this.textRenderer, row.label().getString(), row.labelLimit(rx0, rx1)));
            context.drawText(this.textRenderer, label, labelX, ry + 3, hovered ? BookPalette.TITLE : BookPalette.HEADING, false);
            int end = labelX + this.textRenderer.getWidth(label);
            if (row.count() == null) {
                if (star) {
                    BookPaint.star(context, rx1 - 10, ry + 3, BookPalette.COIN);
                }
                return;
            }
            int countX = rx1 - 3 - row.countWidth();
            context.drawText(this.textRenderer, row.count(), countX, ry + 3, BookPalette.FAINT, true);
            int ruleEnd = countX - 4;
            if (star) {
                BookPaint.star(context, countX - 11, ry + 3, BookPalette.COIN);
                ruleEnd = countX - 15;
            }
            if (ruleEnd - (end + 4) >= 12) {
                context.fill(end + 4, ry + 7, ruleEnd, ry + 8, BookPalette.BRASS_LO);
                context.fill(end + 4, ry + 8, ruleEnd, ry + 9, BookPalette.EDGE);
            }
        } else if (depth == 1) {
            BookPaint.chevron(context, rx0 + 10, ry + 3, row.expanded(), hovered ? BookPalette.TEXT_HI : BookPalette.FAINT);
            Text label = Text.literal(BookPaint.ellipsize(this.textRenderer, row.label().getString(), row.labelLimit(rx0, rx1)));
            context.drawText(this.textRenderer, label, labelX, ry + 2, hovered ? BookPalette.TEXT_HI : BookPalette.TEXT, true);
            if (row.count() == null) {
                if (star) {
                    BookPaint.star(context, rx1 - 10, ry + 2, BookPalette.COIN);
                }
                return;
            }
            int countX = rx1 - 3 - row.countWidth();
            context.drawText(this.textRenderer, row.count(), countX, ry + 2, BookPalette.FAINT, true);
            if (star) {
                BookPaint.star(context, countX - 11, ry + 2, BookPalette.COIN);
            }
        } else {
            BookPaint.gem(context, rx0 + 17, ry + 3, row.color(), selected || hovered);
            Text label = Text.literal(BookPaint.ellipsize(this.textRenderer, row.label().getString(), row.labelLimit(rx0, rx1)));
            context.drawText(this.textRenderer, label, labelX, ry + 2, selected || hovered ? BookPalette.TEXT_HI : BookPalette.TEXT, true);
            if (row.isCurrent()) {
                BookPaint.star(context, rx1 - 10, ry + 2, BookPalette.COIN);
            }
        }
    }

    private void renderDirectoryFooter(DrawContext context, BookRect nav) {
        BookPaint.etched(context, nav.x() + 3, nav.right() - 3, nav.bottom() - 21);
        String currentRoleId = getCurrentRoleIdentifier();
        if (currentRoleId == null) {
            return;
        }
        RoleInfoData info = RoleInfoRegistry.get(currentRoleId);
        if (info == null) {
            return;
        }
        int textY = nav.bottom() - 14;
        BookPaint.star(context, nav.x() + 7, textY + 1, BookPalette.COIN);
        Text prefix = Text.translatable("roleinfo.current_role.prefix");
        context.drawText(this.textRenderer, prefix, nav.x() + 16, textY, BookPalette.FAINT, true);
        Integer roleColor = getRoleColorById(currentRoleId);
        int nameColor = roleColor != null ? 0xFF000000 | (roleColor & 0xFFFFFF) : BookPalette.COIN;
        context.drawText(this.textRenderer, RoleInfoRegistry.resolveText(info.nameKey), nav.x() + 16 + this.textRenderer.getWidth(prefix), textY, nameColor, true);
    }

    // ==================== Reader ====================

    private Text renderReader(DrawContext context, int mouseX, int mouseY, Frames frames) {
        BookRect reader = frames.reader();
        BookPaint.panel(context, reader.x(), reader.y(), reader.width(), reader.height());
        BookPaint.titleBand(context, reader.x(), reader.y(), reader.width());

        BookRect close = closeBox(reader);
        BookRect book = bookBox(reader);
        boolean closeHover = close.contains(mouseX, mouseY);
        boolean bookHover = frames.compact() && book.contains(mouseX, mouseY);

        if (closeHover) {
            BookPaint.iconWash(context, close.x(), close.y(), BookPalette.ICON_HOVER);
        }
        BookPaint.close7(context, reader.right() - 14, reader.y() + 8, closeHover ? BookPalette.COIN : BookPalette.BRASS_HI);
        if (frames.compact()) {
            if (bookHover) {
                BookPaint.iconWash(context, book.x(), book.y(), BookPalette.ICON_HOVER);
            }
            BookPaint.bookIcon(context, book.x() + 1, book.y() + 3, bookHover ? BookPalette.BRASS_HI : BookPalette.BRASS);
        }

        RoleInfoData info = getSelectedInfo();
        BookRect sheet = sheet(reader);
        BookRect view = sheetViewport(sheet);
        int measure = sheet.right() - TEXT_PAD - (sheet.x() + TEXT_PAD);
        BookPage.Layout layout = getOrBuildLayout(info, selectedEntryId, Math.max(1, measure));
        int contentHeight = READER_HEADER_HEIGHT + layout.height();
        int maxScroll = Math.max(0, contentHeight - view.height());
        detailScrollOffset = MathHelper.clamp(detailScrollOffset, 0, maxScroll);

        renderReaderBand(context, reader, info, layout);

        context.fill(sheet.x() - 1, sheet.y() - 1, sheet.right() + 1, sheet.y(), BookPalette.EDGE);
        context.fill(sheet.x() - 1, sheet.bottom(), sheet.right() + 1, sheet.bottom() + 1, BookPalette.EDGE);
        context.fill(sheet.x() - 1, sheet.y(), sheet.x(), sheet.bottom(), BookPalette.EDGE);
        context.fill(sheet.right(), sheet.y(), sheet.right() + 1, sheet.bottom(), BookPalette.EDGE);
        BookPaint.paper(context, sheet.x(), sheet.y(), sheet.width(), sheet.height());

        if (info == null) {
            context.drawText(this.textRenderer, Text.translatable("roleinfo.no_info"),
                    sheet.x() + sheet.width() / 2 - this.textRenderer.getWidth(Text.translatable("roleinfo.no_info")) / 2,
                    view.y() + view.height() / 2, BookPalette.INK_MUTED, false);
        } else if (view.height() > 0) {
            int originY = view.y() - detailScrollOffset;
            context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
            renderPageHeader(context, info, sheet, originY);
            renderPageBody(context, layout, sheet, originY + READER_HEADER_HEIGHT, view);
            context.disableScissor();
        }

        if (maxScroll > 0 && view.height() > 0) {
            if (detailScrollOffset > 0) {
                BookPaint.fadeTop(context, view.x(), view.y(), view.right(), BookPalette.PAPER);
            }
            if (detailScrollOffset < maxScroll) {
                BookPaint.fadeBottom(context, view.x(), view.bottom(), view.right(), BookPalette.PAPER);
            }
            int track = view.height() - 4;
            int thumb = BookPaint.thumbSize(track, view.height(), contentHeight);
            int thumbY = BookPaint.thumbOffset(view.y() + 2, track, thumb, detailScrollOffset, view.height(), contentHeight);
            int gx = sheet.right() - 6;
            boolean thumbHot = draggingDetailScrollbar || isWithin(mouseX, mouseY, gx - 1, thumbY, 5, thumb);
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 2.0F);
            BookPaint.scrollbarPaper(context, gx, view.y() + 2, track, thumbY, thumb, thumbHot);
            context.getMatrices().pop();
        }

        if (closeHover) {
            return ScreenTexts.BACK;
        }
        if (bookHover) {
            return Text.translatable("roleinfo.button.directory");
        }
        return null;
    }

    private void renderReaderBand(DrawContext context, BookRect reader, RoleInfoData info, BookPage.Layout layout) {
        boolean running = detailScrollOffset > RUNNING_HEAD_SCROLL;
        int rightLimit = bookBox(reader).x() - 6 - (reader.x() + 7);
        if (running && info != null) {
            Text section = layout.sectionAt(detailScrollOffset + 6 - READER_HEADER_HEIGHT);
            Text title = RoleInfoRegistry.resolveText(info.nameKey);
            String head = BookPaint.ellipsize(this.textRenderer, title.getString(), Math.max(20, rightLimit - 8));
            BookPaint.gem(context, reader.x() + 7, reader.y() + 9, getEntryColor(selectedEntryId, info), false);
            context.drawText(this.textRenderer, head, reader.x() + 15, reader.y() + 7, BookPalette.HEADING, false);
            if (section != null) {
                int end = reader.x() + 15 + this.textRenderer.getWidth(head);
                String separator = " › ";
                context.drawText(this.textRenderer, separator, end, reader.y() + 7, BookPalette.FAINT, true);
                int room = reader.right() - 22 - (end + this.textRenderer.getWidth(separator));
                context.drawText(this.textRenderer,
                        BookPaint.ellipsize(this.textRenderer, section.getString(), Math.max(10, room)),
                        end + this.textRenderer.getWidth(separator), reader.y() + 7, BookPalette.MUTED, true);
            }
        } else {
            String crumb = getCrumb(selectedEntryId, info);
            context.drawText(this.textRenderer, BookPaint.ellipsize(this.textRenderer, crumb, rightLimit),
                    reader.x() + 7, reader.y() + 7, BookPalette.HEADING, false);
        }
    }

    private void renderPageHeader(DrawContext context, RoleInfoData info, BookRect sheet, int originY) {
        TextRenderer font = this.textRenderer;
        int tx0 = sheet.x() + TEXT_PAD;
        int tx1 = sheet.right() - TEXT_PAD;
        Text title = RoleInfoRegistry.resolveText(info.nameKey);
        Text chipLabel = getChipLabel(selectedEntryId, info);
        int titleWidth = font.getWidth(title);
        int chipWidth = chipLabel == null ? 0 : font.getWidth(chipLabel) + 15;
        boolean chipFull = chipLabel != null;
        int available = (tx1 - tx0) - 14;
        if (chipFull && 2 * titleWidth > available - chipWidth - 6) {
            chipFull = false;
            chipWidth = 11;
        }
        int titleRoom = available - (chipLabel != null ? chipWidth + 6 : 0);
        boolean doubleTitle = 2 * titleWidth <= titleRoom;
        String shown = doubleTitle ? title.getString() : BookPaint.ellipsize(font, title.getString(), titleRoom);

        BookPaint.gem2x(context, tx0, originY + 11, getEntryColor(selectedEntryId, info));
        if (doubleTitle) {
            context.getMatrices().push();
            context.getMatrices().translate(tx0 + 14, originY + 8, 0.0F);
            context.getMatrices().scale(2.0F, 2.0F, 1.0F);
            context.drawText(font, shown, 0, 0, BookPalette.INK, false);
            context.getMatrices().pop();
        } else {
            context.drawText(font, shown, tx0 + 14, originY + 12, BookPalette.INK, false);
        }
        if (chipLabel != null) {
            renderChip(context, chipFull ? chipLabel : null, tx1, originY + 11);
        }
        context.fill(tx0, originY + 29, tx1, originY + 30, BookPalette.INK_RULE);
        context.fill(tx0, originY + 31, tx1, originY + 32, BookPalette.INK_RULE_SOFT);
    }

    private void renderChip(DrawContext context, Text label, int right, int y) {
        int width = label == null ? 11 : this.textRenderer.getWidth(label) + 15;
        int x = right - width;
        BookPaint.roundedFill(context, x, y, width, 11, BookPalette.GILT_LEAF);
        BookPaint.roundedOutline(context, x, y, width, 11, BookPalette.BRASS);
        BookPaint.star(context, x + 2, y + 2, BookPalette.GILT_STAR);
        if (label != null) {
            context.drawText(this.textRenderer, label, x + 12, y + 2, BookPalette.GILT_INK, false);
        }
    }

    private void renderPageBody(DrawContext context, BookPage.Layout layout, BookRect sheet, int bodyTop, BookRect view) {
        int tx0 = sheet.x() + TEXT_PAD;
        for (BookPage.Ornament ornament : layout.ornaments()) {
            int top = bodyTop + ornament.y() - 2;
            if (top + ornament.height() + 4 <= view.y() || top >= view.bottom()) {
                continue;
            }
            renderOrnament(context, ornament, tx0, bodyTop);
        }
        for (BookPage.RenderedLine line : layout.lines()) {
            int y = bodyTop + line.y();
            if (y + 9 <= view.y() || y >= view.bottom()) {
                continue;
            }
            context.drawText(this.textRenderer, line.text(), tx0 + line.indent(), y, BookPalette.INK, false);
        }
    }

    private void renderOrnament(DrawContext context, BookPage.Ornament o, int tx0, int bodyTop) {
        int x = tx0 + o.x();
        int y = bodyTop + o.y();
        int cx = x + o.width() / 2;
        switch (o.kind()) {
            case SECTION_MARK -> BookPaint.diamond(context, x, y, o.color());
            case SECTION_RULE -> context.fill(x, y, x + o.width(), y + o.height(), o.color());
            case BULLET -> BookPaint.bullet(context, x, y, o.color());
            case TAILPIECE -> {
                BookPaint.diamond(context, cx - 2, y, o.color());
                context.fill(cx - 8, y + 2, cx - 6, y + 3, o.color());
                context.fill(cx + 7, y + 2, cx + 9, y + 3, o.color());
            }
        }
    }

    // ==================== Reader content ====================

    private BookPage.Layout getOrBuildLayout(RoleInfoData info, String entryId, int measure) {
        if (info == null) {
            return BookPage.Layout.EMPTY;
        }
        if (entryId.equals(cachedLayoutId) && measure == cachedLayoutWidth) {
            return cachedLayout;
        }
        cachedLayout = buildReaderPage(info, entryId, measure);
        cachedLayoutId = entryId;
        cachedLayoutWidth = measure;
        return cachedLayout;
    }

    private BookPage.Layout buildReaderPage(RoleInfoData info, String entryId, int measure) {
        BookPage.Builder builder = BookPage.builder(this.textRenderer, measure);
        int tone = getEntryTone(entryId, info);
        boolean eventPage = isEventPage(entryId);
        builder.section(getDescriptionLabel(), tone);
        builder.paragraph(RoleInfoRegistry.resolveText(info.descriptionKey), 0);
        if (info.winConditionKey != null) {
            builder.section(Text.translatable("roleinfo.win_condition_label"), tone);
            builder.paragraph(RoleInfoRegistry.resolveText(info.winConditionKey), 0);
        }
        if (info.skills != null && !info.skills.isEmpty()) {
            builder.section(Text.translatable(eventPage ? "roleinfo.event_effects_header" : "roleinfo.skills_header"), tone);
            for (RoleInfoData.SkillInfoData skill : info.skills.values()) {
                List<BookText.Cell> line = new ArrayList<>(BookText.cells(RoleInfoRegistry.resolveText(skill.nameKey), BookPalette.INK));
                if (!eventPage && skill.triggerKey != null) {
                    Text trigger = RoleInfoRegistry.getTriggerText(skill);
                    if (!trigger.getString().isBlank()) {
                        line.addAll(BookText.cells("  ", BookPalette.INK_MUTED));
                        line.addAll(BookText.cells(trigger, BookPalette.INK_MUTED));
                    }
                }
                builder.bullet(line);
                Text effect = RoleInfoRegistry.resolveText(skill.effectKey);
                if (!effect.getString().isBlank()) {
                    builder.paragraph(effect, 10);
                }
            }
        }
        return builder.finish();
    }

    /** Gem colour: the role's own colour when it has one, otherwise the page tone. */
    private int getEntryColor(String entryId, RoleInfoData info) {
        Integer roleColor = getRoleColorById(entryId);
        if (roleColor != null) {
            return roleColor & 0xFFFFFF;
        }
        return getEntryTone(entryId, info);
    }

    private int getEntryTone(String entryId, RoleInfoData info) {
        if (entryId == null || entryId.startsWith("category:")) {
            if (RoleInfoRegistry.CATEGORY_KILLER.equals(entryId)) {
                return BookPalette.P_DANGER;
            }
            if (RoleInfoRegistry.CATEGORY_NEUTRAL.equals(entryId)) {
                return BookPalette.P_GOOD;
            }
            if (RoleInfoRegistry.CATEGORY_PASSENGER.equals(entryId)) {
                return BookPalette.P_INFO;
            }
            if (RoleInfoRegistry.CATEGORY_MURDER_MAYHEM.equals(entryId)) {
                return BookPalette.P_ITEM;
            }
            return BookPalette.P_MONEY;
        }
        if (isEventPage(entryId)) {
            return BookPalette.P_ITEM;
        }
        if (RoleInfoRegistry.ROLE_LOOSE_END.equals(entryId)) {
            return BookPalette.P_MONEY;
        }
        return switch (RoleInfoRegistry.getFactionCategoryId(info)) {
            case RoleInfoRegistry.CATEGORY_KILLER -> BookPalette.P_DANGER;
            case RoleInfoRegistry.CATEGORY_NEUTRAL -> BookPalette.P_GOOD;
            default -> BookPalette.P_INFO;
        };
    }

    private Text getChipLabel(String entryId, RoleInfoData info) {
        if (info == null || entryId == null || entryId.startsWith("category:") || isEventPage(entryId)) {
            return null;
        }
        if (info.factionKey == null) {
            return null;
        }
        Text faction = RoleInfoRegistry.resolveText(info.factionKey);
        return faction.getString().isBlank() ? null : faction;
    }

    private String getCrumb(String entryId, RoleInfoData info) {
        String mode = RoleInfoRegistry.resolveText(getModeNameKey()).getString();
        String kind = RoleInfoRegistry.resolveText(getKindKey(entryId, info)).getString();
        return mode + " · " + kind;
    }

    private String getModeNameKey() {
        return switch (getCurrentModeContext().categoryId()) {
            case RoleInfoRegistry.CATEGORY_MURDER_MAYHEM -> "roleinfo.category.murder_mayhem.name";
            case RoleInfoRegistry.CATEGORY_LOOSE_ENDS -> "roleinfo.category.loose_ends.name";
            case RoleInfoRegistry.CATEGORY_DEATH_ARENA -> "roleinfo.category.death_arena.name";
            default -> "roleinfo.category.classic.name";
        };
    }

    private String getKindKey(String entryId, RoleInfoData info) {
        if (entryId == null || !entryId.startsWith("category:")) {
            if (isEventPage(entryId)) {
                return "roleinfo.chip.event";
            }
            return "roleinfo.chip.role";
        }
        return switch (entryId) {
            case RoleInfoRegistry.CATEGORY_PASSENGER, RoleInfoRegistry.CATEGORY_KILLER, RoleInfoRegistry.CATEGORY_NEUTRAL -> "roleinfo.chip.faction";
            default -> "roleinfo.chip.mode";
        };
    }

    // ==================== Layout ====================

    private Frames getFrames() {
        int margin = Math.min(8, Math.max(0, Math.min(this.width, this.height) / 4));
        int maxSidebar = (this.width - 144) / 2 - margin - 8;
        boolean compact = maxSidebar < 64;
        int sidebarWidth = Math.min(Math.max(0, this.width - margin * 2), Math.min(176, Math.max(64, this.width / 4 - 8)));
        if (!compact) {
            sidebarWidth = Math.min(sidebarWidth, maxSidebar);
        }
        int articleX = compact ? margin : margin + sidebarWidth + SPREAD_GAP;
        int articleWidth = compact ? this.width - margin * 2 : Math.min(READER_WIDTH, this.width - articleX - margin);
        BookRect directory = new BookRect(margin, margin, sidebarWidth, this.height - margin * 2);
        BookRect article = new BookRect(articleX, margin, articleWidth, this.height - margin * 2);
        if (!compact) {
            int shift = Math.max(0, (this.width - (article.x() + article.width()) - directory.x()) / 2);
            directory = directory.shifted(shift, 0);
            article = article.shifted(shift, 0);
            return new Frames(directory, true, article, true, false, false);
        }
        if (compactReaderOpen) {
            BookRect reader = new BookRect(article.x(), margin, article.width(), this.height - margin * 2);
            return new Frames(new BookRect(0, 0, 0, 0), false, reader, true, false, true);
        }
        BookRect full = new BookRect(margin, margin, this.width - margin * 2, this.height - margin * 2);
        return new Frames(full, true, new BookRect(0, 0, 0, 0), false, true, true);
    }

    private static BookRect closeBox(BookRect panel) {
        return new BookRect(panel.right() - 18, panel.y() + 4, 14, 14);
    }

    private static BookRect searchToggle(BookRect nav, boolean closeInBand) {
        return new BookRect(nav.right() - 18 - (closeInBand ? 14 : 0), nav.y() + 4, 14, 14);
    }

    private static BookRect collapseAllBox(BookRect nav, boolean closeInBand) {
        BookRect search = searchToggle(nav, closeInBand);
        return new BookRect(search.x() - 14, search.y(), 14, 14);
    }

    private static BookRect expandAllBox(BookRect nav, boolean closeInBand) {
        BookRect collapse = collapseAllBox(nav, closeInBand);
        return new BookRect(collapse.x() - 14, collapse.y(), 14, 14);
    }

    private static BookRect bookBox(BookRect reader) {
        return new BookRect(reader.right() - 32, reader.y() + 4, 14, 14);
    }

    private static BookRect searchWell(BookRect nav) {
        return new BookRect(nav.x() + 6, nav.y() + 24, Math.max(0, nav.width() - 12), 14);
    }

    private static BookRect directoryViewport(BookRect nav, boolean searchOpen) {
        int top = nav.y() + (searchOpen ? SEARCH_BOTTOM : BAND_BOTTOM);
        return new BookRect(nav.x() + 3, top, Math.max(0, nav.width() - 6), Math.max(0, nav.bottom() - FOOTER_HEIGHT - top));
    }

    private static BookRect sheet(BookRect reader) {
        return new BookRect(reader.x() + 5, reader.y() + 22, Math.max(0, reader.width() - 10), Math.max(0, reader.height() - 27));
    }

    private static BookRect sheetViewport(BookRect sheet) {
        return new BookRect(sheet.x() + 2, sheet.y() + 2, Math.max(0, sheet.width() - 4), Math.max(0, sheet.height() - 4));
    }

    private static BookRect directoryScrollHit(BookRect view) {
        return new BookRect(view.right() - 5, view.y(), 5, view.height());
    }

    private static BookRect readerScrollHit(BookRect sheet) {
        return new BookRect(sheet.right() - 7, sheet.y() + 2, 5, Math.max(0, sheet.height() - 4));
    }

    private void updateSearchFieldLayout(BookRect nav) {
        BookRect well = searchWell(nav);
        searchField.setX(nav.x() + 21);
        searchField.setY(well.y() + 3);
        searchField.setWidth(Math.max(1, nav.width() - 39));
        searchField.setHeight(9);
        searchField.setVisible(true);
    }

    private int bodyHeight(int depth) {
        return depth == 0 ? ROOT_HEIGHT : ROW_HEIGHT;
    }

    // ==================== Rows ====================

    private void rebuildPaintRows() {
        paintRows.clear();
        String currentRoleId = getCurrentRoleIdentifier();
        int cursor = 0;
        for (int i = 0; i < visibleEntries.size(); i++) {
            TreeEntry entry = visibleEntries.get(i);
            int depth = entry.depth;
            if (depth == 0 && !paintRows.isEmpty()) {
                cursor += ROOT_GAP;
            }
            int bodyTop = cursor;
            cursor += bodyHeight(depth);
            Text count = null;
            int countWidth = 0;
            if (depth <= 1) {
                int childCount = subtreeCounts.getOrDefault(entry.id, 0);
                if (childCount > 0) {
                    count = Text.literal(String.valueOf(childCount));
                    countWidth = this.textRenderer.getWidth(count);
                }
            }
            boolean isCurrent = entry.id.equals(currentRoleId);
            boolean containsCurrent = isCurrent || subtreeIds.getOrDefault(entry.id, Set.of()).contains(currentRoleId);
            Integer roleColor = depth == 2 ? getRoleColorById(entry.id) : null;
            int color = roleColor != null ? roleColor & 0xFFFFFF : 0xFFFFFF;
            paintRows.add(new PaintRow(entry, depth, bodyTop, entry.label, count, countWidth, isCategoryExpanded(entry.id), containsCurrent, isCurrent, color));
        }
        directoryContentHeight = cursor;
    }

    private boolean isCategoryExpanded(String id) {
        return getEffectiveExpandedCategoryIds().contains(id);
    }

    private int rowIndexAt(double mouseX, double mouseY, BookRect view, int origin) {
        for (int i = 0; i < paintRows.size(); i++) {
            PaintRow row = paintRows.get(i);
            int ry = origin + row.bodyTop();
            if (isWithin(mouseX, mouseY, view.x(), ry, view.width() - 4, bodyHeight(row.depth()))) {
                return i;
            }
        }
        return -1;
    }

    private void refreshVisibleEntries() {
        visibleEntries.clear();
        String filter = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        updateSearchExpandedCategories(filter);
        for (TreeEntry entry : buildTreeEntries(filter)) {
            if (entry.depth == 0 || isParentExpanded(entry)) {
                visibleEntries.add(entry);
            }
        }
        ensureSelectedEntryExists();
    }

    private List<TreeEntry> buildTreeEntries(String filter) {
        TreeNode root = buildTreeModel();
        List<TreeEntry> entries = new ArrayList<>();
        for (TreeNode child : root.children) {
            appendVisibleEntries(child, 0, filter, entries);
        }
        return entries;
    }

    private void appendVisibleEntries(TreeNode node, int depth, String filter, List<TreeEntry> entries) {
        if (!node.matches(filter)) {
            return;
        }
        entries.add(new TreeEntry(node.id, node.label, depth, node.isCategory, node.parentId));
        if (!node.isCategory || !getEffectiveExpandedCategoryIds().contains(node.id)) {
            return;
        }
        for (TreeNode child : node.children) {
            appendVisibleEntries(child, depth + 1, filter, entries);
        }
    }

    private TreeNode buildTreeModel() {
        subtreeCounts.clear();
        subtreeIds.clear();
        Map<String, List<RoleEntry>> rolesByFaction = new LinkedHashMap<>();
        rolesByFaction.put(RoleInfoRegistry.CATEGORY_PASSENGER, new ArrayList<>());
        rolesByFaction.put(RoleInfoRegistry.CATEGORY_KILLER, new ArrayList<>());
        rolesByFaction.put(RoleInfoRegistry.CATEGORY_NEUTRAL, new ArrayList<>());

        for (Map.Entry<String, RoleInfoData> entry : RoleInfoRegistry.getAll().entrySet()) {
            if (RoleInfoRegistry.ROLE_LOOSE_END.equals(entry.getKey())) {
                continue;
            }
            RoleInfoData info = entry.getValue();
            String factionCategoryId = RoleInfoRegistry.getFactionCategoryId(info);
            rolesByFaction.computeIfAbsent(factionCategoryId, ignored -> new ArrayList<>())
                    .add(new RoleEntry(entry.getKey(), RoleInfoRegistry.resolveText(info.nameKey), entry.getKey().equals(selectedEntryId)));
        }

        for (List<RoleEntry> roles : rolesByFaction.values()) {
            roles.sort((left, right) -> {
                if (left.currentRole != right.currentRole) {
                    return left.currentRole ? -1 : 1;
                }
                return left.label.getString().compareToIgnoreCase(right.label.getString());
            });
        }

        TreeNode root = new TreeNode("category:root", Text.empty(), true, null);
        TreeNode classic = new TreeNode(RoleInfoRegistry.CATEGORY_CLASSIC, RoleInfoRegistry.resolveText("tr:roleinfo.category.classic.name"), true, null);
        TreeNode passenger = new TreeNode(RoleInfoRegistry.CATEGORY_PASSENGER, RoleInfoRegistry.resolveText("tr:roleinfo.category.passenger.name"), true, RoleInfoRegistry.CATEGORY_CLASSIC);
        TreeNode killer = new TreeNode(RoleInfoRegistry.CATEGORY_KILLER, RoleInfoRegistry.resolveText("tr:roleinfo.category.killer.name"), true, RoleInfoRegistry.CATEGORY_CLASSIC);
        TreeNode neutral = new TreeNode(RoleInfoRegistry.CATEGORY_NEUTRAL, RoleInfoRegistry.resolveText("tr:roleinfo.category.neutral.name"), true, RoleInfoRegistry.CATEGORY_CLASSIC);
        TreeNode looseEnds = new TreeNode(RoleInfoRegistry.CATEGORY_LOOSE_ENDS, RoleInfoRegistry.resolveText("tr:roleinfo.category.loose_ends.name"), true, null);
        TreeNode deathArena = new TreeNode(RoleInfoRegistry.CATEGORY_DEATH_ARENA, RoleInfoRegistry.resolveText("tr:roleinfo.category.death_arena.name"), true, null);

        appendRoleChildren(passenger, rolesByFaction.get(RoleInfoRegistry.CATEGORY_PASSENGER));
        appendRoleChildren(killer, rolesByFaction.get(RoleInfoRegistry.CATEGORY_KILLER));
        appendRoleChildren(neutral, rolesByFaction.get(RoleInfoRegistry.CATEGORY_NEUTRAL));

        classic.children.add(passenger);
        classic.children.add(killer);
        classic.children.add(neutral);

        // 迷乱谋杀模式已废弃：不再构建该分类节点，游戏内说明里看不到它的模式介绍与事件条目。
        // 恢复方式：把下面两段（murderMayhem 节点 + 迷雾事件子节点）与 root.children.add(murderMayhem) 加回来。

        RoleInfoData looseEndInfo = RoleInfoRegistry.get(RoleInfoRegistry.ROLE_LOOSE_END);
        if (looseEndInfo != null) {
            looseEnds.children.add(new TreeNode(
                    RoleInfoRegistry.ROLE_LOOSE_END,
                    RoleInfoRegistry.resolveText(looseEndInfo.nameKey),
                    false,
                    RoleInfoRegistry.CATEGORY_LOOSE_ENDS
            ));
        }

        root.children.add(classic);
        root.children.add(looseEnds);
        root.children.add(deathArena);

        for (TreeNode child : root.children) {
            indexSubtree(child);
        }
        return root;
    }

    private void indexSubtree(TreeNode node) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(node.id);
        int count = 0;
        for (TreeNode child : node.children) {
            indexSubtree(child);
            ids.addAll(subtreeIds.getOrDefault(child.id, Set.of()));
            count += child.children.isEmpty() ? 1 : Math.max(1, subtreeCounts.getOrDefault(child.id, 0));
        }
        subtreeIds.put(node.id, ids);
        subtreeCounts.put(node.id, count);
    }

    private void appendRoleChildren(TreeNode parent, List<RoleEntry> roles) {
        if (roles == null) {
            return;
        }
        for (RoleEntry role : roles) {
            parent.children.add(new TreeNode(role.id, role.label, false, parent.id));
        }
    }

    private void updateSearchExpandedCategories(String filter) {
        searchExpandedCategoryIds.clear();
        if (filter == null || filter.isBlank()) {
            return;
        }
        collectSearchExpandedCategories(buildTreeModel(), filter, searchExpandedCategoryIds);
    }

    private boolean collectSearchExpandedCategories(TreeNode node, String filter, Set<String> categoriesToExpand) {
        boolean descendantRoleMatched = false;
        for (TreeNode child : node.children) {
            boolean childMatched = collectSearchExpandedCategories(child, filter, categoriesToExpand);
            descendantRoleMatched |= childMatched;
        }
        boolean selfMatched = !node.isCategory && node.matches(filter);
        if (descendantRoleMatched && node.isCategory) {
            categoriesToExpand.add(node.id);
        }
        return selfMatched || descendantRoleMatched;
    }

    private Set<String> getEffectiveExpandedCategoryIds() {
        if (searchExpandedCategoryIds.isEmpty()) {
            return expandedCategoryIds;
        }
        Set<String> effective = new LinkedHashSet<>(expandedCategoryIds);
        effective.addAll(searchExpandedCategoryIds);
        return effective;
    }

    private boolean isParentExpanded(TreeEntry entry) {
        if (entry.parentId == null) {
            return true;
        }
        return getEffectiveExpandedCategoryIds().contains(entry.parentId);
    }

    private void expandAllCategories() {
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_CLASSIC);
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_MURDER_MAYHEM);
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_LOOSE_ENDS);
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_DEATH_ARENA);
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_PASSENGER);
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_KILLER);
        expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_NEUTRAL);
        refreshVisibleEntries();
        ensureSelectedEntryVisible();
    }

    private void collapseAllCategories() {
        expandedCategoryIds.clear();
        refreshVisibleEntries();
        ensureSelectedEntryVisible();
    }

    private void ensureSelectedEntryExists() {
        boolean exists = visibleEntries.stream().anyMatch(entry -> entry.id.equals(selectedEntryId));
        if (!exists) {
            selectedEntryId = getCurrentModeContext().categoryId();
        }
        boolean stillExists = visibleEntries.stream().anyMatch(entry -> entry.id.equals(selectedEntryId));
        if (!stillExists && !visibleEntries.isEmpty()) {
            selectedEntryId = visibleEntries.get(0).id;
        }
    }

    private void ensureSelectedEntryVisible() {
        Frames frames = getFrames();
        BookRect view = directoryViewport(frames.directory(), searchExpanded);
        rebuildPaintRows();
        int maxScroll = Math.max(0, directoryContentHeight - view.height());
        for (PaintRow row : paintRows) {
            if (!row.entry().id().equals(selectedEntryId)) {
                continue;
            }
            int rowTop = row.bodyTop();
            int rowBottom = rowTop + bodyHeight(row.depth());
            if (rowTop < treeScrollOffset) {
                treeScrollOffset = Math.max(0, rowTop);
            } else if (rowBottom > treeScrollOffset + view.height()) {
                treeScrollOffset = Math.min(maxScroll, rowBottom - view.height());
            }
            return;
        }
    }

    // ==================== Selection context ====================

    private RoleInfoData getSelectedInfo() {
        return RoleInfoRegistry.getPage(selectedEntryId);
    }

    private Text getDescriptionLabel() {
        if (isModeCategory(selectedEntryId)) {
            return Text.translatable("roleinfo.mode_description_label");
        }
        if (isEventPage(selectedEntryId)) {
            return Text.translatable("roleinfo.event_description_label");
        }
        if (RoleInfoRegistry.CATEGORY_PASSENGER.equals(selectedEntryId)
                || RoleInfoRegistry.CATEGORY_KILLER.equals(selectedEntryId)
                || RoleInfoRegistry.CATEGORY_NEUTRAL.equals(selectedEntryId)) {
            return Text.translatable("roleinfo.faction_description_label");
        }
        return Text.translatable("roleinfo.role_description_label");
    }

    private boolean isModeCategory(String entryId) {
        return RoleInfoRegistry.CATEGORY_CLASSIC.equals(entryId)
                || RoleInfoRegistry.CATEGORY_MURDER_MAYHEM.equals(entryId)
                || RoleInfoRegistry.CATEGORY_LOOSE_ENDS.equals(entryId)
                || RoleInfoRegistry.CATEGORY_DEATH_ARENA.equals(entryId)
                || RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW.equals(entryId)
                || RoleInfoRegistry.CATEGORY_LOOSE_ENDS_OVERVIEW.equals(entryId)
                || RoleInfoRegistry.CATEGORY_DEATH_ARENA_OVERVIEW.equals(entryId);
    }

    private boolean isEventPage(String entryId) {
        return entryId != null && entryId.startsWith("event:");
    }

    private void initializeSelectionForContext() {
        ModeContext modeContext = getCurrentModeContext();
        String rootId = modeContext.categoryId();
        if (!modeContext.recognized()) {
            expandedCategoryIds.clear();
        }
        if (RoleInfoRegistry.CATEGORY_MURDER_MAYHEM.equals(rootId)) {
            selectedEntryId = modeContext.overviewPageId();
            expandedCategoryIds.add(rootId);
            return;
        }
        String currentRoleId = getCurrentRoleIdentifier();
        if (currentRoleId == null || RoleInfoRegistry.get(currentRoleId) == null) {
            selectedEntryId = rootId;
            return;
        }
        if (RoleInfoRegistry.CATEGORY_LOOSE_ENDS.equals(rootId) && !RoleInfoRegistry.ROLE_LOOSE_END.equals(currentRoleId)) {
            selectedEntryId = rootId;
            return;
        }
        if (RoleInfoRegistry.CATEGORY_DEATH_ARENA.equals(rootId)) {
            selectedEntryId = rootId;
            return;
        }
        selectedEntryId = currentRoleId;
        expandedCategoryIds.add(rootId);
        if (modeContext.recognized()
                && (RoleInfoRegistry.CATEGORY_CLASSIC.equals(rootId) || RoleInfoRegistry.CATEGORY_MURDER_MAYHEM.equals(rootId))) {
            expandedCategoryIds.add(RoleInfoRegistry.CATEGORY_CLASSIC);
            RoleInfoData currentRoleInfo = RoleInfoRegistry.get(currentRoleId);
            expandedCategoryIds.add(RoleInfoRegistry.getFactionCategoryId(currentRoleInfo));
        }
    }

    private ModeContext getCurrentModeContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return new ModeContext(WatheGameModes.MURDER_ID, RoleInfoRegistry.CATEGORY_CLASSIC, RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW, false);
        }
        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
        if (NoellesrolesClient.isDeathArenaActiveForClientPlayer()) {
            return new ModeContext(WatheGameModes.LOOSE_ENDS_ID, RoleInfoRegistry.CATEGORY_DEATH_ARENA, RoleInfoRegistry.CATEGORY_DEATH_ARENA_OVERVIEW, true);
        }
        GameMode gameMode = gwc.getGameMode();
        Identifier gameModeId = gameMode != null ? gameMode.identifier : null;
        // 迷乱谋杀模式已废弃：不再为它构造说明页上下文（其分类节点已从树里移除），
        // 该模式下会走下面的兜底分支回到经典谋杀说明。
        if (WatheGameModes.MURDER_ID.equals(gameModeId)) {
            return new ModeContext(gameModeId, RoleInfoRegistry.CATEGORY_CLASSIC, RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW, true);
        }
        if (WatheGameModes.LOOSE_ENDS_ID.equals(gameModeId)) {
            return new ModeContext(gameModeId, RoleInfoRegistry.CATEGORY_LOOSE_ENDS, RoleInfoRegistry.CATEGORY_LOOSE_ENDS_OVERVIEW, true);
        }
        return new ModeContext(WatheGameModes.MURDER_ID, RoleInfoRegistry.CATEGORY_CLASSIC, RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW, false);
    }

    private String getCurrentMurderMayhemOverviewPageId(MinecraftClient client) {
        if (client.player == null) {
            return RoleInfoRegistry.CATEGORY_MURDER_MAYHEM;
        }
        MurderMayhemWorldComponent component = MurderMayhemWorldComponent.KEY.get(client.player.getWorld());
        Identifier currentEventId = component.getCurrentEventId();
        if (FogOfWarMurderMayhemEvent.ID.equals(currentEventId)) {
            return RoleInfoRegistry.EVENT_MURDER_MAYHEM_FOG_OF_WAR;
        }
        return RoleInfoRegistry.CATEGORY_MURDER_MAYHEM;
    }

    private String getCurrentRoleIdentifier() {
        return RoleInfoLookup.currentRoleId();
    }

    private Integer getRoleColorById(String roleId) {
        return RoleInfoLookup.roleColor(roleId);
    }

    // ==================== Input ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Frames frames = getFrames();
            if (searchExpanded && searchField != null && searchField.isFocused() && frames.directoryVisible()) {
                BookRect nav = frames.directory();
                boolean onWell = searchWell(nav).contains(mouseX, mouseY)
                        || isWithin(mouseX, mouseY, nav.right() - 17, nav.y() + 26, 13, 14);
                if (!onWell) {
                    this.setFocused(null);
                    searchField.setFocused(false);
                }
            }
            if (frames.directoryVisible()) {
                BookRect nav = frames.directory();
                if (frames.closeInBand() && closeBox(nav).contains(mouseX, mouseY)) {
                    this.close();
                    return true;
                }
                if (searchToggle(nav, frames.closeInBand()).contains(mouseX, mouseY)) {
                    searchExpanded = !searchExpanded;
                    if (searchExpanded) {
                        this.setFocused(searchField);
                    } else {
                        this.setFocused(null);
                        if (searchField != null) {
                            searchField.setFocused(false);
                            searchField.setVisible(false);
                        }
                    }
                    return true;
                }
                if (collapseAllBox(nav, frames.closeInBand()).contains(mouseX, mouseY)) {
                    collapseAllCategories();
                    return true;
                }
                if (expandAllBox(nav, frames.closeInBand()).contains(mouseX, mouseY)) {
                    expandAllCategories();
                    return true;
                }
                if (searchExpanded && searchField != null && !searchField.getText().isEmpty()
                        && isWithin(mouseX, mouseY, nav.right() - 17, nav.y() + 26, 13, 14)) {
                    searchField.setText("");
                    return true;
                }
                BookRect view = directoryViewport(nav, searchExpanded);
                int total = directoryContentHeight;
                boolean scrollable = total > view.height();
                int maxScroll = Math.max(0, total - view.height());
                if (scrollable && directoryScrollHit(view).contains(mouseX, mouseY)) {
                    int track = view.height();
                    int thumb = BookPaint.thumbSize(track, view.height(), total);
                    int thumbY = BookPaint.thumbOffset(view.y(), track, thumb, treeScrollOffset, view.height(), total);
                    if (isWithin(mouseX, mouseY, view.right() - 5, thumbY, 5, thumb)) {
                        draggingTreeScrollbar = true;
                        treeDragOffset = (int) mouseY - thumbY;
                    } else {
                        treeScrollOffset = scrollFromThumb((int) mouseY - thumb / 2, view.y(), track, thumb, view.height(), total);
                    }
                    return true;
                }
                int origin = view.y() - treeScrollOffset;
                int index = rowIndexAt(mouseX, mouseY, view, origin);
                if (index >= 0) {
                    TreeEntry entry = paintRows.get(index).entry();
                    selectedEntryId = entry.id;
                    detailScrollOffset = 0;
                    if (entry.isCategory) {
                        if (expandedCategoryIds.contains(entry.id)) {
                            expandedCategoryIds.remove(entry.id);
                        } else {
                            expandedCategoryIds.add(entry.id);
                        }
                        refreshVisibleEntries();
                    } else if (frames.compact()) {
                        compactReaderOpen = true;
                        this.setFocused(null);
                        if (searchField != null) {
                            searchField.setFocused(false);
                        }
                    }
                    return true;
                }
            }
            if (frames.readerVisible()) {
                BookRect reader = frames.reader();
                if (closeBox(reader).contains(mouseX, mouseY)) {
                    this.close();
                    return true;
                }
                if (frames.compact() && bookBox(reader).contains(mouseX, mouseY)) {
                    compactReaderOpen = false;
                    return true;
                }
                BookRect sheet = sheet(reader);
                BookRect view = sheetViewport(sheet);
                RoleInfoData info = getSelectedInfo();
                int measure = Math.max(1, sheet.right() - TEXT_PAD - (sheet.x() + TEXT_PAD));
                BookPage.Layout layout = getOrBuildLayout(info, selectedEntryId, measure);
                int total = READER_HEADER_HEIGHT + layout.height();
                int maxScroll = Math.max(0, total - view.height());
                if (maxScroll > 0 && readerScrollHit(sheet).contains(mouseX, mouseY)) {
                    int track = view.height() - 4;
                    int thumb = BookPaint.thumbSize(track, view.height(), total);
                    int thumbY = BookPaint.thumbOffset(view.y() + 2, track, thumb, detailScrollOffset, view.height(), total);
                    if (isWithin(mouseX, mouseY, sheet.right() - 7, thumbY, 5, thumb)) {
                        draggingDetailScrollbar = true;
                        detailDragOffset = (int) mouseY - thumbY;
                    } else {
                        detailScrollOffset = scrollFromThumb((int) mouseY - thumb / 2, view.y() + 2, track, thumb, view.height(), total);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingTreeScrollbar = false;
            draggingDetailScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        Frames frames = getFrames();
        if (button == 0 && draggingTreeScrollbar && frames.directoryVisible()) {
            BookRect view = directoryViewport(frames.directory(), searchExpanded);
            int total = directoryContentHeight;
            int track = view.height();
            int thumb = BookPaint.thumbSize(track, view.height(), total);
            treeScrollOffset = scrollFromThumb((int) mouseY - treeDragOffset, view.y(), track, thumb, view.height(), total);
            return true;
        }
        if (button == 0 && draggingDetailScrollbar && frames.readerVisible()) {
            BookRect view = sheetViewport(sheet(frames.reader()));
            RoleInfoData info = getSelectedInfo();
            int measure = Math.max(1, frames.reader().width() - 10 - TEXT_PAD * 2);
            BookPage.Layout layout = getOrBuildLayout(info, selectedEntryId, measure);
            int total = READER_HEADER_HEIGHT + layout.height();
            int track = view.height() - 4;
            int thumb = BookPaint.thumbSize(track, view.height(), total);
            detailScrollOffset = scrollFromThumb((int) mouseY - detailDragOffset, view.y() + 2, track, thumb, view.height(), total);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Frames frames = getFrames();
        if (frames.directoryVisible()) {
            BookRect nav = frames.directory();
            BookRect view = directoryViewport(nav, searchExpanded);
            int maxScroll = Math.max(0, directoryContentHeight - view.height());
            if (isWithin(mouseX, mouseY, nav.x(), view.y(), nav.width(), view.height()) && maxScroll > 0) {
                treeScrollOffset = MathHelper.clamp(treeScrollOffset - (int) (verticalAmount * 26), 0, maxScroll);
                return true;
            }
        }
        if (frames.readerVisible()) {
            BookRect reader = frames.reader();
            BookRect sheet = sheet(reader);
            BookRect view = sheetViewport(sheet);
            int measure = Math.max(1, sheet.right() - TEXT_PAD - (sheet.x() + TEXT_PAD));
            RoleInfoData info = getSelectedInfo();
            BookPage.Layout layout = getOrBuildLayout(info, selectedEntryId, measure);
            int maxScroll = Math.max(0, READER_HEADER_HEIGHT + layout.height() - view.height());
            if (isWithin(mouseX, mouseY, sheet.x(), view.y(), sheet.width(), view.height()) && maxScroll > 0) {
                detailScrollOffset = MathHelper.clamp(detailScrollOffset - (int) (verticalAmount * 24), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NoellesrolesClient.roleInfoBind != null
                && NoellesrolesClient.roleInfoBind.matchesKey(keyCode, scanCode)
                && !isEditingTextField()) {
            NoellesrolesClient.markRoleInfoKeyHandled();
            this.close();
            return true;
        }
        if (NoellesrolesClient.assistInterfaceBind != null
                && NoellesrolesClient.assistInterfaceBind.matchesKey(keyCode, scanCode)
                && !isEditingTextField()
                && this.client != null
                && this.client.player != null) {
            GameWorldComponent gwc = GameWorldComponent.KEY.get(this.client.player.getWorld());
            if (gwc.isRunning() && SpectatorStateHelper.isRealSpectator(this.client.player)) {
                NoellesrolesClient.markAssistInterfaceKeyHandled();
                this.client.setScreen(new SpectatorAssistPanelScreen());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isEditingTextField() {
        return this.getFocused() instanceof TextFieldWidget
                || (this.searchField != null && this.searchField.isFocused());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    // ==================== Helpers ====================

    private int scrollFromThumb(int thumbY, int trackTop, int track, int thumb, int viewport, int total) {
        int travel = Math.max(1, track - thumb);
        int span = Math.max(1, total - viewport);
        int clamped = MathHelper.clamp(thumbY, trackTop, trackTop + track - thumb);
        return (clamped - trackTop) * span / travel;
    }

    private void drawBrassTooltip(DrawContext context, Text tip, double mouseX, double mouseY) {
        List<Text> lines = List.of(tip);
        int tipWidth = BookPaint.tooltipWidth(this.textRenderer, lines);
        int tipHeight = BookPaint.tooltipHeight(lines.size());
        int x = MathHelper.clamp((int) mouseX + 12, 4, this.width - 4 - tipWidth);
        int y = MathHelper.clamp((int) mouseY - 12, 4, this.height - 4 - tipHeight);
        BookPaint.tooltip(context, this.textRenderer, lines, x, y, tipWidth);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Frames(
            BookRect directory,
            boolean directoryVisible,
            BookRect reader,
            boolean readerVisible,
            boolean closeInBand,
            boolean compact
    ) {
    }

    private record TreeEntry(String id, Text label, int depth, boolean isCategory, String parentId) {
    }

    private record RoleEntry(String id, Text label, boolean currentRole) {
    }

    private record ModeContext(Identifier gameModeId, String categoryId, String overviewPageId, boolean recognized) {
    }

    private record PaintRow(
            TreeEntry entry,
            int depth,
            int bodyTop,
            Text label,
            Text count,
            int countWidth,
            boolean expanded,
            boolean containsCurrent,
            boolean isCurrent,
            int color
    ) {
        int labelLimit(int rx0, int rx1) {
            int labelX = rx0 + (this.depth == 0 ? ROW_LABEL_PAD : (this.depth == 1 ? 18 : 25));
            if (this.count == null) {
                return Math.max(10, rx1 - labelX - 14);
            }
            return Math.max(10, rx1 - 3 - this.countWidth - 14 - labelX);
        }
    }

    private static final class TreeNode {
        final String id;
        final Text label;
        final boolean isCategory;
        final String parentId;
        final List<TreeNode> children = new ArrayList<>();

        TreeNode(String id, Text label, boolean isCategory, String parentId) {
            this.id = id;
            this.label = label;
            this.isCategory = isCategory;
            this.parentId = parentId;
        }

        boolean matches(String filter) {
            if (filter == null || filter.isBlank()) {
                return true;
            }
            String normalized = label.getString().toLowerCase(Locale.ROOT);
            if (normalized.contains(filter)) {
                return true;
            }
            for (TreeNode child : children) {
                if (child.matches(filter)) {
                    return true;
                }
            }
            return false;
        }
    }
}

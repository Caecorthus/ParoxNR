package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.client.roleinfo.RoleInfoData;
import org.agmas.noellesroles.client.roleinfo.RoleInfoLookup;
import org.agmas.noellesroles.client.roleinfo.RoleInfoRegistry;
import org.agmas.noellesroles.client.screen.book.BookPaint;
import org.agmas.noellesroles.client.screen.book.BookPalette;
import org.agmas.noellesroles.client.screen.book.BookRect;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.packet.AttendantEditInfoC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 乘务登记表面板（书卷风格）：左侧乘客目录，右侧羊皮纸登记页；
 * 登记页含见面/存活三态选择器与职业选择子页。
 */
public class AttendantRegisterScreen extends Screen {
    private static final int SPREAD_GAP = 8;
    private static final int READER_WIDTH = 284;
    private static final int BAND_BOTTOM = 23;
    private static final int TEXT_PAD = 11;
    private static final int DIRECTORY_ROW = 20;
    private static final int FILTER_BAR = 18;
    private static final int AVATAR = 16;
    private static final int DETAIL_HEADER = 46;
    private static final int LINE_HEIGHT = 12;
    private static final int SECTION_GAP = 14;
    private static final int SEGMENT_WIDTH = 52;
    private static final int SEGMENT_HEIGHT = 16;
    private static final int SEGMENT_GAP = 4;
    private static final int PICKER_ROW = 16;

    private static final Text UNKNOWN_TEXT = Text.translatable("screen.attendant.value.unknown");

    private final ClientPlayerEntity player;
    private UUID selected;
    private boolean rolePickerOpen;
    private boolean compactReaderOpen;
    private int directoryScrollOffset;
    private int detailScrollOffset;
    private int pickerScrollOffset;
    private boolean draggingDirectoryScrollbar;
    private boolean draggingDetailScrollbar;
    private int directoryDragOffset;
    private int detailDragOffset;
    private Byte pendingMet;
    private Byte pendingAlive;
    private String pendingRoleId;
    private List<AttendantUi.FactionGroup> factionGroups = List.of();
    private final Set<Faction> collapsedFactions = EnumSet.noneOf(Faction.class);
    private int pickerLayoutHeight;
    private int metFilter;
    private int aliveFilter;
    private Text roleWarning = null;

    public AttendantRegisterScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.attendant.title"));
        this.player = player;
    }

    /** Opens the book style screen, or the legacy flat screen when the config option is enabled. */
    public static Screen create(ClientPlayerEntity player) {
        return NoellesRolesConfig.HANDLER.instance().legacyGuiStyle
                ? new AttendantRegisterLegacyScreen(player)
                : new AttendantRegisterScreen(player);
    }

    private AttendantPlayerComponent component() {
        return AttendantPlayerComponent.KEY.get(this.player);
    }

    private boolean canStayOpen() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        return gameWorld.isRole(this.player, Noellesroles.ATTENDANT)
                && GameFunctions.isPlayerPlayingAndAlive(this.player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(this.player);
    }

    @Override
    protected void init() {
        super.init();
        if (!canStayOpen()) {
            this.close();
            return;
        }
        this.factionGroups = AttendantUi.groupedRoles(GameWorldComponent.KEY.get(this.player.getWorld()));
        List<AttendantPlayerComponent.Entry> list = entries();
        if (list.isEmpty()) {
            this.selected = null;
        } else if (this.selected == null || component().getEntry(this.selected) == null) {
            this.selected = list.get(0).getUuid();
        }
    }

    private List<AttendantPlayerComponent.Entry> entries() {
        List<AttendantPlayerComponent.Entry> list = new ArrayList<>(component().getEntryList());
        list.sort(Comparator
                .comparingInt((AttendantPlayerComponent.Entry entry) ->
                        entry.getRoomIndex() < 0 ? Integer.MAX_VALUE : entry.getRoomIndex())
                .thenComparing(AttendantPlayerComponent.Entry::getName));
        return list;
    }

    private List<AttendantPlayerComponent.Entry> displayEntries() {
        List<AttendantPlayerComponent.Entry> list = entries();
        list.removeIf(entry -> !AttendantUi.matchesFilter(entry, this.metFilter, this.aliveFilter));
        return list;
    }

    private AttendantPlayerComponent.Entry currentEntry() {
        return this.selected == null ? null : component().getEntry(this.selected);
    }

    // ==================== 登记值（含本地待同步覆盖） ====================

    private byte effectiveMet() {
        AttendantPlayerComponent.Entry entry = currentEntry();
        byte synced = entry == null ? AttendantPlayerComponent.UNKNOWN : entry.getMet();
        if (this.pendingMet != null) {
            if (this.pendingMet == synced) {
                this.pendingMet = null;
            } else {
                return this.pendingMet;
            }
        }
        return synced;
    }

    private byte effectiveAlive() {
        AttendantPlayerComponent.Entry entry = currentEntry();
        byte synced = entry == null ? AttendantPlayerComponent.UNKNOWN : entry.getAlive();
        if (this.pendingAlive != null) {
            if (this.pendingAlive == synced) {
                this.pendingAlive = null;
            } else {
                return this.pendingAlive;
            }
        }
        return synced;
    }

    private String effectiveRoleId() {
        AttendantPlayerComponent.Entry entry = currentEntry();
        String synced = entry == null ? "" : entry.getRoleId();
        if (this.pendingRoleId != null) {
            if (this.pendingRoleId.equals(synced)) {
                this.pendingRoleId = null;
            } else {
                return this.pendingRoleId;
            }
        }
        return synced;
    }

    private void setField(int field, String value) {
        if (this.selected == null) {
            return;
        }
        ClientPlayNetworking.send(new AttendantEditInfoC2SPacket(this.selected, field, value));
        switch (field) {
            case 0 -> this.pendingMet = Byte.parseByte(value);
            case 1 -> this.pendingAlive = Byte.parseByte(value);
            case 2 -> this.pendingRoleId = value;
            default -> {
            }
        }
    }

    private void selectEntry(UUID uuid, boolean compact) {
        this.selected = uuid;
        this.detailScrollOffset = 0;
        this.pickerScrollOffset = 0;
        this.rolePickerOpen = false;
        this.pendingMet = null;
        this.pendingAlive = null;
        this.pendingRoleId = null;
        this.roleWarning = null;
        if (compact) {
            this.compactReaderOpen = true;
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, BookPalette.SCRIM);

        if (this.selected == null) {
            List<AttendantPlayerComponent.Entry> list = entries();
            if (!list.isEmpty()) {
                this.selected = list.get(0).getUuid();
            }
        }

        Frames frames = getFrames();
        List<Text> tooltip = null;
        if (frames.directoryVisible()) {
            tooltip = renderDirectory(context, mouseX, mouseY, frames);
        }
        if (frames.readerVisible()) {
            List<Text> readerTip = renderReader(context, mouseX, mouseY, frames);
            if (tooltip == null) {
                tooltip = readerTip;
            }
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

    // ==================== 目录 ====================

    private List<Text> renderDirectory(DrawContext context, int mouseX, int mouseY, Frames frames) {
        BookRect nav = frames.directory();
        BookPaint.panel(context, nav.x(), nav.y(), nav.width(), nav.height());
        BookPaint.titleBand(context, nav.x(), nav.y(), nav.width());

        boolean closeInBand = frames.closeInBand();
        BookRect close = closeBox(nav);
        boolean closeHover = closeInBand && close.contains(mouseX, mouseY);

        BookPaint.gem(context, nav.x() + 7, nav.y() + 9, Noellesroles.ATTENDANT.color(), false);
        int titleRoom = nav.width() - 15 - (closeInBand ? 18 : 6);
        context.drawText(this.textRenderer,
                BookPaint.ellipsize(this.textRenderer, this.title.getString(), Math.max(10, titleRoom)),
                nav.x() + 15, nav.y() + 7, BookPalette.TITLE, false);
        if (closeInBand) {
            if (closeHover) {
                BookPaint.iconWash(context, close.x(), close.y(), BookPalette.ICON_HOVER);
            }
            BookPaint.close7(context, nav.right() - 14, nav.y() + 8, closeHover ? BookPalette.COIN : BookPalette.BRASS_HI);
        }

        BookRect metChip = filterMetBox(nav);
        BookRect aliveChip = filterAliveBox(nav);
        renderFilterChip(context, metChip,
                Text.translatable("screen.attendant.filter.met_label", AttendantUi.metFilterText(this.metFilter)),
                metChip.contains(mouseX, mouseY));
        renderFilterChip(context, aliveChip,
                Text.translatable("screen.attendant.filter.alive_label", AttendantUi.aliveFilterText(this.aliveFilter)),
                aliveChip.contains(mouseX, mouseY));

        List<AttendantPlayerComponent.Entry> list = displayEntries();
        BookRect view = directoryViewport(nav);
        int total = list.size() * DIRECTORY_ROW;
        boolean scrollable = total > view.height();
        int maxScroll = Math.max(0, total - view.height());
        this.directoryScrollOffset = MathHelper.clamp(this.directoryScrollOffset, 0, maxScroll);
        int rx0 = nav.x() + 4;
        int rx1 = nav.right() - 4 - (scrollable ? 5 : 0);
        int hoveredRow = -1;

        if (view.height() > 0) {
            context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
            if (list.isEmpty()) {
                Text empty = Text.translatable("screen.attendant.empty");
                context.drawText(this.textRenderer, empty, nav.x() + nav.width() / 2 - this.textRenderer.getWidth(empty) / 2,
                        view.y() + view.height() / 2 - 4, BookPalette.FAINT, false);
            }
            int origin = view.y() - this.directoryScrollOffset;
            for (int i = 0; i < list.size(); i++) {
                int ry = origin + i * DIRECTORY_ROW;
                if (ry + DIRECTORY_ROW <= view.y() || ry >= view.bottom()) {
                    continue;
                }
                boolean hovered = isWithin(mouseX, mouseY, view.x(), view.y(), view.width(), view.height())
                        && isWithin(mouseX, mouseY, rx0, ry, Math.max(0, rx1 - rx0), DIRECTORY_ROW);
                renderDirectoryRow(context, list.get(i), rx0, rx1, ry, hovered, list.get(i).getUuid().equals(this.selected));
                if (hovered) {
                    hoveredRow = i;
                }
            }
            context.disableScissor();
        }

        if (scrollable && view.height() > 0) {
            if (this.directoryScrollOffset > 0) {
                BookPaint.fadeTop(context, view.x(), view.y(), view.right() - 6, BookPalette.BODY);
            }
            if (this.directoryScrollOffset < maxScroll) {
                BookPaint.fadeBottom(context, view.x(), view.bottom(), view.right() - 6, BookPalette.BODY);
            }
            int gx = nav.right() - 8;
            int thumb = BookPaint.thumbSize(view.height(), view.height(), total);
            int thumbY = BookPaint.thumbOffset(view.y(), view.height(), thumb, this.directoryScrollOffset, view.height(), total);
            boolean thumbHot = this.draggingDirectoryScrollbar || isWithin(mouseX, mouseY, gx - 1, thumbY, 5, thumb);
            BookPaint.scrollbarDark(context, gx, view.y(), view.height(), thumbY, thumb, thumbHot);
        }

        if (hoveredRow >= 0) {
            return buildEntryTooltip(list.get(hoveredRow));
        }
        if (closeHover) {
            return List.of(ScreenTexts.BACK);
        }
        return null;
    }

    private void renderDirectoryRow(DrawContext context, AttendantPlayerComponent.Entry entry, int rx0, int rx1,
                                    int y, boolean hovered, boolean selected) {
        BookPaint.rowState(context, rx0, y, rx1, DIRECTORY_ROW - 2, hovered, selected);
        renderAvatar(context, rx0 + 3, y + 1, entry.getUuid());

        Text room = AttendantUi.roomText(entry.getRoomName());
        int roomWidth = this.textRenderer.getWidth(room);
        int nameRoom = Math.max(10, (rx1 - 6) - (rx0 + 24) - roomWidth - 6);
        String name = BookPaint.ellipsize(this.textRenderer, AttendantUi.displayName(entry.getName()), nameRoom);
        context.drawText(this.textRenderer, name, rx0 + 24, y + 6,
                selected ? BookPalette.TITLE : BookPalette.TEXT, false);
        context.drawText(this.textRenderer, room, rx1 - 4 - roomWidth, y + 6, BookPalette.FAINT, false);
    }

    private List<Text> buildEntryTooltip(AttendantPlayerComponent.Entry entry) {
        return List.of(
                Text.translatable("screen.attendant.tooltip.id", AttendantUi.displayName(entry.getName())),
                Text.translatable("screen.attendant.tooltip.met", AttendantUi.triStateText(entry.getMet())),
                Text.translatable("screen.attendant.tooltip.alive", AttendantUi.triStateText(entry.getAlive())),
                Text.translatable("screen.attendant.tooltip.role", AttendantUi.roleText(entry.getRoleId())),
                Text.translatable("screen.attendant.tooltip.room", AttendantUi.roomText(entry.getRoomName()))
        );
    }

    // ==================== Reader ====================

    private List<Text> renderReader(DrawContext context, int mouseX, int mouseY, Frames frames) {
        BookRect reader = frames.reader();
        BookPaint.panel(context, reader.x(), reader.y(), reader.width(), reader.height());
        BookPaint.titleBand(context, reader.x(), reader.y(), reader.width());

        BookRect close = closeBox(reader);
        BookRect back = backBox(reader);
        boolean closeHover = close.contains(mouseX, mouseY);
        boolean backAvailable = this.rolePickerOpen || frames.compact();
        boolean backHover = backAvailable && back.contains(mouseX, mouseY);

        AttendantPlayerComponent.Entry entry = currentEntry();
        String crumbLabel = Text.translatable(this.rolePickerOpen ? "screen.attendant.role.pick" : "screen.attendant.crumb.detail").getString();
        String name = entry == null ? "" : AttendantUi.displayName(entry.getName());
        String crumb = name.isEmpty() ? crumbLabel : crumbLabel + " · " + name;
        int crumbRoom = back.x() - 4 - (reader.x() + 7);
        context.drawText(this.textRenderer, BookPaint.ellipsize(this.textRenderer, crumb, Math.max(10, crumbRoom)),
                reader.x() + 7, reader.y() + 7, BookPalette.HEADING, false);

        if (closeHover) {
            BookPaint.iconWash(context, close.x(), close.y(), BookPalette.ICON_HOVER);
        }
        BookPaint.close7(context, reader.right() - 14, reader.y() + 8, closeHover ? BookPalette.COIN : BookPalette.BRASS_HI);
        if (backAvailable) {
            if (backHover) {
                BookPaint.iconWash(context, back.x(), back.y(), BookPalette.ICON_HOVER);
            }
            if (this.rolePickerOpen) {
                BookPaint.chevronLeft(context, back.x() + 4, back.y() + 5, backHover ? BookPalette.BRASS_HI : BookPalette.BRASS);
            } else {
                BookPaint.bookIcon(context, back.x() + 1, back.y() + 3, backHover ? BookPalette.BRASS_HI : BookPalette.BRASS);
            }
        }

        BookRect sheet = sheet(reader);
        BookRect view = sheetViewport(sheet);

        context.fill(sheet.x() - 1, sheet.y() - 1, sheet.right() + 1, sheet.y(), BookPalette.EDGE);
        context.fill(sheet.x() - 1, sheet.bottom(), sheet.right() + 1, sheet.bottom() + 1, BookPalette.EDGE);
        context.fill(sheet.x() - 1, sheet.y(), sheet.x(), sheet.bottom(), BookPalette.EDGE);
        context.fill(sheet.right(), sheet.y(), sheet.right() + 1, sheet.bottom(), BookPalette.EDGE);
        BookPaint.paper(context, sheet.x(), sheet.y(), sheet.width(), sheet.height());

        Text hoveredRoleTip = null;
        if (entry == null) {
            Text empty = Text.translatable("screen.attendant.empty");
            context.drawText(this.textRenderer, empty,
                    sheet.x() + sheet.width() / 2 - this.textRenderer.getWidth(empty) / 2,
                    view.y() + view.height() / 2 - 4, BookPalette.INK_MUTED, false);
        } else if (view.height() > 0) {
            int contentHeight = this.rolePickerOpen ? pickerContentHeight() : detailContentHeight(sheet);
            int maxScroll = Math.max(0, contentHeight - view.height());
            int scroll = MathHelper.clamp(this.rolePickerOpen ? this.pickerScrollOffset : this.detailScrollOffset, 0, maxScroll);
            if (this.rolePickerOpen) {
                this.pickerScrollOffset = scroll;
            } else {
                this.detailScrollOffset = scroll;
            }
            int originY = view.y() - scroll;
            Role hoveredRole = null;
            boolean mouseInView = isWithin(mouseX, mouseY, view.x(), view.y(), view.width(), view.height());
            context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
            if (this.rolePickerOpen) {
                hoveredRole = renderPicker(context, sheet, view, originY, mouseInView ? mouseX : Double.NEGATIVE_INFINITY, mouseInView ? mouseY : Double.NEGATIVE_INFINITY);
            } else {
                renderDetail(context, sheet, entry, originY, mouseInView ? mouseX : Double.NEGATIVE_INFINITY, mouseInView ? mouseY : Double.NEGATIVE_INFINITY);
            }
            context.disableScissor();
            if (maxScroll > 0) {
                if (scroll > 0) {
                    BookPaint.fadeTop(context, view.x(), view.y(), view.right(), BookPalette.PAPER);
                }
                if (scroll < maxScroll) {
                    BookPaint.fadeBottom(context, view.x(), view.bottom(), view.right(), BookPalette.PAPER);
                }
                int track = view.height() - 4;
                int thumb = BookPaint.thumbSize(track, view.height(), contentHeight);
                int thumbY = BookPaint.thumbOffset(view.y() + 2, track, thumb, scroll, view.height(), contentHeight);
                int gx = sheet.right() - 6;
                boolean thumbHot = this.draggingDetailScrollbar || isWithin(mouseX, mouseY, gx - 1, thumbY, 5, thumb);
                context.getMatrices().push();
                context.getMatrices().translate(0.0F, 0.0F, 2.0F);
                BookPaint.scrollbarPaper(context, gx, view.y() + 2, track, thumbY, thumb, thumbHot);
                context.getMatrices().pop();
            }
            if (this.roleWarning != null) {
                drawWarningBanner(context, sheet);
            }
            if (hoveredRole != null) {
                hoveredRoleTip = roleSummaryTip(hoveredRole);
            }
        }

        if (hoveredRoleTip != null) {
            return List.of(hoveredRoleTip);
        }
        if (closeHover) {
            return List.of(ScreenTexts.BACK);
        }
        if (backHover) {
            return List.of(Text.translatable(this.rolePickerOpen ? "screen.attendant.button.back" : "screen.attendant.button.directory"));
        }
        return null;
    }

    // ==================== 登记页内容 ====================

    private record DetailLayout(BookRect metSegment, BookRect aliveSegment, BookRect roleRow, int height) {
    }

    private DetailLayout detailLayout(BookRect sheet, int originY) {
        int tx0 = sheet.x() + TEXT_PAD;
        int tx1 = sheet.right() - TEXT_PAD;
        int y = originY + DETAIL_HEADER;
        y += LINE_HEIGHT + 4;
        y += LINE_HEIGHT * 2;
        y += SECTION_GAP;
        y += LINE_HEIGHT + 4;
        BookRect metSegment = new BookRect(tx0, y, SEGMENT_WIDTH * 3 + SEGMENT_GAP * 2, SEGMENT_HEIGHT);
        y += SEGMENT_HEIGHT + SECTION_GAP;
        y += LINE_HEIGHT + 4;
        BookRect aliveSegment = new BookRect(tx0, y, SEGMENT_WIDTH * 3 + SEGMENT_GAP * 2, SEGMENT_HEIGHT);
        y += SEGMENT_HEIGHT + SECTION_GAP;
        y += LINE_HEIGHT + 4;
        BookRect roleRow = new BookRect(tx0, y, Math.max(0, tx1 - tx0), 18);
        y += 18 + SECTION_GAP;
        y += LINE_HEIGHT;
        return new DetailLayout(metSegment, aliveSegment, roleRow, y - originY + 6);
    }

    private int detailContentHeight(BookRect sheet) {
        return detailLayout(sheet, 0).height();
    }

    private void renderDetail(DrawContext context, BookRect sheet, AttendantPlayerComponent.Entry entry,
                              int originY, double mouseX, double mouseY) {
        TextRenderer font = this.textRenderer;
        int tx0 = sheet.x() + TEXT_PAD;
        int tx1 = sheet.right() - TEXT_PAD;
        String name = AttendantUi.displayName(entry.getName());

        renderAvatar2x(context, tx0, originY + 3, entry.getUuid());
        Text chipMet = Text.translatable("screen.attendant.button.met", AttendantUi.triStateText(effectiveMet()));
        Text chipAlive = Text.translatable("screen.attendant.button.alive", AttendantUi.triStateText(effectiveAlive()));
        int chipRoom = tx1 - (tx0 + 42);
        int nameRoom = Math.max(20, chipRoom - Math.max(font.getWidth(chipMet), font.getWidth(chipAlive)) - 20);
        String shownName = BookPaint.ellipsize(font, name, nameRoom);
        if (2 * font.getWidth(shownName) <= nameRoom) {
            context.getMatrices().push();
            context.getMatrices().translate(tx0 + 42, originY + 9, 0.0F);
            context.getMatrices().scale(2.0F, 2.0F, 1.0F);
            context.drawText(font, shownName, 0, 0, BookPalette.INK, false);
            context.getMatrices().pop();
        } else {
            context.drawText(font, shownName, tx0 + 42, originY + 12, BookPalette.INK, false);
        }
        renderChip(context, chipMet, AttendantUi.triStateColor(effectiveMet()), tx1, originY + 7);
        renderChip(context, chipAlive, AttendantUi.triStateColor(effectiveAlive()), tx1, originY + 22);
        context.fill(tx0, originY + 40, tx1, originY + 41, BookPalette.INK_RULE);
        context.fill(tx0, originY + 42, tx1, originY + 43, BookPalette.INK_RULE_SOFT);

        DetailLayout layout = detailLayout(sheet, originY);
        int y = originY + DETAIL_HEADER;

        drawSectionTitle(context, tx0, tx1, y, Text.translatable("screen.attendant.section.register"));
        y += LINE_HEIGHT + 4;
        context.drawText(font, Text.translatable("screen.attendant.line.id", name), tx0 + 9, y, BookPalette.INK, false);
        y += LINE_HEIGHT;
        context.drawText(font, Text.translatable("screen.attendant.line.room", AttendantUi.roomText(entry.getRoomName())),
                tx0 + 9, y, BookPalette.INK, false);
        y += LINE_HEIGHT + SECTION_GAP;

        drawSectionTitle(context, tx0, tx1, y, Text.translatable("screen.attendant.section.met"));
        drawSegment(context, layout.metSegment(), effectiveMet(), mouseX, mouseY);
        y += LINE_HEIGHT + 4 + SEGMENT_HEIGHT + SECTION_GAP;

        drawSectionTitle(context, tx0, tx1, y, Text.translatable("screen.attendant.section.alive"));
        drawSegment(context, layout.aliveSegment(), effectiveAlive(), mouseX, mouseY);
        y += LINE_HEIGHT + 4 + SEGMENT_HEIGHT + SECTION_GAP;

        drawSectionTitle(context, tx0, tx1, y, Text.translatable("screen.attendant.section.role"));
        drawRoleRow(context, layout.roleRow(), mouseX, mouseY);
        y += LINE_HEIGHT + 4 + 18 + SECTION_GAP;

        context.drawText(font, BookPaint.ellipsize(font, Text.translatable("screen.attendant.hint.register").getString(),
                Math.max(10, tx1 - tx0)), tx0, y, BookPalette.INK_FAINT, false);
    }

    private void drawSectionTitle(DrawContext context, int tx0, int tx1, int y, Text label) {
        BookPaint.diamond(context, tx0, y + 2, BookPalette.GILT_STAR);
        context.drawText(this.textRenderer, label, tx0 + 9, y, BookPalette.INK, false);
        int ruleX = tx0 + 9 + this.textRenderer.getWidth(label) + 5;
        if (tx1 - ruleX >= 16) {
            context.fill(ruleX, y + 4, tx1, y + 5, BookPalette.INK_RULE_SOFT);
        }
    }

    private void drawSegment(DrawContext context, BookRect segment, byte value, double mouseX, double mouseY) {
        byte[] states = {AttendantPlayerComponent.UNKNOWN, AttendantPlayerComponent.YES, AttendantPlayerComponent.NO};
        for (int i = 0; i < states.length; i++) {
            BookRect box = segmentBox(segment, i);
            boolean selected = value == states[i];
            boolean hovered = box.contains(mouseX, mouseY);
            int background = selected ? BookPalette.GILT_LEAF : (hovered ? BookPalette.PAPER_GLINT : BookPalette.PAPER_DEEP);
            BookPaint.roundedFill(context, box.x(), box.y(), box.width(), box.height(), background);
            BookPaint.roundedOutline(context, box.x(), box.y(), box.width(), box.height(),
                    selected ? BookPalette.BRASS : (hovered ? BookPalette.BRASS_LO : BookPalette.INK_RULE_SOFT));
            Text label = AttendantUi.triStateText(states[i]);
            int color = selected ? BookPalette.GILT_INK : (hovered ? BookPalette.INK : BookPalette.INK_MUTED);
            context.drawText(this.textRenderer, label,
                    box.x() + (box.width() - this.textRenderer.getWidth(label)) / 2,
                    box.y() + (box.height() - 8) / 2, color, false);
        }
    }

    private void drawRoleRow(DrawContext context, BookRect row, double mouseX, double mouseY) {
        boolean hovered = row.contains(mouseX, mouseY);
        BookPaint.roundedFill(context, row.x(), row.y(), row.width(), row.height(),
                hovered ? BookPalette.PAPER_GLINT : BookPalette.PAPER_DEEP);
        BookPaint.roundedOutline(context, row.x(), row.y(), row.width(), row.height(),
                hovered ? BookPalette.BRASS_LO : BookPalette.INK_RULE_SOFT);

        String roleId = effectiveRoleId();
        Integer roleColor = RoleInfoLookup.roleColor(roleId);
        BookPaint.gem(context, row.x() + 5, row.y() + 6,
                roleColor == null ? BookPalette.BRASS : roleColor & 0xFFFFFF, hovered);
        Text value = AttendantUi.roleText(roleId);
        context.drawText(this.textRenderer, value, row.x() + 14, row.y() + 5,
                roleColor == null ? BookPalette.INK_MUTED : BookPalette.INK, false);

        Text pick = Text.translatable("screen.attendant.role.pick");
        int pickX = row.right() - 14 - this.textRenderer.getWidth(pick);
        context.drawText(this.textRenderer, pick, pickX, row.y() + 5, BookPalette.INK_MUTED, false);
        BookPaint.chevron(context, pickX + this.textRenderer.getWidth(pick) + 3, row.y() + 6, false,
                hovered ? BookPalette.GILT_STAR : BookPalette.INK_FAINT);
    }

    private void renderChip(DrawContext context, Text label, int starColor, int right, int y) {
        int width = this.textRenderer.getWidth(label) + 15;
        int x = right - width;
        BookPaint.roundedFill(context, x, y, width, 11, BookPalette.GILT_LEAF);
        BookPaint.roundedOutline(context, x, y, width, 11, BookPalette.BRASS);
        BookPaint.star(context, x + 2, y + 2, starColor);
        context.drawText(this.textRenderer, label, x + 12, y + 2, BookPalette.GILT_INK, false);
    }

    // ==================== 职业选择子页 ====================

    private record PickerItem(Role role, Faction faction, boolean header, int y) {
    }

    /** 职业选择页行布局：未知项 + 各阵营（可折叠）标题与角色行。 */
    private List<PickerItem> pickerItems() {
        List<PickerItem> items = new ArrayList<>();
        int y = 4;
        items.add(new PickerItem(null, null, false, y));
        y += PICKER_ROW;
        for (AttendantUi.FactionGroup group : this.factionGroups) {
            items.add(new PickerItem(null, group.faction(), true, y));
            y += PICKER_ROW;
            if (!this.collapsedFactions.contains(group.faction())) {
                for (Role role : group.roles()) {
                    items.add(new PickerItem(role, null, false, y));
                    y += PICKER_ROW;
                }
            }
        }
        this.pickerLayoutHeight = y + 6;
        return items;
    }

    private int pickerContentHeight() {
        pickerItems();
        return this.pickerLayoutHeight;
    }

    private Role renderPicker(DrawContext context, BookRect sheet, BookRect view, int originY,
                              double mouseX, double mouseY) {
        TextRenderer font = this.textRenderer;
        int tx0 = sheet.x() + TEXT_PAD;
        int tx1 = sheet.right() - TEXT_PAD;
        String current = effectiveRoleId();
        Role hoveredRole = null;

        for (PickerItem item : pickerItems()) {
            int y = originY + item.y();
            if (y + PICKER_ROW <= view.y() || y >= view.bottom()) {
                continue;
            }
            boolean hoveredRow = isWithin(mouseX, mouseY, tx0, y, Math.max(0, tx1 - tx0), PICKER_ROW);
            if (item.header()) {
                if (hoveredRow) {
                    context.fill(tx0, y, tx1, y + PICKER_ROW - 2, BookPalette.PAPER_GLINT);
                }
                boolean expanded = !this.collapsedFactions.contains(item.faction());
                BookPaint.chevron(context, tx0 + 3, y + 6, expanded,
                        hoveredRow ? BookPalette.GILT_STAR : BookPalette.INK_MUTED);
                Text label = AttendantUi.factionText(item.faction());
                context.drawText(font, label, tx0 + 13, y + 4, hoveredRow ? BookPalette.INK : BookPalette.INK_MUTED, false);
                int ruleX = tx0 + 13 + font.getWidth(label) + 5;
                if (tx1 - ruleX >= 10) {
                    context.fill(ruleX, y + 8, tx1, y + 9, BookPalette.INK_RULE_SOFT);
                }
                continue;
            }
            Role role = item.role();
            String roleId = role == null ? "" : role.identifier().toString();
            boolean selected = roleId.equals(current);
            if (selected) {
                context.fill(tx0, y, tx1, y + PICKER_ROW - 2, BookPalette.PAPER_DEEP);
                context.fill(tx0, y, tx0 + 2, y + PICKER_ROW - 2, BookPalette.GILT_STAR);
            } else if (hoveredRow) {
                context.fill(tx0, y, tx1, y + PICKER_ROW - 2, BookPalette.PAPER_GLINT);
            }
            if (role == null) {
                BookPaint.bookIcon(context, tx0 + 5, y + 4, hoveredRow || selected ? BookPalette.GILT_STAR : BookPalette.BRASS_LO);
            } else {
                Integer color = RoleInfoLookup.roleColor(roleId);
                BookPaint.gem(context, tx0 + 5, y + 5, color == null ? BookPalette.BRASS : color & 0xFFFFFF, hoveredRow || selected);
            }
            Text label = role == null ? UNKNOWN_TEXT
                    : Text.translatable("announcement.role." + role.identifier().getPath());
            int textColor = selected ? BookPalette.INK : (hoveredRow ? BookPalette.INK : BookPalette.INK_MUTED);
            context.drawText(font, BookPaint.ellipsize(font, label.getString(), Math.max(10, tx1 - (tx0 + 16))),
                    tx0 + 16, y + 4, textColor, false);
            if (hoveredRow && role != null) {
                hoveredRole = role;
            }
        }
        return hoveredRole;
    }

    private Text roleSummaryTip(Role role) {
        RoleInfoData info = RoleInfoRegistry.get(role.identifier().toString());
        if (info == null || info.summaryKey == null) {
            return null;
        }
        Text summary = RoleInfoRegistry.resolveText(info.summaryKey);
        return summary.getString().isBlank() ? null : summary;
    }

    /** 固定在羊皮纸底部的职业重复提示条（action bar 会被书卷界面遮住，面板内也要可见）。 */
    private void drawWarningBanner(DrawContext context, BookRect sheet) {
        int stripHeight = 14;
        int stripX = sheet.x() + 3;
        int stripY = sheet.bottom() - stripHeight - 3;
        int stripWidth = Math.max(0, sheet.width() - 6);
        BookPaint.roundedFill(context, stripX, stripY, stripWidth, stripHeight, BookPalette.PAPER_DEEP);
        BookPaint.roundedOutline(context, stripX, stripY, stripWidth, stripHeight, BookPalette.BRASS_LO);
        String text = BookPaint.ellipsize(this.textRenderer, this.roleWarning.getString(), Math.max(10, stripWidth - 8));
        context.drawText(this.textRenderer, text,
                stripX + (stripWidth - this.textRenderer.getWidth(text)) / 2, stripY + 4,
                BookPalette.P_DANGER, false);
    }

    // ==================== 布局 ====================

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
        if (this.compactReaderOpen) {
            BookRect reader = new BookRect(article.x(), margin, article.width(), this.height - margin * 2);
            return new Frames(new BookRect(0, 0, 0, 0), false, reader, true, false, true);
        }
        BookRect full = new BookRect(margin, margin, this.width - margin * 2, this.height - margin * 2);
        return new Frames(full, true, new BookRect(0, 0, 0, 0), false, true, true);
    }

    private static BookRect closeBox(BookRect panel) {
        return new BookRect(panel.right() - 18, panel.y() + 4, 14, 14);
    }

    private static BookRect backBox(BookRect reader) {
        return new BookRect(reader.right() - 32, reader.y() + 4, 14, 14);
    }

    private static BookRect filterMetBox(BookRect nav) {
        int width = Math.max(30, (nav.width() - 18) / 2);
        return new BookRect(nav.x() + 6, nav.y() + BAND_BOTTOM + 1, width, 14);
    }

    private static BookRect filterAliveBox(BookRect nav) {
        BookRect met = filterMetBox(nav);
        return new BookRect(met.right() + 6, met.y(), met.width(), met.height());
    }

    private void renderFilterChip(DrawContext context, BookRect box, Text label, boolean hovered) {
        BookPaint.roundedFill(context, box.x(), box.y(), box.width(), box.height(),
                hovered ? BookPalette.SELECT : BookPalette.WELL);
        BookPaint.roundedOutline(context, box.x(), box.y(), box.width(), box.height(),
                hovered ? BookPalette.BRASS : BookPalette.BRASS_LO);
        String text = BookPaint.ellipsize(this.textRenderer, label.getString(), Math.max(10, box.width() - 8));
        context.drawText(this.textRenderer, text, box.x() + (box.width() - this.textRenderer.getWidth(text)) / 2,
                box.y() + (box.height() - 8) / 2, hovered ? BookPalette.TEXT_HI : BookPalette.MUTED, false);
    }

    private static BookRect directoryViewport(BookRect nav) {
        int top = nav.y() + BAND_BOTTOM + FILTER_BAR;
        return new BookRect(nav.x() + 3, top, Math.max(0, nav.width() - 6), Math.max(0, nav.bottom() - top - 5));
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

    private static BookRect segmentBox(BookRect segment, int index) {
        return new BookRect(segment.x() + index * (SEGMENT_WIDTH + SEGMENT_GAP), segment.y(), SEGMENT_WIDTH, SEGMENT_HEIGHT);
    }

    private void renderAvatar(DrawContext context, int x, int y, UUID uuid) {
        BookPaint.roundedFill(context, x - 1, y - 1, AVATAR + 2, AVATAR + 2, BookPalette.WELL);
        PlayerSkinDrawer.draw(context, RoleScreenHelper.getPlayerSkinTextures(uuid).texture(), x, y, AVATAR);
        BookPaint.roundedOutline(context, x - 1, y - 1, AVATAR + 2, AVATAR + 2, BookPalette.BRASS_LO);
    }

    private void renderAvatar2x(DrawContext context, int x, int y, UUID uuid) {
        BookPaint.roundedFill(context, x, y, 34, 34, BookPalette.WELL);
        context.getMatrices().push();
        context.getMatrices().translate(x + 1, y + 1, 0.0F);
        context.getMatrices().scale(2.0F, 2.0F, 1.0F);
        PlayerSkinDrawer.draw(context, RoleScreenHelper.getPlayerSkinTextures(uuid).texture(), 0, 0, AVATAR);
        context.getMatrices().pop();
        BookPaint.roundedOutline(context, x, y, 34, 34, BookPalette.BRASS_LO);
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Frames frames = getFrames();
            if (frames.directoryVisible()) {
                BookRect nav = frames.directory();
                if (frames.closeInBand() && closeBox(nav).contains(mouseX, mouseY)) {
                    this.close();
                    return true;
                }
                if (filterMetBox(nav).contains(mouseX, mouseY)) {
                    this.metFilter = (this.metFilter + 1) % 3;
                    this.directoryScrollOffset = 0;
                    return true;
                }
                if (filterAliveBox(nav).contains(mouseX, mouseY)) {
                    this.aliveFilter = (this.aliveFilter + 1) % 3;
                    this.directoryScrollOffset = 0;
                    return true;
                }
                BookRect view = directoryViewport(nav);
                int total = displayEntries().size() * DIRECTORY_ROW;
                int maxScroll = Math.max(0, total - view.height());
                if (total > view.height() && directoryScrollHit(view).contains(mouseX, mouseY)) {
                    int track = view.height();
                    int thumb = BookPaint.thumbSize(track, view.height(), total);
                    int thumbY = BookPaint.thumbOffset(view.y(), track, thumb, this.directoryScrollOffset, view.height(), total);
                    if (isWithin(mouseX, mouseY, view.right() - 5, thumbY, 5, thumb)) {
                        this.draggingDirectoryScrollbar = true;
                        this.directoryDragOffset = (int) mouseY - thumbY;
                    } else {
                        this.directoryScrollOffset = scrollFromThumb((int) mouseY - thumb / 2, view.y(), track, thumb, view.height(), total);
                    }
                    return true;
                }
                int origin = view.y() - this.directoryScrollOffset;
                int index = rowIndexAt(mouseX, mouseY, view, origin);
                if (index >= 0) {
                    selectEntry(displayEntries().get(index).getUuid(), frames.compact());
                    return true;
                }
            }
            if (frames.readerVisible()) {
                BookRect reader = frames.reader();
                if (closeBox(reader).contains(mouseX, mouseY)) {
                    this.close();
                    return true;
                }
                if ((this.rolePickerOpen || frames.compact()) && backBox(reader).contains(mouseX, mouseY)) {
                    if (this.rolePickerOpen) {
                        this.rolePickerOpen = false;
                    } else {
                        this.compactReaderOpen = false;
                    }
                    return true;
                }
                if (currentEntry() != null) {
                    BookRect sheet = sheet(reader);
                    BookRect view = sheetViewport(sheet);
                    if (isWithin(mouseX, mouseY, view.x(), view.y(), view.width(), view.height())) {
                        int contentHeight = this.rolePickerOpen ? pickerContentHeight() : detailContentHeight(sheet);
                        int maxScroll = Math.max(0, contentHeight - view.height());
                        if (maxScroll > 0 && readerScrollHit(sheet).contains(mouseX, mouseY)) {
                            int track = view.height() - 4;
                            int thumb = BookPaint.thumbSize(track, view.height(), contentHeight);
                            int thumbY = BookPaint.thumbOffset(view.y() + 2, track, thumb,
                                    this.rolePickerOpen ? this.pickerScrollOffset : this.detailScrollOffset, view.height(), contentHeight);
                            if (isWithin(mouseX, mouseY, sheet.right() - 7, thumbY, 5, thumb)) {
                                this.draggingDetailScrollbar = true;
                                this.detailDragOffset = (int) mouseY - thumbY;
                            } else {
                                int scroll = scrollFromThumb((int) mouseY - thumb / 2, view.y() + 2, track, thumb, view.height(), contentHeight);
                                if (this.rolePickerOpen) {
                                    this.pickerScrollOffset = scroll;
                                } else {
                                    this.detailScrollOffset = scroll;
                                }
                            }
                            return true;
                        }
                        if (this.rolePickerOpen) {
                            int originY = view.y() - MathHelper.clamp(this.pickerScrollOffset, 0, maxScroll);
                            int tx0 = sheet.x() + TEXT_PAD;
                            List<PickerItem> items = pickerItems();
                            int index = (int) ((mouseY - (originY + 4)) / PICKER_ROW);
                            if (mouseX >= tx0 && mouseX < sheet.right() - TEXT_PAD
                                    && index >= 0 && index < items.size()) {
                                PickerItem item = items.get(index);
                                if (item.header()) {
                                    if (!this.collapsedFactions.remove(item.faction())) {
                                        this.collapsedFactions.add(item.faction());
                                    }
                                } else {
                                    String roleId = item.role() == null ? "" : item.role().identifier().toString();
                                    setField(2, roleId);
                                    Text warning = AttendantUi.duplicateRoleWarning(component(), this.selected, roleId);
                                    this.roleWarning = warning;
                                    if (warning != null) {
                                        this.player.sendMessage(warning, true);
                                    }
                                    this.rolePickerOpen = false;
                                }
                                return true;
                            }
                        } else {
                            int originY = view.y() - MathHelper.clamp(this.detailScrollOffset, 0, maxScroll);
                            DetailLayout layout = detailLayout(sheet, originY);
                            for (int i = 0; i < 3; i++) {
                                if (segmentBox(layout.metSegment(), i).contains(mouseX, mouseY)) {
                                    setField(0, String.valueOf(i));
                                    return true;
                                }
                                if (segmentBox(layout.aliveSegment(), i).contains(mouseX, mouseY)) {
                                    setField(1, String.valueOf(i));
                                    return true;
                                }
                            }
                            if (layout.roleRow().contains(mouseX, mouseY)) {
                                this.rolePickerOpen = true;
                                this.pickerScrollOffset = 0;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingDirectoryScrollbar = false;
            this.draggingDetailScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        Frames frames = getFrames();
        if (button == 0 && this.draggingDirectoryScrollbar && frames.directoryVisible()) {
            BookRect view = directoryViewport(frames.directory());
            int total = displayEntries().size() * DIRECTORY_ROW;
            int track = view.height();
            int thumb = BookPaint.thumbSize(track, view.height(), total);
            this.directoryScrollOffset = scrollFromThumb((int) mouseY - this.directoryDragOffset, view.y(), track, thumb, view.height(), total);
            return true;
        }
        if (button == 0 && this.draggingDetailScrollbar && frames.readerVisible()) {
            BookRect sheet = sheet(frames.reader());
            BookRect view = sheetViewport(sheet);
            int contentHeight = this.rolePickerOpen ? pickerContentHeight() : detailContentHeight(sheet);
            int track = view.height() - 4;
            int thumb = BookPaint.thumbSize(track, view.height(), contentHeight);
            int scroll = scrollFromThumb((int) mouseY - this.detailDragOffset, view.y() + 2, track, thumb, view.height(), contentHeight);
            if (this.rolePickerOpen) {
                this.pickerScrollOffset = scroll;
            } else {
                this.detailScrollOffset = scroll;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Frames frames = getFrames();
        if (frames.directoryVisible()) {
            BookRect nav = frames.directory();
            BookRect view = directoryViewport(nav);
            int maxScroll = Math.max(0, displayEntries().size() * DIRECTORY_ROW - view.height());
            if (isWithin(mouseX, mouseY, nav.x(), view.y(), nav.width(), view.height()) && maxScroll > 0) {
                this.directoryScrollOffset = MathHelper.clamp(this.directoryScrollOffset - (int) (verticalAmount * 26), 0, maxScroll);
                return true;
            }
        }
        if (frames.readerVisible() && currentEntry() != null) {
            BookRect reader = frames.reader();
            BookRect sheet = sheet(reader);
            BookRect view = sheetViewport(sheet);
            int contentHeight = this.rolePickerOpen ? pickerContentHeight() : detailContentHeight(sheet);
            int maxScroll = Math.max(0, contentHeight - view.height());
            if (isWithin(mouseX, mouseY, sheet.x(), view.y(), sheet.width(), view.height()) && maxScroll > 0) {
                int step = this.rolePickerOpen ? 20 : 24;
                if (this.rolePickerOpen) {
                    this.pickerScrollOffset = MathHelper.clamp(this.pickerScrollOffset - (int) (verticalAmount * step), 0, maxScroll);
                } else {
                    this.detailScrollOffset = MathHelper.clamp(this.detailScrollOffset - (int) (verticalAmount * step), 0, maxScroll);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (!canStayOpen()) {
            this.close();
        }
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

    // ==================== 辅助 ====================

    private int rowIndexAt(double mouseX, double mouseY, BookRect view, int origin) {
        if (mouseX < view.x() || mouseX >= view.right() - 5 || mouseY < view.y() || mouseY >= view.bottom()) {
            return -1;
        }
        int index = (int) ((mouseY - origin) / DIRECTORY_ROW);
        return index >= 0 && index < displayEntries().size() ? index : -1;
    }

    private int scrollFromThumb(int thumbY, int trackTop, int track, int thumb, int viewport, int total) {
        int travel = Math.max(1, track - thumb);
        int span = Math.max(1, total - viewport);
        int clamped = MathHelper.clamp(thumbY, trackTop, trackTop + track - thumb);
        return (clamped - trackTop) * span / travel;
    }

    private void drawBrassTooltip(DrawContext context, List<Text> lines, double mouseX, double mouseY) {
        int tipWidth = BookPaint.tooltipWidth(this.textRenderer, lines);
        int tipHeight = BookPaint.tooltipHeight(lines.size());
        int x = MathHelper.clamp((int) mouseX + 12, 4, this.width - 4 - tipWidth);
        int y = MathHelper.clamp((int) mouseY - 12, 4, this.height - 4 - tipHeight);
        BookPaint.tooltip(context, this.textRenderer, lines, x, y, tipWidth);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Frames(BookRect directory, boolean directoryVisible, BookRect reader, boolean readerVisible,
                          boolean compact, boolean closeInBand) {
    }
}

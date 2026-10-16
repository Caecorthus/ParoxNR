package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.client.widget.AttendantRoleChoiceWidget;
import org.agmas.noellesroles.client.widget.AttendantTargetWidget;
import org.agmas.noellesroles.packet.AttendantEditInfoC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 乘务登记表面板（旧版风格）：网格展示所有乘客头像与 ID；点击头像进入编辑视图。 */
public class AttendantRegisterLegacyScreen extends Screen {
    private static final int ACCENT_BAR_COLOR = 0xFF000000 | (Noellesroles.ATTENDANT.color() & 0x00FFFFFF);
    private static final int COLUMNS = 6;
    private static final int SPACING_X = 36;
    private static final int SPACING_Y = 45;
    private static final double SCROLL_STEP = 24.0;
    private static final int ROLE_COLUMNS = 3;
    private static final int ROLE_BUTTON_WIDTH = 90;
    private static final int ROLE_BUTTON_HEIGHT = 20;
    private static final int ROLE_GAP_X = 5;
    private static final int ROLE_GAP_Y = 5;
    private static final int ROLE_HEADER_HEIGHT = 14;
    private static final int ROLE_HEADER_GAP = 2;
    private static final int ROLE_FACTION_GAP = 6;
    private static final double ROLE_SCROLL_STEP = ROLE_BUTTON_HEIGHT + ROLE_GAP_Y;

    private final ClientPlayerEntity player;
    private final List<AttendantRoleChoiceWidget> roleWidgets = new ArrayList<>();
    private final List<RolePlacement> rolePlacements = new ArrayList<>();
    private final List<RoleHeader> roleHeaders = new ArrayList<>();
    private UUID selected;
    private int scrollOffset;
    private int maxScroll;
    private int targetCount;
    private int roleListStartX;
    private int roleViewportTop;
    private int roleViewportBottom;
    private int roleContentHeight;
    private double roleScrollOffset = 0.0;
    private String pendingRoleId = null;
    private int metFilter;
    private int aliveFilter;
    private Text roleWarning = null;

    public AttendantRegisterLegacyScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.attendant.title"));
        this.player = player;
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
        this.roleWidgets.clear();

        if (!canStayOpen()) {
            this.close();
            return;
        }
        if (this.selected != null && component().getEntry(this.selected) == null) {
            this.selected = null;
        }

        if (this.selected == null) {
            initGridView();
        } else {
            initEditView();
        }
    }

    // ==== 网格视图 ====

    private void initGridView() {
        List<AttendantPlayerComponent.Entry> entries = filteredEntries();
        this.targetCount = entries.size();

        int centerX = this.width / 2;
        int rows = Math.max(1, RoleScreenHelper.getGridRowCount(entries.size(), COLUMNS));
        int contentHeight = rows * SPACING_Y + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
        int viewTop = RoleScreenHelper.getMenuViewTop(this.height);
        int contentTop = RoleScreenHelper.getMenuContentTop(this.height);
        int viewBottom = RoleScreenHelper.getMenuViewBottom(this.height);
        int viewHeight = Math.max(1, viewBottom - viewTop);
        this.maxScroll = Math.max(0, contentHeight - viewHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        int startX = RoleScreenHelper.getGridStartX(entries.size(), COLUMNS, SPACING_X, centerX);
        int startY = contentTop - this.scrollOffset;

        for (int i = 0; i < entries.size(); i++) {
            AttendantPlayerComponent.Entry entry = entries.get(i);
            int row = i / COLUMNS;
            int col = i % COLUMNS;

            AttendantTargetWidget widget = new AttendantTargetWidget(
                    startX + col * SPACING_X,
                    startY + row * SPACING_Y,
                    entry.getUuid(),
                    displayName(entry.getName()),
                    entry.getRoomName(),
                    entry.getMet(),
                    entry.getAlive(),
                    entry.getRoleId(),
                    this::selectTarget,
                    0, viewTop, this.width, viewBottom
            );
            widget.visible = RoleScreenHelper.intersectsPlayerWidgetFrame(widget.getX(), widget.getY(),
                    0, viewTop, this.width, viewBottom);
            addDrawableChild(widget);
        }

        int buttonY = RoleScreenHelper.getMenuButtonY(this.height);
        addDrawableChild(ButtonWidget.builder(metFilterLabel(), button -> {
                    this.metFilter = (this.metFilter + 1) % 3;
                    this.clearAndInit();
                })
                .dimensions(centerX - 140, buttonY, 95, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(aliveFilterLabel(), button -> {
                    this.aliveFilter = (this.aliveFilter + 1) % 3;
                    this.clearAndInit();
                })
                .dimensions(centerX - 40, buttonY, 95, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attendant.button.close"), button -> this.close())
                .dimensions(centerX + 60, buttonY, 80, 20)
                .build());
    }

    private List<AttendantPlayerComponent.Entry> sortedEntries() {
        List<AttendantPlayerComponent.Entry> entries = new ArrayList<>(component().getEntryList());
        entries.sort(Comparator
                .comparingInt((AttendantPlayerComponent.Entry entry) ->
                        entry.getRoomIndex() < 0 ? Integer.MAX_VALUE : entry.getRoomIndex())
                .thenComparing(AttendantPlayerComponent.Entry::getName));
        return entries;
    }

    private List<AttendantPlayerComponent.Entry> filteredEntries() {
        return sortedEntries().stream()
                .filter(entry -> AttendantUi.matchesFilter(entry, this.metFilter, this.aliveFilter))
                .toList();
    }

    private Text metFilterLabel() {
        return Text.translatable("screen.attendant.filter.met_label", AttendantUi.metFilterText(this.metFilter));
    }

    private Text aliveFilterLabel() {
        return Text.translatable("screen.attendant.filter.alive_label", AttendantUi.aliveFilterText(this.aliveFilter));
    }

    private void selectTarget(UUID targetUuid) {
        this.selected = targetUuid;
        this.roleScrollOffset = 0.0;
        this.pendingRoleId = null;
        this.roleWarning = null;
        this.clearAndInit();
    }

    // ==== 编辑视图 ====

    private void initEditView() {
        int centerX = this.width / 2;
        int buttonsY = getEditButtonsY();
        this.roleViewportTop = getRoleViewportTop();
        this.roleViewportBottom = getRoleViewportBottom();

        addDrawableChild(ButtonWidget.builder(metLabel(), button -> {
                    byte next = (byte) ((currentValue(0) + 1) % 3);
                    ClientPlayNetworking.send(new AttendantEditInfoC2SPacket(this.selected, 0, String.valueOf(next)));
                    button.setMessage(metLabel(next));
                })
                .dimensions(centerX - 100, buttonsY, 95, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(aliveLabel(), button -> {
                    byte next = (byte) ((currentValue(1) + 1) % 3);
                    ClientPlayNetworking.send(new AttendantEditInfoC2SPacket(this.selected, 1, String.valueOf(next)));
                    button.setMessage(aliveLabel(next));
                })
                .dimensions(centerX + 5, buttonsY, 95, 20)
                .build());

        // 角色选择网格：首项为「未知」（清除），其余按阵营分组（乘客/杀手/中立）
        this.roleWidgets.clear();
        this.rolePlacements.clear();
        this.roleHeaders.clear();
        int totalWidth = ROLE_COLUMNS * ROLE_BUTTON_WIDTH + (ROLE_COLUMNS - 1) * ROLE_GAP_X;
        this.roleListStartX = centerX - totalWidth / 2;
        int y = this.roleViewportTop;

        addRoleChoice(null, "", this.roleListStartX, y, totalWidth);
        y += ROLE_BUTTON_HEIGHT + ROLE_GAP_Y;

        List<AttendantUi.FactionGroup> groups =
                AttendantUi.groupedRoles(GameWorldComponent.KEY.get(this.player.getWorld()));
        for (AttendantUi.FactionGroup group : groups) {
            this.roleHeaders.add(new RoleHeader(group.faction(), this.roleListStartX, y));
            y += ROLE_HEADER_HEIGHT + ROLE_HEADER_GAP;
            List<Role> roles = group.roles();
            int rows = Math.max(1, (roles.size() + ROLE_COLUMNS - 1) / ROLE_COLUMNS);
            for (int i = 0; i < roles.size(); i++) {
                int col = i % ROLE_COLUMNS;
                int row = i / ROLE_COLUMNS;
                addRoleChoice(roles.get(i), roles.get(i).identifier().toString(),
                        this.roleListStartX + col * (ROLE_BUTTON_WIDTH + ROLE_GAP_X),
                        y + row * (ROLE_BUTTON_HEIGHT + ROLE_GAP_Y), ROLE_BUTTON_WIDTH);
            }
            y += rows * (ROLE_BUTTON_HEIGHT + ROLE_GAP_Y) + ROLE_FACTION_GAP;
        }
        this.roleContentHeight = y - this.roleViewportTop + ROLE_FACTION_GAP;
        this.roleScrollOffset = Math.max(0.0, Math.min(this.roleScrollOffset, getMaxRoleScroll()));
        updateRoleWidgetPositions();

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attendant.button.back"), button -> {
                    this.selected = null;
                    this.roleScrollOffset = 0.0;
                    this.pendingRoleId = null;
                    this.roleWarning = null;
                    this.clearAndInit();
                })
                .dimensions(centerX - 86, RoleScreenHelper.getMenuButtonY(this.height), 80, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attendant.button.close"), button -> this.close())
                .dimensions(centerX + 6, RoleScreenHelper.getMenuButtonY(this.height), 80, 20)
                .build());
    }

    private byte currentValue(int field) {
        AttendantPlayerComponent.Entry entry = component().getEntry(this.selected);
        if (entry == null) {
            return AttendantPlayerComponent.UNKNOWN;
        }
        return field == 0 ? entry.getMet() : entry.getAlive();
    }

    private void addRoleChoice(Role role, String choiceId, int baseX, int baseY, int width) {
        AttendantRoleChoiceWidget widget = new AttendantRoleChoiceWidget(baseX, baseY, width, role, choiceId,
                () -> choiceId.equals(currentRoleId()),
                () -> chooseRole(choiceId),
                this.selected, ACCENT_BAR_COLOR, 0, this.roleViewportTop, this.width, this.roleViewportBottom);
        this.roleWidgets.add(widget);
        this.rolePlacements.add(new RolePlacement(widget, baseX, baseY));
        addDrawableChild(widget);
    }

    /** 选择职业：本地先显示待同步值；若与已登记职业重复（义警/平民除外）则提示。 */
    private void chooseRole(String choiceId) {
        this.pendingRoleId = choiceId;
        Text warning = AttendantUi.duplicateRoleWarning(component(), this.selected, choiceId);
        this.roleWarning = warning;
        if (warning != null) {
            this.player.sendMessage(warning, true);
        }
    }

    /** 当前职业登记值；点击后先用本地待同步值显示，等组件同步到位后自动切回服务端值。 */
    private String currentRoleId() {
        AttendantPlayerComponent.Entry entry = component().getEntry(this.selected);
        String syncedRoleId = entry == null ? "" : entry.getRoleId();
        if (this.pendingRoleId != null) {
            if (this.pendingRoleId.equals(syncedRoleId)) {
                this.pendingRoleId = null;
            } else {
                return this.pendingRoleId;
            }
        }
        return syncedRoleId;
    }

    private Text metLabel() {
        return metLabel(currentValue(0));
    }

    private Text aliveLabel() {
        return aliveLabel(currentValue(1));
    }

    private static Text metLabel(byte value) {
        return Text.translatable("screen.attendant.button.met", AttendantUi.triStateText(value));
    }

    private static Text aliveLabel(byte value) {
        return Text.translatable("screen.attendant.button.alive", AttendantUi.triStateText(value));
    }

    private void updateRoleWidgetPositions() {
        int scroll = (int) Math.round(this.roleScrollOffset);
        for (RolePlacement placement : this.rolePlacements) {
            AttendantRoleChoiceWidget widget = placement.widget();
            int x = placement.baseX();
            int y = placement.baseY() - scroll;
            widget.setPosition(x, y);
            widget.visible = RoleScreenHelper.intersectsRect(x, y, widget.getWidth(), widget.getHeight(),
                    0, this.roleViewportTop, this.width, this.roleViewportBottom);
        }
    }

    private double getMaxRoleScroll() {
        return Math.max(0, this.roleContentHeight - (this.roleViewportBottom - this.roleViewportTop));
    }

    // ==== 编辑视图布局（init 与 render 共用） ====

    private int getEditAvatarY() {
        return RoleScreenHelper.getMenuTitleY(this.height / 2) + 24;
    }

    private int getEditInfoLineY() {
        return getEditAvatarY() + 40;
    }

    private int getEditButtonsY() {
        return getEditInfoLineY() + 26;
    }

    private int getRoleLabelY() {
        return getEditButtonsY() + 26;
    }

    private int getRoleViewportTop() {
        return getRoleLabelY() + 12;
    }

    private int getRoleViewportBottom() {
        return this.height - 46;
    }

    // ==== 渲染 ====

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (this.selected == null) {
            RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.attendant.title"),
                    centerX, RoleScreenHelper.getMenuTitleY(centerY));
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.attendant.subtitle.grid"),
                    centerX, RoleScreenHelper.getMenuSubtitleY(centerY));
            if (this.targetCount == 0) {
                RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.attendant.empty"),
                        centerX, centerY);
            }
            RoleScreenHelper.renderTopmostPlayerOverlays(context, font, this.children());
            return;
        }

        AttendantPlayerComponent.Entry entry = component().getEntry(this.selected);
        if (entry == null) {
            return;
        }
        String name = displayName(entry.getName());
        RoleScreenHelper.drawCenteredTitle(context, font, Text.literal(name), centerX, RoleScreenHelper.getMenuTitleY(centerY));

        if (this.roleWarning != null) {
            context.drawCenteredTextWithShadow(font, this.roleWarning, centerX,
                    RoleScreenHelper.getMenuSubtitleY(centerY), 0xFFBB66);
        }

        context.getMatrices().push();
        context.getMatrices().translate(centerX - 16, getEditAvatarY(), 0);
        context.getMatrices().scale(2.0f, 2.0f, 2.0f);
        PlayerSkinDrawer.draw(context, RoleScreenHelper.getPlayerSkinTextures(this.selected).texture(), 0, 0, 16);
        context.getMatrices().pop();

        RoleScreenHelper.drawCenteredSubTitle(context, font,
                Text.translatable("screen.attendant.line.id", name), centerX, getEditInfoLineY());
        RoleScreenHelper.drawCenteredSubTitle(context, font,
                Text.translatable("screen.attendant.line.room", AttendantUi.roomText(entry.getRoomName())), centerX, getEditInfoLineY() + 12);
        RoleScreenHelper.drawCenteredSubTitle(context, font,
                Text.translatable("screen.attendant.line.role", AttendantUi.roleText(currentRoleId())), centerX, getRoleLabelY());

        if (!this.roleHeaders.isEmpty() && this.roleViewportBottom > this.roleViewportTop) {
            int scroll = (int) Math.round(this.roleScrollOffset);
            context.enableScissor(0, this.roleViewportTop, this.width, this.roleViewportBottom);
            for (RoleHeader header : this.roleHeaders) {
                int headerY = header.baseY() - scroll;
                if (headerY + ROLE_HEADER_HEIGHT < this.roleViewportTop || headerY > this.roleViewportBottom) {
                    continue;
                }
                drawFactionHeader(context, header.faction(), header.baseX(), headerY);
            }
            context.disableScissor();
        }
    }

    private void drawFactionHeader(DrawContext context, Faction faction, int x, int y) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        Text label = AttendantUi.factionText(faction);
        context.drawTextWithShadow(font, label, x + 2, y + 3, 0xFFB9A06A);
        int lineX = x + 2 + font.getWidth(label) + 6;
        int lineEnd = x + ROLE_COLUMNS * ROLE_BUTTON_WIDTH + (ROLE_COLUMNS - 1) * ROLE_GAP_X;
        if (lineEnd - lineX > 6) {
            context.fill(lineX, y + 7, lineEnd, y + 8, 0x66FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.selected == null && this.maxScroll > 0) {
            int nextOffset = Math.max(0, Math.min(
                    this.scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP), this.maxScroll));
            if (nextOffset != this.scrollOffset) {
                this.scrollOffset = nextOffset;
                this.clearAndInit();
                return true;
            }
        }
        if (this.selected != null && mouseY >= this.roleViewportTop && mouseY <= this.roleViewportBottom) {
            double nextOffset = Math.max(0.0, Math.min(
                    this.roleScrollOffset - verticalAmount * ROLE_SCROLL_STEP, getMaxRoleScroll()));
            if (nextOffset != this.roleScrollOffset) {
                this.roleScrollOffset = nextOffset;
                updateRoleWidgetPositions();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void tick() {
        if (!canStayOpen()) {
            this.close();
        }
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
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RoleScreenHelper.renderRoleMenuBackground(context, this.width, this.height, ACCENT_BAR_COLOR);
    }

    private static String displayName(String name) {
        return name == null || name.isEmpty()
                ? Text.translatable("screen.role_target.unknown_player").getString()
                : name;
    }

    private record RolePlacement(AttendantRoleChoiceWidget widget, int baseX, int baseY) {
    }

    private record RoleHeader(Faction faction, int baseX, int baseY) {
    }
}

package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.roleinfo.RoleInfoData;
import org.agmas.noellesroles.client.roleinfo.RoleInfoRegistry;
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

public class RoleInfoLegacyScreen extends Screen {
    private static final int HEADER_HEIGHT = 25;
    private static final int FOOTER_HEIGHT = 25;
    private static final int SIDE_PADDING = 18;
    private static final int COLUMN_GAP = 10;
    private static final int SEARCH_HEIGHT = 20;
    private static final int LEFT_MIN_WIDTH = 146;
    private static final int LEFT_MAX_WIDTH = 178;
    private static final int ACTION_BUTTON_SIZE = 20;
    private static final int ACTION_BUTTON_GAP = 3;
    private static final int LIST_ENTRY_HEIGHT = 20;
    private static final int LIST_ENTRY_GAP = 3;
    private static final int DETAIL_LINE_HEIGHT = 12;
    private static final int SECTION_GAP = 8;
    private static final int TREE_INDENT = 12;
    private static final int TEXT_PADDING = 8;

    private final Set<String> expandedCategoryIds = new LinkedHashSet<>();
    private final Set<String> searchExpandedCategoryIds = new LinkedHashSet<>();
    private final List<TreeEntry> visibleEntries = new ArrayList<>();

    private TextFieldWidget searchField;
    private int treeScrollOffset;
    private int treeMaxScroll;
    private int detailScrollOffset;
    private int detailMaxScroll;
    private String selectedEntryId = RoleInfoRegistry.CATEGORY_CLASSIC;
    private boolean draggingTreeScrollbar;
    private boolean draggingDetailScrollbar;
    private int treeDragOffset;
    private int detailDragOffset;

    public RoleInfoLegacyScreen() {
        super(Text.translatable("roleinfo.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        String previousSearch = this.searchField != null ? this.searchField.getText() : "";

        clearChildren();
        initializeSelectionForContext();
        Layout layout = getLayout();
        this.searchField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                layout.leftX,
                layout.searchY,
                layout.searchWidth,
                SEARCH_HEIGHT,
                Text.translatable("roleinfo.search")
        ));
        this.searchField.setPlaceholder(Text.translatable("roleinfo.search.placeholder"));
        this.searchField.setText(previousSearch);
        this.searchField.setChangedListener(value -> {
            treeScrollOffset = 0;
            refreshVisibleEntries();
        });

        refreshVisibleEntries();
        ensureSelectedEntryVisible();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0xB8242424);
        context.fillGradient(0, 0, this.width, HEADER_HEIGHT + 8, 0x9A343434, 0x20343434);
        context.fillGradient(0, this.height - FOOTER_HEIGHT - 8, this.width, this.height, 0x20343434, 0x9A343434);

        Layout layout = getLayout();
        updateTreeScrollBounds(layout);
        updateDetailScrollBounds(layout);

        renderPanels(context, layout);
        renderActionButtons(context, mouseX, mouseY, layout);
        renderTree(context, mouseX, mouseY, layout);
        renderDetail(context, layout);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        RoleInfoData selected = getSelectedInfo();
        Text subtitle = selected != null && selected.factionKey != null
                ? RoleInfoRegistry.resolveText(selected.factionKey)
                : getModeSubtitle();
        context.drawCenteredTextWithShadow(this.textRenderer, subtitle, this.width / 2, 22, 0xA8A8A8);
        renderCurrentRoleText(context);

        Text hoverTooltip = getHoveredActionTooltip(mouseX, mouseY, layout);
        if (hoverTooltip != null) {
            context.drawTooltip(this.textRenderer, hoverTooltip, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
    }

    @Override
    protected void applyBlur(float delta) {
    }

    private void renderPanels(DrawContext context, Layout layout) {
        context.fill(layout.leftX - 2, layout.contentTop - 2, layout.leftX + layout.leftWidth + 2, layout.contentBottom + 2, 0xA0000000);
        context.fill(layout.rightX - 2, layout.contentTop - 2, layout.rightX + layout.rightWidth + 2, layout.contentBottom + 2, 0xA0000000);
    }

    private void renderTree(DrawContext context, int mouseX, int mouseY, Layout layout) {
        Set<String> effectiveExpandedCategoryIds = getEffectiveExpandedCategoryIds();
        context.enableScissor(layout.leftX - 2, layout.listTop - 2, layout.leftX + layout.leftWidth + 2, layout.contentBottom + 2);
        for (int i = 0; i < visibleEntries.size(); i++) {
            TreeEntry entry = visibleEntries.get(i);
            int rowY = layout.listTop + i * (LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP) - treeScrollOffset;
            if (rowY + LIST_ENTRY_HEIGHT <= layout.listTop || rowY >= layout.contentBottom) {
                continue;
            }

            boolean hovered = isWithin(mouseX, mouseY, layout.leftX, rowY, layout.leftWidth, LIST_ENTRY_HEIGHT);
            boolean selected = entry.id.equals(selectedEntryId);
            int background = selected ? 0xFF161616 : hovered ? 0xDD0E0E0E : 0xCC080808;
            int border = selected ? 0xFF404040 : 0xFF1B1B1B;
            int textColor = selected ? 0xFFFFFFFF : entry.isCategory ? 0xFFD9D9D9 : 0xFFBBBBBB;

            context.fill(layout.leftX, rowY, layout.leftX + layout.leftWidth, rowY + LIST_ENTRY_HEIGHT, background);
            context.fill(layout.leftX, rowY, layout.leftX + 1, rowY + LIST_ENTRY_HEIGHT, border);
            context.fill(layout.leftX + layout.leftWidth - 1, rowY, layout.leftX + layout.leftWidth, rowY + LIST_ENTRY_HEIGHT, border);
            context.fill(layout.leftX, rowY, layout.leftX + layout.leftWidth, rowY + 1, border);
            context.fill(layout.leftX, rowY + LIST_ENTRY_HEIGHT - 1, layout.leftX + layout.leftWidth, rowY + LIST_ENTRY_HEIGHT, border);

            int textX = layout.leftX + TEXT_PADDING + entry.depth * TREE_INDENT;
            if (entry.isCategory) {
                Text toggle = Text.literal(effectiveExpandedCategoryIds.contains(entry.id) ? "▼" : "▶");
                context.drawTextWithShadow(this.textRenderer, toggle, textX, rowY + 6, selected ? 0xFFE6E6E6 : 0xFF999999);
                textX += 10;
            } else {
                textX += 10;
            }

            context.drawTextWithShadow(this.textRenderer, entry.label, textX, rowY + 6, textColor);
        }
        context.disableScissor();

        if (treeMaxScroll > 0) {
            renderScrollbar(context, layout.leftScrollbarX, layout.listTop, layout.listHeight, treeScrollOffset, treeMaxScroll, draggingTreeScrollbar);
        }
    }

    private void renderActionButtons(DrawContext context, int mouseX, int mouseY, Layout layout) {
        renderActionButton(
                context,
                layout.expandButtonX,
                layout.searchY,
                Text.literal("<>"),
                isWithin(mouseX, mouseY, layout.expandButtonX, layout.searchY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE)
        );
        renderActionButton(
                context,
                layout.collapseButtonX,
                layout.searchY,
                Text.literal("><"),
                isWithin(mouseX, mouseY, layout.collapseButtonX, layout.searchY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE)
        );
    }

    private void renderActionButton(DrawContext context, int x, int y, Text label, boolean hovered) {
        int background = hovered ? 0xEE111111 : 0xD6080808;
        int border = hovered ? 0xFF606060 : 0xFF222222;
        context.fill(x, y, x + ACTION_BUTTON_SIZE, y + ACTION_BUTTON_SIZE, background);
        context.fill(x, y, x + ACTION_BUTTON_SIZE, y + 1, border);
        context.fill(x, y + ACTION_BUTTON_SIZE - 1, x + ACTION_BUTTON_SIZE, y + ACTION_BUTTON_SIZE, border);
        context.fill(x, y, x + 1, y + ACTION_BUTTON_SIZE, border);
        context.fill(x + ACTION_BUTTON_SIZE - 1, y, x + ACTION_BUTTON_SIZE, y + ACTION_BUTTON_SIZE, border);

        int textX = x + (ACTION_BUTTON_SIZE - this.textRenderer.getWidth(label)) / 2;
        int textY = y + (ACTION_BUTTON_SIZE - 8) / 2;
        context.drawTextWithShadow(this.textRenderer, label, textX, textY, hovered ? 0xFFFFFFFF : 0xFFD0D0D0);
    }

    private void renderDetail(DrawContext context, Layout layout) {
        RoleInfoData info = getSelectedInfo();
        TextRenderer font = this.textRenderer;
        int contentWidth = layout.rightWidth - TEXT_PADDING * 2;
        int x = layout.rightX + TEXT_PADDING;
        int y = layout.contentTop + 8 - detailScrollOffset;

        context.enableScissor(layout.rightX - 2, layout.contentTop - 2, layout.rightX + layout.rightWidth + 2, layout.contentBottom + 2);

        if (info == null) {
            context.drawCenteredTextWithShadow(font,
                    Text.translatable("roleinfo.no_info"),
                    layout.rightX + layout.rightWidth / 2,
                    layout.contentTop + layout.contentHeight / 2,
                    0xFFAAAAAA);
            context.disableScissor();
            return;
        }

        int titleColor = getTitleColor(info);
        Text titleText = RoleInfoRegistry.resolveText(info.nameKey);
        context.drawTextWithShadow(font, titleText, x, y, titleColor);
        y += DETAIL_LINE_HEIGHT + 2;

        if (info.factionKey != null) {
            Text factionText = Text.translatable("roleinfo.faction_label", RoleInfoRegistry.resolveText(info.factionKey));
            y = drawWrappedText(context, font, factionText, x, y, contentWidth, 0xFFBBBBBB) + SECTION_GAP;
        }

        context.fill(x, y, x + contentWidth, y + 1, 0xFF2B2B2B);
        y += SECTION_GAP;

        y = drawSection(context, font, x, y, contentWidth,
                getDescriptionLabel(),
                RoleInfoRegistry.resolveText(info.descriptionKey),
                0xFFDDDD66,
                0xFFDDDDDD);

        if (info.winConditionKey != null) {
            y = drawSection(context, font, x, y, contentWidth,
                    Text.translatable("roleinfo.win_condition_label"),
                    RoleInfoRegistry.resolveText(info.winConditionKey),
                    0xFFFFCC00,
                    0xFFEEEE88);
        }

        if (info.skills != null && !info.skills.isEmpty()) {
            context.fill(x, y, x + contentWidth, y + 1, 0xFF2B2B2B);
            y += SECTION_GAP;

            boolean eventPage = isEventPage(selectedEntryId);
            context.drawTextWithShadow(font,
                    Text.translatable(eventPage ? "roleinfo.event_effects_header" : "roleinfo.skills_header"),
                    x,
                    y,
                    0xFFFFAA00);
            y += DETAIL_LINE_HEIGHT + SECTION_GAP;

            for (RoleInfoData.SkillInfoData skill : info.skills.values()) {
                Text skillName = Text.literal("| ").append(RoleInfoRegistry.resolveText(skill.nameKey));
                y = drawWrappedText(context, font, skillName, x, y, contentWidth, 0xFF00CCFF);

                if (!eventPage && skill.triggerKey != null) {
                    Text triggerLabel = Text.translatable("roleinfo.skill.trigger_label", RoleInfoRegistry.getTriggerText(skill));
                    y = drawWrappedText(context, font, triggerLabel, x + 8, y, contentWidth - 8, 0xFF666666);
                }

                Text effectLabel = Text.translatable("roleinfo.skill.effect_label", RoleInfoRegistry.resolveText(skill.effectKey));
                y = drawWrappedText(context, font, effectLabel, x + 8, y, contentWidth - 8, 0xFFBBBBBB);
                y += 6;
            }
        }

        context.disableScissor();

        int detailContentHeight = Math.max(0, y + detailScrollOffset - layout.contentTop);
        detailMaxScroll = Math.max(0, detailContentHeight - layout.contentHeight + 8);
        detailScrollOffset = Math.max(0, Math.min(detailScrollOffset, detailMaxScroll));

        if (detailMaxScroll > 0) {
            renderScrollbar(context, layout.rightScrollbarX, layout.contentTop, layout.contentHeight, detailScrollOffset, detailMaxScroll, draggingDetailScrollbar);
        }
    }

    private int drawSection(DrawContext context, TextRenderer font, int x, int y, int width, Text label, Text content, int labelColor, int contentColor) {
        context.drawTextWithShadow(font, label, x, y, labelColor);
        y += DETAIL_LINE_HEIGHT;
        y = drawWrappedText(context, font, content, x + 8, y, width - 8, contentColor);
        return y + SECTION_GAP;
    }

    private int drawWrappedText(DrawContext context, TextRenderer font, Text text, int x, int y, int width, int color) {
        List<OrderedText> lines = font.wrapLines(text, Math.max(20, width));
        for (OrderedText line : lines) {
            context.drawTextWithShadow(font, line, x, y, color);
            y += DETAIL_LINE_HEIGHT;
        }
        return y;
    }

    private int getTitleColor(RoleInfoData info) {
        if (info == null) {
            return 0xFFFFFFFF;
        }
        Integer roleColor = getRoleColorById(selectedEntryId);
        if (roleColor != null) {
            return 0xFF000000 | (roleColor & 0x00FFFFFF);
        }
        String factionId = RoleInfoRegistry.getFactionCategoryId(info);
        return switch (factionId) {
            case RoleInfoRegistry.CATEGORY_KILLER -> 0xFFFF6666;
            case RoleInfoRegistry.CATEGORY_NEUTRAL -> 0xFF8CD98C;
            default -> 0xFF8FC2FF;
        };
    }

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

    private void renderCurrentRoleText(DrawContext context) {
        String currentRoleId = getCurrentRoleIdentifier();
        if (currentRoleId == null) {
            return;
        }

        RoleInfoData currentRoleInfo = RoleInfoRegistry.get(currentRoleId);
        if (currentRoleInfo == null) {
            return;
        }

        int roleColor = 0xFFFFCC00;
        Integer resolvedRoleColor = getRoleColorById(currentRoleId);
        if (resolvedRoleColor != null) {
            roleColor = 0xFF000000 | (resolvedRoleColor & 0x00FFFFFF);
        }

        MutableText currentRoleText = Text.literal("")
                .append(Text.translatable("roleinfo.current_role.prefix").withColor(0xFFFFCC00))
                .append(RoleInfoRegistry.resolveText(currentRoleInfo.nameKey).copy().withColor(roleColor));
        context.drawCenteredTextWithShadow(this.textRenderer, currentRoleText, this.width / 2, this.height - FOOTER_HEIGHT + 8, 0xFFFFFF);
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
        updateTreeScrollBounds(getLayout());
        treeScrollOffset = Math.max(0, Math.min(treeScrollOffset, treeMaxScroll));
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

        // 迷乱谋杀模式已废弃：不再构建该分类节点（与 RoleInfoScreen 保持一致）。

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
        return root;
    }

    private void appendRoleChildren(TreeNode parent, List<RoleEntry> roles) {
        if (roles == null) {
            return;
        }
        for (RoleEntry role : roles) {
            parent.children.add(new TreeNode(role.id, role.label, false, parent.id));
        }
    }

    private String getCurrentRoleIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
        for (Role role : WatheRoles.ROLES) {
            if (gwc.isRole(client.player, role)) {
                return role.identifier().toString();
            }
        }
        return null;
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

    private Text getHoveredActionTooltip(int mouseX, int mouseY, Layout layout) {
        if (isWithin(mouseX, mouseY, layout.expandButtonX, layout.searchY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE)) {
            return Text.translatable("roleinfo.button.expand_all.tooltip");
        }
        if (isWithin(mouseX, mouseY, layout.collapseButtonX, layout.searchY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE)) {
            return Text.translatable("roleinfo.button.collapse_all.tooltip");
        }
        return null;
    }

    private void ensureSelectedEntryExists() {
        if (selectedEntryId == null) {
            selectedEntryId = getCurrentModeCategoryId();
        }
        boolean exists = visibleEntries.stream().anyMatch(entry -> entry.id.equals(selectedEntryId));
        if (!exists) {
            selectedEntryId = getCurrentModeCategoryId();
        }
    }

    private void ensureSelectedEntryVisible() {
        for (int i = 0; i < visibleEntries.size(); i++) {
            if (!visibleEntries.get(i).id.equals(selectedEntryId)) {
                continue;
            }
            Layout layout = getLayout();
            int rowY = layout.listTop + i * (LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP) - treeScrollOffset;
            if (rowY < layout.listTop) {
                treeScrollOffset = Math.max(0, treeScrollOffset - (layout.listTop - rowY));
            } else if (rowY + LIST_ENTRY_HEIGHT > layout.contentBottom) {
                treeScrollOffset = Math.min(treeMaxScroll, treeScrollOffset + (rowY + LIST_ENTRY_HEIGHT - layout.contentBottom));
            }
            return;
        }
    }

    private boolean isParentExpanded(TreeEntry entry) {
        if (entry.parentId == null) {
            return true;
        }
        return getEffectiveExpandedCategoryIds().contains(entry.parentId);
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
        boolean shouldExpand = descendantRoleMatched;
        if (shouldExpand && node.isCategory) {
            categoriesToExpand.add(node.id);
        }
        return selfMatched || descendantRoleMatched;
    }

    private Set<String> getEffectiveExpandedCategoryIds() {
        if (searchExpandedCategoryIds.isEmpty()) {
            return expandedCategoryIds;
        }
        Set<String> effectiveExpandedCategoryIds = new LinkedHashSet<>(expandedCategoryIds);
        effectiveExpandedCategoryIds.addAll(searchExpandedCategoryIds);
        return effectiveExpandedCategoryIds;
    }

    private String getCurrentModeCategoryId() {
        return getCurrentModeContext().categoryId();
    }

    private String getCurrentModeOverviewPageId() {
        return getCurrentModeContext().overviewPageId();
    }

    private ModeContext getCurrentModeContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return new ModeContext(
                    WatheGameModes.MURDER_ID,
                    RoleInfoRegistry.CATEGORY_CLASSIC,
                    RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW,
                    "roleinfo.category.classic.subtitle",
                    false
            );
        }
        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
        if (NoellesrolesClient.isDeathArenaActiveForClientPlayer()) {
            return new ModeContext(
                    WatheGameModes.LOOSE_ENDS_ID,
                    RoleInfoRegistry.CATEGORY_DEATH_ARENA,
                    RoleInfoRegistry.CATEGORY_DEATH_ARENA_OVERVIEW,
                    "roleinfo.category.death_arena.subtitle",
                    true
            );
        }
        GameMode gameMode = gwc.getGameMode();
        Identifier gameModeId = gameMode != null ? gameMode.identifier : null;
        // 迷乱谋杀模式已废弃：不再为它构造说明页上下文（与 RoleInfoScreen 保持一致）。
        if (WatheGameModes.MURDER_ID.equals(gameModeId)) {
            return new ModeContext(
                    gameModeId,
                    RoleInfoRegistry.CATEGORY_CLASSIC,
                    RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW,
                    "roleinfo.category.classic.subtitle",
                    true
            );
        }
        if (WatheGameModes.LOOSE_ENDS_ID.equals(gameModeId)) {
            return new ModeContext(
                    gameModeId,
                    RoleInfoRegistry.CATEGORY_LOOSE_ENDS,
                    RoleInfoRegistry.CATEGORY_LOOSE_ENDS_OVERVIEW,
                    "roleinfo.category.loose_ends.subtitle",
                    true
            );
        }
        return new ModeContext(
                WatheGameModes.MURDER_ID,
                RoleInfoRegistry.CATEGORY_CLASSIC,
                RoleInfoRegistry.CATEGORY_CLASSIC_OVERVIEW,
                "roleinfo.category.classic.subtitle",
                false
        );
    }

    private Text getModeSubtitle() {
        return Text.translatable(getCurrentModeContext().subtitleKey());
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

    private boolean isLooseEndsSelection() {
        return RoleInfoRegistry.CATEGORY_LOOSE_ENDS.equals(selectedEntryId)
                || RoleInfoRegistry.ROLE_LOOSE_END.equals(selectedEntryId);
    }

    private void updateTreeScrollBounds(Layout layout) {
        int contentHeight = visibleEntries.size() * (LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP);
        treeMaxScroll = Math.max(0, contentHeight - layout.listHeight + LIST_ENTRY_GAP);
    }

    private void updateDetailScrollBounds(Layout layout) {
        detailScrollOffset = Math.max(0, Math.min(detailScrollOffset, detailMaxScroll));
        treeScrollOffset = Math.max(0, Math.min(treeScrollOffset, treeMaxScroll));
    }

    private void renderScrollbar(DrawContext context, int x, int y, int height, int scrollOffset, int maxScroll, boolean dragging) {
        context.fill(x, y, x + 6, y + height, 0xD0101010);
        int thumbHeight = Math.max(18, (int) ((height / (float) (maxScroll + height)) * height));
        int thumbTravel = Math.max(0, height - thumbHeight);
        int thumbY = y + (maxScroll == 0 ? 0 : (int) (thumbTravel * (scrollOffset / (float) maxScroll)));
        int thumbColor = dragging ? 0xFFF5F5F5 : 0xFFD0D0D0;
        context.fill(x + 1, thumbY, x + 5, thumbY + thumbHeight, thumbColor);
    }

    private Integer getRoleColorById(String roleId) {
        if (roleId == null || roleId.startsWith("category:")) {
            return null;
        }
        for (Role role : WatheRoles.ROLES) {
            if (role.identifier().toString().equals(roleId)) {
                return role.color();
            }
        }
        return null;
    }

    private int getScrollbarThumbHeight(int viewportHeight, int maxScroll) {
        return Math.max(18, (int) ((viewportHeight / (float) (maxScroll + viewportHeight)) * viewportHeight));
    }

    private int getScrollbarThumbY(int trackY, int trackHeight, int scrollOffset, int maxScroll) {
        int thumbHeight = getScrollbarThumbHeight(trackHeight, maxScroll);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        return trackY + (maxScroll == 0 ? 0 : (int) (thumbTravel * (scrollOffset / (float) maxScroll)));
    }

    private int scrollOffsetFromThumbY(int thumbY, int trackY, int trackHeight, int maxScroll) {
        if (maxScroll <= 0) {
            return 0;
        }
        int thumbHeight = getScrollbarThumbHeight(trackHeight, maxScroll);
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int clampedThumbY = Math.max(trackY, Math.min(thumbY, trackY + trackHeight - thumbHeight));
        float ratio = (clampedThumbY - trackY) / (float) thumbTravel;
        return Math.round(ratio * maxScroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Layout layout = getLayout();
            if (treeMaxScroll > 0 && isWithin(mouseX, mouseY, layout.leftScrollbarX, layout.listTop, 6, layout.listHeight)) {
                int thumbY = getScrollbarThumbY(layout.listTop, layout.listHeight, treeScrollOffset, treeMaxScroll);
                int thumbHeight = getScrollbarThumbHeight(layout.listHeight, treeMaxScroll);
                if (isWithin(mouseX, mouseY, layout.leftScrollbarX, thumbY, 6, thumbHeight)) {
                    draggingTreeScrollbar = true;
                    treeDragOffset = (int) mouseY - thumbY;
                } else {
                    treeScrollOffset = scrollOffsetFromThumbY((int) mouseY - thumbHeight / 2, layout.listTop, layout.listHeight, treeMaxScroll);
                }
                return true;
            }
            if (detailMaxScroll > 0 && isWithin(mouseX, mouseY, layout.rightScrollbarX, layout.contentTop, 6, layout.contentHeight)) {
                int thumbY = getScrollbarThumbY(layout.contentTop, layout.contentHeight, detailScrollOffset, detailMaxScroll);
                int thumbHeight = getScrollbarThumbHeight(layout.contentHeight, detailMaxScroll);
                if (isWithin(mouseX, mouseY, layout.rightScrollbarX, thumbY, 6, thumbHeight)) {
                    draggingDetailScrollbar = true;
                    detailDragOffset = (int) mouseY - thumbY;
                } else {
                    detailScrollOffset = scrollOffsetFromThumbY((int) mouseY - thumbHeight / 2, layout.contentTop, layout.contentHeight, detailMaxScroll);
                }
                return true;
            }
            if (isWithin(mouseX, mouseY, layout.expandButtonX, layout.searchY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE)) {
                expandAllCategories();
                return true;
            }
            if (isWithin(mouseX, mouseY, layout.collapseButtonX, layout.searchY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE)) {
                collapseAllCategories();
                return true;
            }
            for (int i = 0; i < visibleEntries.size(); i++) {
                int rowY = layout.listTop + i * (LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP) - treeScrollOffset;
                if (!isWithin(mouseX, mouseY, layout.leftX, rowY, layout.leftWidth, LIST_ENTRY_HEIGHT)) {
                    continue;
                }
                TreeEntry entry = visibleEntries.get(i);
                selectedEntryId = entry.id;
                detailScrollOffset = 0;
                if (entry.isCategory) {
                    if (expandedCategoryIds.contains(entry.id)) {
                        expandedCategoryIds.remove(entry.id);
                    } else {
                        expandedCategoryIds.add(entry.id);
                    }
                    refreshVisibleEntries();
                }
                return true;
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
        Layout layout = getLayout();
        if (button == 0 && draggingTreeScrollbar) {
            treeScrollOffset = scrollOffsetFromThumbY((int) mouseY - treeDragOffset, layout.listTop, layout.listHeight, treeMaxScroll);
            return true;
        }
        if (button == 0 && draggingDetailScrollbar) {
            detailScrollOffset = scrollOffsetFromThumbY((int) mouseY - detailDragOffset, layout.contentTop, layout.contentHeight, detailMaxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = getLayout();
        int scrollAmount = (int) (verticalAmount * (LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP));
        if (isWithin(mouseX, mouseY, layout.leftX, layout.contentTop, layout.leftWidth, layout.contentHeight) && treeMaxScroll > 0) {
            treeScrollOffset = Math.max(0, Math.min(treeScrollOffset - scrollAmount, treeMaxScroll));
            return true;
        }
        if (isWithin(mouseX, mouseY, layout.rightX, layout.contentTop, layout.rightWidth, layout.contentHeight) && detailMaxScroll > 0) {
            detailScrollOffset = Math.max(0, Math.min(detailScrollOffset - (int) (verticalAmount * 20), detailMaxScroll));
            return true;
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

    private Layout getLayout() {
        int leftWidth = Math.min(LEFT_MAX_WIDTH, Math.max(LEFT_MIN_WIDTH, (this.width - SIDE_PADDING * 2 - COLUMN_GAP) / 3));
        int leftX = SIDE_PADDING;
        int rightX = leftX + leftWidth + COLUMN_GAP;
        int rightWidth = this.width - rightX - SIDE_PADDING;
        int contentTop = HEADER_HEIGHT + 10;
        int contentBottom = this.height - FOOTER_HEIGHT - 10;
        int contentHeight = Math.max(30, contentBottom - contentTop);
        int searchY = contentTop;
        int listTop = searchY + SEARCH_HEIGHT + 8;
        int listHeight = Math.max(20, contentBottom - listTop);
        int collapseButtonX = leftX + leftWidth - ACTION_BUTTON_SIZE;
        int expandButtonX = collapseButtonX - ACTION_BUTTON_GAP - ACTION_BUTTON_SIZE;
        int searchWidth = Math.max(40, expandButtonX - ACTION_BUTTON_GAP - leftX);
        return new Layout(leftX, leftWidth, rightX, rightWidth, searchY, listTop, contentTop, contentBottom, contentHeight, listHeight, searchWidth, expandButtonX, collapseButtonX, leftX + leftWidth - 6, rightX + rightWidth - 6);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(
            int leftX,
            int leftWidth,
            int rightX,
            int rightWidth,
            int searchY,
            int listTop,
            int contentTop,
            int contentBottom,
            int contentHeight,
            int listHeight,
            int searchWidth,
            int expandButtonX,
            int collapseButtonX,
            int leftScrollbarX,
            int rightScrollbarX
    ) {
    }

    private record TreeEntry(String id, Text label, int depth, boolean isCategory, String parentId) {
    }

    private record RoleEntry(String id, Text label, boolean currentRole) {
    }

    private record ModeContext(Identifier gameModeId, String categoryId, String overviewPageId, String subtitleKey, boolean recognized) {
    }

    private static final class TreeNode {
        private final String id;
        private final Text label;
        private final boolean isCategory;
        private final String parentId;
        private final List<TreeNode> children = new ArrayList<>();

        private TreeNode(String id, Text label, boolean isCategory, String parentId) {
            this.id = id;
            this.label = label;
            this.isCategory = isCategory;
            this.parentId = parentId;
        }

        private boolean matches(String filter) {
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

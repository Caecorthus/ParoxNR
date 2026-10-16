package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.ForgedPotionItem;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.ForgedPotionSelectC2SPacket;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.client.widget.MenuPlayerTargetWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class RoleTargetMenuScreen extends Screen {
    private static final Text UNKNOWN_PLAYER_TEXT = Text.translatable("screen.role_target.unknown_player");
    private static final int COLUMNS = 6;
    private static final int SPACING_X = 36;
    private static final int SPACING_Y = 45;
    private static final double SCROLL_STEP = 24.0;

    private final ClientPlayerEntity player;
    private final MenuType menuType;
    private UUID firstSelection;
    private UUID secondSelection;
    private int scrollOffset;
    private int maxScroll;
    private int targetCount;

    public RoleTargetMenuScreen(ClientPlayerEntity player, MenuType menuType) {
        super(menuType.getTitle());
        this.player = player;
        this.menuType = menuType;
    }

    @Override
    protected void init() {
        super.init();

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!canStayOpen(gameWorld)) {
            this.close();
            return;
        }

        List<TargetEntry> targets = getTargets(gameWorld);
        this.targetCount = targets.size();

        int centerX = this.width / 2;
        int rows = Math.max(1, RoleScreenHelper.getGridRowCount(targets.size(), COLUMNS));
        int contentHeight = rows * SPACING_Y + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
        int viewTop = RoleScreenHelper.getMenuViewTop(this.height);
        int contentTop = RoleScreenHelper.getMenuContentTop(this.height);
        int viewBottom = RoleScreenHelper.getMenuViewBottom(this.height);
        int viewHeight = Math.max(1, viewBottom - viewTop);
        this.maxScroll = Math.max(0, contentHeight - viewHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        int startX = RoleScreenHelper.getGridStartX(targets.size(), COLUMNS, SPACING_X, centerX);
        int startY = contentTop - this.scrollOffset;

        for (int i = 0; i < targets.size(); i++) {
            TargetEntry target = targets.get(i);
            int row = i / COLUMNS;
            int col = i % COLUMNS;

            TargetWidget widget = new TargetWidget(
                    startX + col * SPACING_X,
                    startY + row * SPACING_Y,
                    target,
                    isSelected(target.uuid()),
                    this::handleSelection,
                    0, viewTop, this.width, viewBottom
            );
            widget.visible = RoleScreenHelper.intersectsPlayerWidgetFrame(widget.getX(), widget.getY(),
                    0, viewTop, this.width, viewBottom);
            addDrawableChild(widget);
        }

        int buttonY = RoleScreenHelper.getMenuButtonY(this.height);
        if (this.menuType == MenuType.SWAPPER) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.role_target.button.close"), button -> this.close())
                    .dimensions(centerX - 86, buttonY, 80, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.role_target.button.confirm"), button -> this.submitSwap())
                    .dimensions(centerX + 6, buttonY, 80, 20)
                    .build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.role_target.button.close"), button -> this.close())
                    .dimensions(centerX - 40, buttonY, 80, 20)
                    .build());
        }
    }

    private boolean canStayOpen(GameWorldComponent gameWorld) {
        if (!GameFunctions.isPlayerPlayingAndAlive(this.player) || SwallowedPlayerComponent.isPlayerSwallowed(this.player)) {
            return false;
        }
        return switch (this.menuType) {
            // 拾荒者搜刮到巫毒娃娃后同样可以正常绑定
            case VOODOO -> gameWorld.isRole(this.player, Noellesroles.VOODOO)
                    || gameWorld.isRole(this.player, Noellesroles.JUNKER);
            case SWAPPER -> gameWorld.isRole(this.player, Noellesroles.SWAPPER)
                    && AbilityPlayerComponent.KEY.get(this.player).getCooldown() <= 0;
            case MORPHLING -> gameWorld.isRole(this.player, Noellesroles.MORPHLING)
                    && MorphlingPlayerComponent.KEY.get(this.player).getMorphTicks() == 0;
            case FORGED_POTION -> gameWorld.isRole(this.player, Noellesroles.MORPHLING);
        };
    }

    private List<TargetEntry> getTargets(GameWorldComponent gameWorld) {
        List<TargetEntry> result = new ArrayList<>();
        switch (this.menuType) {
            case VOODOO -> {
                List<UUID> validPlayers = new ArrayList<>(gameWorld.getAllPlayers());
                validPlayers.remove(this.player.getUuid());
                for (UUID uuid : WatheClient.PLAYER_ENTRIES_CACHE.keySet()) {
                    if (!validPlayers.contains(uuid)) {
                        continue;
                    }
                    var targetPlayer = this.player.getWorld().getPlayerByUuid(uuid);
                    if (targetPlayer != null && SwallowedInteractionHelper.blocksPlayerTarget(targetPlayer, SwallowedInteractionHelper.TargetingRule.VOODOO_TARGET)) {
                        continue;
                    }
                    result.add(new TargetEntry(uuid, ShopEntry.Type.TOOL));
                }
            }
            case SWAPPER -> {
                List<UUID> alivePlayers = new ArrayList<>(gameWorld.getAllAlivePlayers());
                for (UUID uuid : WatheClient.PLAYER_ENTRIES_CACHE.keySet()) {
                    if (!alivePlayers.contains(uuid)) {
                        continue;
                    }
                    var targetPlayer = this.player.getWorld().getPlayerByUuid(uuid);
                    if (targetPlayer != null && SwallowedInteractionHelper.blocksPlayerTarget(targetPlayer)) {
                        continue;
                    }
                    result.add(new TargetEntry(uuid, ShopEntry.Type.POISON));
                }
            }
            case MORPHLING, FORGED_POTION -> {
                // 变形菜单不包含自己；赝造药水可选任意玩家（含自己与已死亡玩家）
                List<UUID> allPlayers = new ArrayList<>(gameWorld.getAllPlayers());
                if (this.menuType == MenuType.MORPHLING) {
                    allPlayers.remove(this.player.getUuid());
                }
                for (UUID uuid : WatheClient.PLAYER_ENTRIES_CACHE.keySet()) {
                    if (!allPlayers.contains(uuid)) {
                        continue;
                    }
                    var targetPlayer = this.player.getWorld().getPlayerByUuid(uuid);
                    if (targetPlayer != null && SwallowedInteractionHelper.blocksPlayerTarget(targetPlayer)) {
                        continue;
                    }
                    ShopEntry.Type backgroundType = gameWorld.isPlayerDead(uuid) ? ShopEntry.Type.WEAPON : ShopEntry.Type.POISON;
                    result.add(new TargetEntry(uuid, backgroundType));
                }
            }
        }
        return result;
    }

    private boolean isSelected(UUID uuid) {
        return switch (this.menuType) {
            case VOODOO -> Objects.equals(VoodooPlayerComponent.KEY.get(this.player).target, uuid);
            case SWAPPER -> Objects.equals(this.firstSelection, uuid) || Objects.equals(this.secondSelection, uuid);
            case MORPHLING -> false;
            case FORGED_POTION -> Objects.equals(ForgedPotionItem.getTarget(getHeldPotion()), uuid);
        };
    }

    private static ItemStack getHeldPotion() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        if (player.getMainHandStack().isOf(ModItems.FORGED_POTION)) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().isOf(ModItems.FORGED_POTION)) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private void handleSelection(UUID targetUuid) {
        switch (this.menuType) {
            case VOODOO, MORPHLING -> {
                ClientPlayNetworking.send(new MorphC2SPacket(targetUuid));
                this.close();
            }
            case FORGED_POTION -> {
                ClientPlayNetworking.send(new ForgedPotionSelectC2SPacket(targetUuid));
                this.close();
            }
            case SWAPPER -> {
                if (Objects.equals(this.firstSelection, targetUuid)) {
                    this.firstSelection = null;
                    if (Objects.equals(this.secondSelection, targetUuid)) {
                        this.secondSelection = null;
                    }
                } else if (this.firstSelection == null) {
                    this.firstSelection = targetUuid;
                } else if (Objects.equals(this.secondSelection, targetUuid)) {
                    this.secondSelection = null;
                } else {
                    this.secondSelection = targetUuid;
                }
                this.clearAndInit();
            }
        }
    }

    private void submitSwap() {
        if (this.firstSelection == null || this.secondSelection == null) {
            return;
        }
        ClientPlayNetworking.send(new SwapperC2SPacket(this.firstSelection, this.secondSelection));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        RoleScreenHelper.drawCenteredTitle(context, font, this.menuType.getTitle(), centerX, RoleScreenHelper.getMenuTitleY(centerY));
        RoleScreenHelper.drawCenteredSubTitle(context, font, getSubtitle(), centerX, RoleScreenHelper.getMenuSubtitleY(centerY));

        List<Text> statusLines = getStatusLines();
        for (int i = 0; i < statusLines.size(); i++) {
            RoleScreenHelper.drawCenteredSubTitle(context, font, statusLines.get(i), centerX, RoleScreenHelper.getMenuStatusY(centerY) + i * 12);
        }

        if (this.targetCount == 0) {
            RoleScreenHelper.drawCenteredSubTitle(context, font, this.menuType.getEmptyText(), centerX, centerY);
        }

        RoleScreenHelper.renderTopmostPlayerOverlays(context, font, this.children());
    }

    private Text getSubtitle() {
        return switch (this.menuType) {
            case VOODOO -> Text.translatable("screen.role_target.voodoo.subtitle");
            case SWAPPER -> Text.translatable("screen.role_target.swapper.subtitle");
            case MORPHLING -> Text.translatable("screen.role_target.morphling.subtitle");
            case FORGED_POTION -> Text.translatable("screen.role_target.forged_potion.subtitle");
        };
    }

    private List<Text> getStatusLines() {
        List<Text> lines = new ArrayList<>();
        switch (this.menuType) {
            case VOODOO -> {
                UUID currentTarget = VoodooPlayerComponent.KEY.get(this.player).target;
                if (currentTarget != null && !Objects.equals(currentTarget, this.player.getUuid())) {
                    lines.add(Text.translatable("screen.role_target.voodoo.current_target",
                            RoleScreenHelper.getPlayerName(currentTarget, UNKNOWN_PLAYER_TEXT)));
                }

                ConfigWorldComponent config = ConfigWorldComponent.KEY.get(this.player.getWorld());
                if (!config.naturalVoodoosAllowed) {
                    lines.add(Text.translatable("hud.voodoo.natural_death_disabled").styled(style -> style.withColor(0xFF5555)));
                }
            }
            case SWAPPER -> {
                if (this.firstSelection == null) {
                    lines.add(Text.translatable("hud.swapper.first_player_selection"));
                } else if (this.secondSelection == null) {
                    lines.add(Text.translatable("hud.swapper.second_player_selection"));
                } else {
                    lines.add(Text.translatable("screen.role_target.swapper.current_pair",
                            RoleScreenHelper.getPlayerName(this.firstSelection, UNKNOWN_PLAYER_TEXT),
                            RoleScreenHelper.getPlayerName(this.secondSelection, UNKNOWN_PLAYER_TEXT)));
                }
            }
            case MORPHLING -> {
            }
            case FORGED_POTION -> {
                UUID currentTarget = ForgedPotionItem.getTarget(getHeldPotion());
                if (currentTarget != null) {
                    lines.add(Text.translatable("item.noellesroles.forged_potion.target",
                            RoleScreenHelper.getPlayerName(currentTarget, UNKNOWN_PLAYER_TEXT)));
                } else {
                    lines.add(Text.translatable("item.noellesroles.forged_potion.no_target"));
                }
            }
        }
        return lines;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll > 0) {
            int nextOffset = this.scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP);
            nextOffset = Math.max(0, Math.min(nextOffset, this.maxScroll));
            if (nextOffset != this.scrollOffset) {
                this.scrollOffset = nextOffset;
                this.clearAndInit();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RoleScreenHelper.renderRoleMenuBackground(context, this.width, this.height, this.menuType.accentColor);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    public enum MenuType {
        VOODOO(0xFF000000 | (Noellesroles.VOODOO.color() & 0x00FFFFFF)),
        SWAPPER(0xFF000000 | (Noellesroles.SWAPPER.color() & 0x00FFFFFF)),
        MORPHLING(0xFF000000 | (Noellesroles.MORPHLING.color() & 0x00FFFFFF)),
        FORGED_POTION(0xFF000000 | (Noellesroles.MORPHLING.color() & 0x00FFFFFF));

        private final int accentColor;

        MenuType(int accentColor) {
            this.accentColor = accentColor;
        }

        private Text getTitle() {
            return switch (this) {
                case VOODOO -> Text.translatable("screen.role_target.voodoo.title");
                case SWAPPER -> Text.translatable("screen.role_target.swapper.title");
                case MORPHLING -> Text.translatable("screen.role_target.morphling.title");
                case FORGED_POTION -> Text.translatable("screen.role_target.forged_potion.title");
            };
        }

        private Text getEmptyText() {
            return switch (this) {
                case VOODOO -> Text.translatable("screen.role_target.voodoo.empty");
                case SWAPPER -> Text.translatable("screen.role_target.swapper.empty");
                case MORPHLING -> Text.translatable("screen.role_target.morphling.empty");
                case FORGED_POTION -> Text.translatable("screen.role_target.forged_potion.empty");
            };
        }
    }

    private record TargetEntry(UUID uuid, ShopEntry.Type backgroundType) {
    }

    private static final class TargetWidget extends MenuPlayerTargetWidget {
        private static final Text UNKNOWN_PLAYER_TEXT = Text.translatable("screen.role_target.unknown_player");

        private final TargetEntry target;
        private final boolean selected;

        private TargetWidget(int x, int y, TargetEntry target, boolean selected, Consumer<UUID> onSelected,
                             int clipLeft, int clipTop, int clipRight, int clipBottom) {
            super(x, y, RoleScreenHelper.getPlayerName(target.uuid(), UNKNOWN_PLAYER_TEXT),
                    button -> onSelected.accept(target.uuid()), clipLeft, clipTop, clipRight, clipBottom);
            this.target = target;
            this.selected = selected;
        }

        @Override
        protected net.minecraft.client.util.SkinTextures getSkinTextures() {
            return RoleScreenHelper.getPlayerSkinTextures(this.target.uuid());
        }

        @Override
        protected ShopEntry.Type getBackgroundType() {
            return this.target.backgroundType();
        }

        @Override
        protected Text getOverlayText() {
            return RoleScreenHelper.getPlayerName(this.target.uuid(), UNKNOWN_PLAYER_TEXT);
        }

        @Override
        protected boolean shouldHighlight() {
            return this.selected || super.shouldHighlight();
        }

        @Override
        protected int getHighlightColor() {
            return this.selected ? 0xAA4B1A8E : super.getHighlightColor();
        }
    }
}

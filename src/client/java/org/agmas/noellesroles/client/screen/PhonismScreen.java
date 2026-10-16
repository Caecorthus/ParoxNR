package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.phonism.PhonismSoundWidget;
import org.agmas.noellesroles.client.phonism.PhonismTargetWidget;
import org.agmas.noellesroles.client.screen.RoleScreenHelper.InteractionBlocker;
import org.agmas.noellesroles.phonism.PhonismSound;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 谎言音效师技能界面：阶段1选玩家，阶段2选音效
public class PhonismScreen extends Screen {
    private static final int ACCENT_BAR_COLOR = 0xFF000000 | (Noellesroles.PHONISM.color() & 0x00FFFFFF);
    private static final int SOUND_PHASE_EXTRA_DIM_COLOR = 0x90000000;
    private static final int SOUND_COLUMNS = 3;
    private static final int SOUND_GAP_X = 5;
    private static final int SOUND_GAP_Y = 5;
    private static final double SOUND_SCROLL_STEP = PhonismSoundWidget.HEIGHT + SOUND_GAP_Y;
    private static final int TARGET_COLUMNS = 6;
    private static final int TARGET_SPACING_X = 36;
    private static final int TARGET_SPACING_Y = 45;
    private static final double TARGET_SCROLL_STEP = 24.0;
    private static final Text UNKNOWN_PLAYER_TEXT = Text.translatable("screen.role_target.unknown_player");

    private final ClientPlayerEntity player;
    private final List<PhonismSoundWidget> soundWidgets = new ArrayList<>();
    private UUID selectedTarget = null;
    private ButtonWidget backButton = null;
    private ButtonWidget targetCloseButton = null;
    private int targetScrollOffset = 0;
    private int targetMaxScroll = 0;
    private int targetCount = 0;
    private int soundListStartX;
    private int soundListBaseY;
    private int soundViewportTop;
    private int soundViewportBottom;
    private int soundContentHeight;
    private double soundScrollOffset = 0.0;

    public PhonismScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.phonism.title.select_target"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        soundWidgets.clear();
        backButton = null;
        targetCloseButton = null;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(player);

        // 基础检查
        if (!gameWorld.isRole(player, Noellesroles.PHONISM) ||
                !GameFunctions.isPlayerPlayingAndAlive(player) ||
                SwallowedPlayerComponent.isPlayerSwallowed(player) ||
                abilityComp.getCooldown() > 0) {
            this.close();
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (selectedTarget == null) {
            // === 阶段1：选择幻听来源玩家 ===

            targetCloseButton = ButtonWidget.builder(Text.translatable("screen.phonism.button.close"), btn -> this.close())
                    .dimensions(centerX - 40, RoleScreenHelper.getMenuButtonY(this.height), 80, 20)
                    .build();
            addDrawableChild(targetCloseButton);

            // 死亡玩家同样可选，因此这里用全部玩家而不是存活玩家
            List<UUID> validPlayers = new ArrayList<>(gameWorld.getAllPlayers());
            validPlayers.remove(player.getUuid());
            List<UUID> targets = new ArrayList<>();
            for (UUID uuid : WatheClient.PLAYER_ENTRIES_CACHE.keySet()) {
                if (!validPlayers.contains(uuid)) {
                    continue;
                }
                if (SwallowedInteractionHelper.blocksPlayerTarget(
                        player.getWorld().getPlayerByUuid(uuid),
                        SwallowedInteractionHelper.TargetingRule.PHONISM_TARGET)) {
                    continue;
                }
                targets.add(uuid);
            }

            // 动态网格布局：每行最多 TARGET_COLUMNS 个人
            targetCount = targets.size();
            int rows = Math.max(1, (targets.size() + TARGET_COLUMNS - 1) / TARGET_COLUMNS);
            int contentHeight = rows * TARGET_SPACING_Y + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
            int viewTop = RoleScreenHelper.getMenuViewTop(this.height);
            int contentTop = RoleScreenHelper.getMenuContentTop(this.height);
            int viewBottom = RoleScreenHelper.getMenuViewBottom(this.height);
            int viewHeight = Math.max(1, viewBottom - viewTop);
            targetMaxScroll = Math.max(0, contentHeight - viewHeight);
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, targetMaxScroll));
            int startX = RoleScreenHelper.getGridStartX(targets.size(), TARGET_COLUMNS, TARGET_SPACING_X, centerX);
            int startY = contentTop - targetScrollOffset;
            InteractionBlocker closeButtonBlocker = RoleScreenHelper.getCenteredButtonBlocker(
                    centerX, RoleScreenHelper.getMenuButtonY(this.height), 80, 20);

            for (int i = 0; i < targets.size(); i++) {
                int row = i / TARGET_COLUMNS;
                int col = i % TARGET_COLUMNS;

                PhonismTargetWidget widget = new PhonismTargetWidget(
                        startX + col * TARGET_SPACING_X,
                        startY + row * TARGET_SPACING_Y,
                        targets.get(i),
                        selected -> {
                            this.selectedTarget = selected;
                            this.clearAndInit();
                        },
                        0, viewTop, this.width, viewBottom, closeButtonBlocker
                );
                widget.visible = RoleScreenHelper.intersectsPlayerWidgetFrame(widget.getX(), widget.getY(),
                        0, viewTop, this.width, viewBottom);
                addDrawableChild(widget);
            }
        } else {
            // === 阶段2：选择幻听内容 ===

            PhonismSound[] sounds = PhonismSound.values();
            int rows = (int) Math.ceil((double) sounds.length / SOUND_COLUMNS);
            int totalW = SOUND_COLUMNS * PhonismSoundWidget.WIDTH + (SOUND_COLUMNS - 1) * SOUND_GAP_X;

            soundListStartX = centerX - totalW / 2;
            soundViewportTop = Math.max(42, centerY - 74);
            int backButtonY = this.height - 42;
            soundViewportBottom = Math.max(soundViewportTop + PhonismSoundWidget.HEIGHT, backButtonY - 4);
            soundListBaseY = soundViewportTop;
            soundContentHeight = Math.max(0, rows * PhonismSoundWidget.HEIGHT + Math.max(0, rows - 1) * SOUND_GAP_Y);
            soundScrollOffset = Math.max(0.0, Math.min(soundScrollOffset, getMaxSoundScroll()));

            for (int i = 0; i < sounds.length; i++) {
                int col = i % SOUND_COLUMNS;
                int row = i / SOUND_COLUMNS;

                PhonismSoundWidget widget = new PhonismSoundWidget(
                        soundListStartX + col * (PhonismSoundWidget.WIDTH + SOUND_GAP_X),
                        soundListBaseY + row * (PhonismSoundWidget.HEIGHT + SOUND_GAP_Y),
                        sounds[i],
                        selectedTarget,
                        0, soundViewportTop, this.width, soundViewportBottom
                );
                soundWidgets.add(widget);
                addDrawableChild(widget);
            }

            // 返回按钮（回到阶段1重新选人）
            backButton = ButtonWidget.builder(Text.translatable("screen.phonism.button.back"), btn -> {
                        selectedTarget = null;
                        this.clearAndInit();
                    })
                    .dimensions(centerX - 40, backButtonY, 80, 20)
                    .build();
            addDrawableChild(backButton);
            updateSoundWidgetPositions();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        if (selectedTarget != null) {
            // 第二页会露出更多场景内容，额外补一层暗化，避免翻页后整体突然变亮。
            context.fill(
                    0,
                    RoleScreenHelper.MENU_BAR_HEIGHT,
                    this.width,
                    this.height - RoleScreenHelper.MENU_BAR_HEIGHT,
                    SOUND_PHASE_EXTRA_DIM_COLOR
            );
        }

        if (selectedTarget == null) {
            super.render(context, mouseX, mouseY, delta);
        } else {
            updateSoundWidgetPositions();
            context.enableScissor(0, soundViewportTop, this.width, soundViewportBottom);
            for (PhonismSoundWidget widget : soundWidgets) {
                if (widget.visible) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
            context.disableScissor();
            if (backButton != null) {
                backButton.render(context, mouseX, mouseY, delta);
            }
        }

        // 渲染文字提示
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (selectedTarget == null) {
            RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.phonism.title.select_target"), centerX, RoleScreenHelper.getMenuTitleY(centerY));
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.phonism.subtitle.click_to_confirm"), centerX, RoleScreenHelper.getMenuSubtitleY(centerY));
            if (this.targetCount == 0) {
                RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.phonism.empty"), centerX, centerY);
            }
        } else {
            String targetName = RoleScreenHelper.getPlayerName(selectedTarget, UNKNOWN_PLAYER_TEXT).getString();
            RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.phonism.title.select_sound", targetName), centerX, centerY - 118);
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.phonism.subtitle.select_sound"), centerX, centerY - 102);
        }

        if (selectedTarget == null) {
            RoleScreenHelper.renderTopmostPlayerOverlays(context, font, this.children());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedTarget == null && targetMaxScroll > 0) {
            double nextOffset = targetScrollOffset - verticalAmount * TARGET_SCROLL_STEP;
            targetScrollOffset = Math.max(0, Math.min((int) Math.round(nextOffset), targetMaxScroll));
            this.clearAndInit();
            return true;
        }
        if (selectedTarget != null && mouseY >= soundViewportTop && mouseY <= soundViewportBottom) {
            double nextOffset = soundScrollOffset - verticalAmount * SOUND_SCROLL_STEP;
            soundScrollOffset = Math.max(0.0, Math.min(nextOffset, getMaxSoundScroll()));
            updateSoundWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private void updateSoundWidgetPositions() {
        for (int i = 0; i < soundWidgets.size(); i++) {
            PhonismSoundWidget widget = soundWidgets.get(i);
            int col = i % SOUND_COLUMNS;
            int row = i / SOUND_COLUMNS;
            int x = soundListStartX + col * (PhonismSoundWidget.WIDTH + SOUND_GAP_X);
            int y = soundListBaseY + row * (PhonismSoundWidget.HEIGHT + SOUND_GAP_Y) - (int) Math.round(soundScrollOffset);
            widget.setPosition(x, y);
            widget.visible = RoleScreenHelper.intersectsRect(x, y, widget.getWidth(), widget.getHeight(),
                    0, soundViewportTop, this.width, soundViewportBottom);
        }
    }

    private double getMaxSoundScroll() {
        return Math.max(0, soundContentHeight - (soundViewportBottom - soundViewportTop));
    }
}

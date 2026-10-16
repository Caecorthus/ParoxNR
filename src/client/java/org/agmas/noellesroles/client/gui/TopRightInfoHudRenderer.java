package org.agmas.noellesroles.client.gui;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.config.datapack.MapRegistry;
import dev.doctor4t.wathe.config.datapack.MapRegistryEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.economist.EconomistClientState;
import org.agmas.noellesroles.client.taskpoint.TaskPointHighlightManager;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.deatharena.DeathArenaStateHelper;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;
import org.agmas.noellesroles.hallucination.HallucinationUiSlot;
import org.agmas.noellesroles.util.SpectatorStateHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 右上角金币下方的信息块：模式、当前地图、观战/玩法介绍/配置界面/任务点高亮按键提示。
 * 有金币时紧贴金币行（y18）下方，无金币时顶到右上角最上方（y6）；所有行白字右对齐。
 */
public final class TopRightInfoHudRenderer {
    private static final int RIGHT_PADDING = 12;
    private static final int TOP_MONEY = 18;
    private static final int TOP_PLAIN = 6;
    private static final int LINE_HEIGHT = 14;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int HIGHLIGHT_GREEN = 0xFF55FF55;
    private static final int COLLAPSE_TIP_BLUE = 0xFF55AAFF;
    private static final int COLLAPSED_HINT_GRAY = 0xFFAAAAAA;
    private static final String DEATH_ARENA_MODE_KEY = "gamemode.noellesroles.death_arena";

    private TopRightInfoHudRenderer() {
    }

    public static void render(DrawContext context, TextRenderer renderer) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!isInfoHudActive(gameWorld)) {
            return;
        }

        // 开局黑屏渐亮期间整块随黑屏同步渐入，全黑时跳过绘制
        float visibility = HudRenderHelper.getOpeningFadeVisibility();
        if (visibility <= 0f) {
            return;
        }

        int rightEdge = context.getScaledWindowWidth() - RIGHT_PADDING;
        int y = getTopY(player);
        boolean collapsed = NoellesRolesConfig.HANDLER.instance().collapseTopRightHints;
        for (Line line : buildLines(player, gameWorld, collapsed)) {
            context.drawTextWithShadow(renderer, line.text(), rightEdge - renderer.getWidth(line.text()), y,
                    HudRenderHelper.applyVisibility(line.color(), visibility));
            y += LINE_HEIGHT;
        }
    }

    /**
     * 信息块可见时返回其底部 y（含金币行占位），否则返回 -1。供状态效果图标避让使用。
     */
    public static int getHudBottomY() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            return -1;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!isInfoHudActive(gameWorld)) {
            return -1;
        }
        boolean collapsed = NoellesRolesConfig.HANDLER.instance().collapseTopRightHints;
        List<Line> lines = buildLines(player, gameWorld, collapsed);
        if (lines.isEmpty()) {
            return -1;
        }
        return getTopY(player) + lines.size() * LINE_HEIGHT;
    }

    private static boolean isInfoHudActive(GameWorldComponent gameWorld) {
        return gameWorld.isRunning() || NoellesrolesClient.isDeathArenaActiveForClientPlayer();
    }

    private static int getTopY(ClientPlayerEntity player) {
        if (!isMoneyRowOccupied(player)) {
            return TOP_PLAIN;
        }
        // 幻觉隐藏/替换金币时经济学家观测行不会被绘制（真金币渲染被取消，TAIL 注入不执行）
        boolean observationLineVisible = EconomistClientState.isObserving()
                && !HallucinationHudRenderer.shouldSuppressMoneyHud();
        return observationLineVisible
                ? TOP_MONEY + EconomistClientState.OBSERVATION_LINE_HEIGHT
                : TOP_MONEY;
    }

    /**
     * 与金币行实际渲染条件一致：训练 HUD 激活、无打开界面、CanSeeMoney 允许、金币未被幻觉隐藏。
     * 幻觉假金币场景下真金币被取消但假金币仍画在 y6，因此同样视为占位。
     */
    private static boolean isMoneyRowOccupied(ClientPlayerEntity player) {
        if (WatheClient.trainComponent == null || !WatheClient.trainComponent.hasHud()) {
            return false;
        }
        if (CanSeeMoney.EVENT.invoker().canSee(player) != CanSeeMoney.Result.ALLOW) {
            return false;
        }
        return !HallucinationPlayerComponent.KEY.get(player).isUiHidden(HallucinationUiSlot.MONEY);
    }

    private static List<Line> buildLines(ClientPlayerEntity player, GameWorldComponent gameWorld, boolean collapsed) {
        if (collapsed) {
            if (NoellesrolesClient.configScreenBind == null) {
                return List.of();
            }
            return List.of(new Line(Text.translatable("config_screen.hint_collapsed",
                    NoellesrolesClient.configScreenBind.getBoundKeyLocalizedText()), COLLAPSED_HINT_GRAY));
        }

        List<Line> lines = new ArrayList<>();
        Text modeName = getModeName(gameWorld);
        if (modeName != null) {
            lines.add(new Line(Text.translatable("hud.noellesroles.top_right.mode", modeName), WHITE));
        }
        Text mapName = getMapName(player, gameWorld);
        if (mapName != null) {
            lines.add(new Line(Text.translatable("hud.noellesroles.top_right.map", mapName), WHITE));
        }

        if (NoellesrolesClient.roleInfoBind != null) {
            lines.add(new Line(Text.translatable("role_info.hint",
                    NoellesrolesClient.roleInfoBind.getBoundKeyLocalizedText()), WHITE));
        }
        if (NoellesrolesClient.configScreenBind != null) {
            lines.add(new Line(Text.translatable("config_screen.hint_ingame",
                    NoellesrolesClient.configScreenBind.getBoundKeyLocalizedText()), WHITE));
        }
        // 死亡后（旁观）无法切换任务点高亮，该行由观战面板提示占据同一位置
        if (SpectatorStateHelper.isRealSpectator(player)) {
            if (NoellesrolesClient.assistInterfaceBind != null) {
                lines.add(new Line(Text.translatable("assist_interface.spectator_hint",
                        NoellesrolesClient.assistInterfaceBind.getBoundKeyLocalizedText()), WHITE));
            }
        } else if (NoellesrolesClient.taskPointHighlightBind != null) {
            Text boundKey = NoellesrolesClient.taskPointHighlightBind.getBoundKeyLocalizedText();
            if (TaskPointHighlightManager.isEnabled()) {
                lines.add(new Line(Text.translatable("tip.noellesroles.taskpoint.toggle_hint_on", boundKey),
                        HIGHLIGHT_GREEN));
            } else {
                lines.add(new Line(Text.translatable("tip.noellesroles.taskpoint.toggle_hint", boundKey), WHITE));
            }
        }
        if (NoellesrolesClient.configScreenBind != null) {
            lines.add(new Line(Text.translatable("config_screen.hint_collapse_tip",
                    NoellesrolesClient.configScreenBind.getBoundKeyLocalizedText()), COLLAPSE_TIP_BLUE));
        }
        return lines;
    }

    private static Text getModeName(GameWorldComponent gameWorld) {
        if (NoellesrolesClient.isDeathArenaActiveForClientPlayer()) {
            return Text.translatable(DEATH_ARENA_MODE_KEY);
        }
        GameMode gameMode = gameWorld.getGameMode();
        if (gameMode == null || gameMode.identifier == null) {
            return null;
        }
        Identifier id = gameMode.identifier;
        return Text.translatable("gamemode." + id.getNamespace() + "." + id.getPath());
    }

    private static Text getMapName(ClientPlayerEntity player, GameWorldComponent gameWorld) {
        if (NoellesrolesClient.isDeathArenaActiveForClientPlayer()) {
            MapRegistryEntry entry = MapRegistry.getInstance().getMap(DeathArenaStateHelper.getConfiguredMapId());
            return entry != null ? localizeMapName(entry.displayName()) : null;
        }

        MapVotingComponent voting = MapVotingComponent.KEY.get(player.getWorld().getScoreboard());
        List<MapVotingComponent.VotingMapEntry> maps = voting.getAvailableMaps();
        int selectedIndex = voting.getSelectedMapIndex();
        if (selectedIndex >= 0 && selectedIndex < maps.size()) {
            return localizeMapName(maps.get(selectedIndex).displayName());
        }
        Text fromLastSelected = findMapByDimension(maps, voting.getUnavailableMaps(), voting.getLastSelectedDimension());
        if (fromLastSelected != null) {
            return fromLastSelected;
        }
        return findMapByDimension(maps, voting.getUnavailableMaps(),
                player.getWorld().getRegistryKey().getValue());
    }

    private static Text findMapByDimension(List<MapVotingComponent.VotingMapEntry> maps,
                                           List<MapVotingComponent.UnavailableMapEntry> unavailableMaps,
                                           Identifier dimensionId) {
        if (dimensionId == null) {
            return null;
        }
        for (MapVotingComponent.VotingMapEntry entry : maps) {
            if (dimensionId.equals(entry.dimensionId())) {
                return localizeMapName(entry.displayName());
            }
        }
        for (MapVotingComponent.UnavailableMapEntry entry : unavailableMaps) {
            if (dimensionId.equals(entry.dimensionId())) {
                return localizeMapName(entry.displayName());
            }
        }
        return null;
    }

    private static Text localizeMapName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        if (displayName.startsWith("gui.wathe.") || displayName.startsWith("gamemode.")) {
            return Text.translatable(displayName);
        }
        return Text.literal(displayName);
    }

    private record Line(Text text, int color) {
    }
}

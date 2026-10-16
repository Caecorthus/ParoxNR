package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.packet.SpectatorAssistTeleportC2SPacket;
import org.agmas.noellesroles.packet.SpectatorInfoRequestC2SPacket;
import org.agmas.noellesroles.packet.SpectatorReplayDetailRequestC2SPacket;
import org.agmas.noellesroles.packet.SpectatorReplayDetailSyncS2CPacket;
import org.agmas.noellesroles.packet.SpectatorInfoSyncS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;

import java.util.*;

public class SpectatorAssistPanelScreen extends Screen {
    private static final int PADDING = 30;
    private static final int COLUMNS = 3;
    private static final int COLUMN_GAP = 6;
    private static final int ROW_HEIGHT = 30;
    private static final int AVATAR_SIZE = 16;
    private static final int HOVER_LINE_HEIGHT = 9;
    /** Wathe 金币字形 U+E781（与 StoreRenderer 绘制金币时使用的字符一致）。 */
    private static final String COIN_GLYPH = String.valueOf((char) 0xE781);

    private static long NEXT_REQUEST_ID = 1L;
    private static SpectatorAssistPanelScreen ACTIVE_INSTANCE;

    private final List<EntryData> entries = new ArrayList<>();
    private final Map<UUID, ServerSyncData> serverSyncByUuid = new HashMap<>();
    private final Map<UUID, ReplayDetailState> replayDetailsByUuid = new HashMap<>();
    private final Map<UUID, Long> pendingReplayDetailRequestIds = new HashMap<>();
    private final Map<UUID, Long> lastAppliedReplayDetailRequestIds = new HashMap<>();
    private int page = 0;
    private int pageCount = 1;
    private int entriesPerPage = COLUMNS;
    private long lastAppliedRequestId = -1L;

    public SpectatorAssistPanelScreen() {
        super(Text.translatable("screen.spectator_assist_panel.title"));
    }

    @Override
    protected void init() {
        super.init();
        ACTIVE_INSTANCE = this;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            close();
            return;
        }

        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!canOpenSpectatorPanel(client.player, gwc)) {
            close();
            return;
        }

        serverSyncByUuid.clear();
        replayDetailsByUuid.clear();
        pendingReplayDetailRequestIds.clear();
        lastAppliedReplayDetailRequestIds.clear();
        rebuildEntries(client, gwc);
        ClientPlayNetworking.send(new SpectatorInfoRequestC2SPacket(NEXT_REQUEST_ID++, -1L));

        int buttonY = this.height - 30;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.spectator_assist_panel.prev"), button -> {
                    if (page > 0) {
                        page--;
                    }
                })
                .dimensions(this.width / 2 - 95, buttonY, 90, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.spectator_assist_panel.next"), button -> {
                    if (page < pageCount - 1) {
                        page++;
                    }
                })
                .dimensions(this.width / 2 + 5, buttonY, 90, 20)
                .build());
    }

    private void rebuildEntries(MinecraftClient client, GameWorldComponent gwc) {
        entries.clear();

        List<UUID> sortedUuids = new ArrayList<>(serverSyncByUuid.keySet());
        sortedUuids.sort(this::compareSpectatorEntries);

        for (UUID uuid : sortedUuids) {
            PlayerListEntry playerEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
            Text nameText = resolveNameText(uuid, playerEntry);
            boolean online = client.world.getPlayerByUuid(uuid) != null;
            boolean dead = gwc.isPlayerDead(uuid);
            boolean self = client.player != null && client.player.getUuid().equals(uuid);
            ServerSyncData syncData = serverSyncByUuid.get(uuid);
            Text roleText = resolveRoleText(syncData);
            int roleColor = resolveRoleColor(syncData);
            Text deathReasonText = resolveDeathReasonText(gwc, uuid, dead, syncData);
            long latestRelevantReplayTick = resolveLatestRelevantReplayTick(syncData);
            Text replaySummary = resolveReplaySummary(syncData);
            Identifier skinTexture = playerEntry != null
                    ? playerEntry.getSkinTextures().texture()
                    : DefaultSkinHelper.getSkinTextures(new GameProfile(uuid, nameText.getString())).texture();
            int balance = syncData != null ? syncData.balance() : -1;
            entries.add(new EntryData(uuid, nameText, roleText, roleColor, online, dead, self, deathReasonText, latestRelevantReplayTick, replaySummary, skinTexture, balance));
        }

        Layout layout = getLayout();
        entriesPerPage = Math.max(COLUMNS, layout.rowsPerPage * COLUMNS);
        pageCount = Math.max(1, (entries.size() + entriesPerPage - 1) / entriesPerPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        super.render(context, mouseX, mouseY, delta);
        context.fillGradient(0, 0, this.width, 25, 0x55333355, 0x00333355);
        context.fillGradient(0, this.height - 25, this.width, this.height, 0x00333355, 0x55333355);

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int centerX = this.width / 2;
        Layout layout = getLayout();

        context.drawCenteredTextWithShadow(font, this.title, centerX, 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(font,
                Text.translatable("screen.spectator_assist_panel.subtitle"),
                centerX, 21, 0xFFD0D0D0);
        context.drawCenteredTextWithShadow(font,
                Text.translatable("screen.spectator_assist_panel.page", page + 1, pageCount),
                centerX, this.height - 52, 0xFFBBBBBB);

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(font,
                    Text.translatable("screen.spectator_assist_panel.empty"),
                    centerX, this.height / 2, 0xFFAAAAAA);
            return;
        }

        int start = page * entriesPerPage;
        int end = Math.min(entries.size(), start + entriesPerPage);
        List<Text> hoverTooltip = null;

        for (int i = start; i < end; i++) {
            EntryData entry = entries.get(i);
            int localIndex = i - start;
            int row = localIndex / COLUMNS;
            int col = localIndex % COLUMNS;
            int x = layout.startX + col * (layout.columnWidth + COLUMN_GAP);
            int y = layout.listTop + row * ROW_HEIGHT;
            int rowColor = (row & 1) == 0 ? 0x44222222 : 0x44333333;
            context.fill(x, y, x + layout.columnWidth, y + ROW_HEIGHT - 1, rowColor);

            int avatarX = x + 6;
            int avatarY = y + 6;
            int borderColor = 0xFF000000 | (entry.roleColor & 0x00FFFFFF);
            context.fill(avatarX - 2, avatarY - 2, avatarX + AVATAR_SIZE + 2, avatarY + AVATAR_SIZE + 2, borderColor);
            context.fill(avatarX - 1, avatarY - 1, avatarX + AVATAR_SIZE + 1, avatarY + AVATAR_SIZE + 1, 0xFF111111);
            PlayerSkinDrawer.draw(context, entry.skinTexture, avatarX, avatarY, AVATAR_SIZE);

            if (isInAvatar(mouseX, mouseY, avatarX, avatarY) && entry.online) {
                context.fill(avatarX - 1, avatarY - 1, avatarX + AVATAR_SIZE + 1, avatarY + AVATAR_SIZE + 1, 0x55FFFFFF);
            }

            int nameColor = entry.online ? 0xFFFFFFFF : 0xFF888888;
            int textX = avatarX + AVATAR_SIZE + 6;
            int textWidth = layout.columnWidth - (textX - x) - 4;
            context.drawTextWithShadow(font, Text.literal(font.trimToWidth(entry.nameText.getString(), textWidth)), textX, y + 4, nameColor);

            Text deadTag = Text.translatable("screen.spectator_assist_panel.dead_tag");
            Text selfTag = Text.translatable("screen.spectator_assist_panel.self_tag");
            String deadTagText = entry.dead ? deadTag.getString() : "";
            int deadTagWidth = entry.dead ? font.getWidth(deadTagText) : 0;
            String selfTagText = entry.self ? selfTag.getString() : "";
            int selfTagWidth = entry.self ? font.getWidth(selfTagText) : 0;
            int reservedTagWidth = 0;
            if (entry.dead) {
                reservedTagWidth += deadTagWidth + 2;
            }
            if (entry.self) {
                reservedTagWidth += selfTagWidth + 2;
            }
            int roleWidth = Math.max(0, textWidth - reservedTagWidth - coinReservedWidth(entry.balance));
            String roleLine = font.trimToWidth(entry.roleText.getString(), roleWidth);
            context.drawTextWithShadow(font, Text.literal(roleLine), textX, y + 15, entry.roleColor);

            if (entry.balance >= 0) {
                drawBalance(context, font, textX + textWidth, y + 15, entry.balance);
            }
            int tagX = textX + font.getWidth(roleLine) + 2;
            if (entry.dead) {
                context.drawTextWithShadow(font, deadTag, tagX, y + 15, 0xFFFF5555);
                tagX += deadTagWidth + 2;
            }
            if (entry.self) {
                context.drawTextWithShadow(font, selfTag, tagX, y + 15, 0xFFFFDD55);
            }

            if (hoverTooltip == null && entry.dead && isInRect(mouseX, mouseY, textX + font.getWidth(roleLine) + 2, y + 17, deadTagWidth)) {
                hoverTooltip = List.of(
                        Text.translatable("screen.spectator_assist_panel.death_reason_title"),
                        entry.deathReasonText
                );
            }

            int nameWidth = Math.min(textWidth, font.getWidth(font.trimToWidth(entry.nameText.getString(), textWidth)));
            if (hoverTooltip == null && isInRect(mouseX, mouseY, textX, y + 6, nameWidth)) {
                hoverTooltip = buildReplayTooltip(entry);
            }

            if (hoverTooltip == null && isInAvatar(mouseX, mouseY, avatarX, avatarY) && entry.online) {
                hoverTooltip = List.of(entry.nameText, Text.translatable("screen.spectator_assist_panel.avatar_tooltip"));
            }
        }

        if (hoverTooltip != null) {
            context.drawTooltip(font, hoverTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Layout layout = getLayout();
            int start = page * entriesPerPage;
            int end = Math.min(entries.size(), start + entriesPerPage);

            for (int i = start; i < end; i++) {
                EntryData entry = entries.get(i);
                int localIndex = i - start;
                int row = localIndex / COLUMNS;
                int col = localIndex % COLUMNS;
                int x = layout.startX + col * (layout.columnWidth + COLUMN_GAP);
                int y = layout.listTop + row * ROW_HEIGHT;
                int avatarX = x + 6;
                int avatarY = y + 6;
                if (isInAvatar((int) mouseX, (int) mouseY, avatarX, avatarY) && entry.online) {
                    ClientPlayNetworking.send(new SpectatorAssistTeleportC2SPacket(entry.uuid));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pageCount <= 1) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (verticalAmount < 0 && page < pageCount - 1) {
            page++;
            return true;
        }
        if (verticalAmount > 0 && page > 0) {
            page--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            close();
            return;
        }

        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!canOpenSpectatorPanel(client.player, gwc)) {
            close();
            return;
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
        if (ACTIVE_INSTANCE == this) {
            ACTIVE_INSTANCE = null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NoellesrolesClient.assistInterfaceBind != null
                && NoellesrolesClient.assistInterfaceBind.matchesKey(keyCode, scanCode)) {
            NoellesrolesClient.markAssistInterfaceKeyHandled();
            this.close();
            return true;
        }
        if (NoellesrolesClient.roleInfoBind != null
                && NoellesrolesClient.roleInfoBind.matchesKey(keyCode, scanCode)) {
            NoellesrolesClient.markRoleInfoKeyHandled();
            if (this.client != null) {
                this.client.setScreen(RoleInfoScreen.create());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isInAvatar(int mouseX, int mouseY, int avatarX, int avatarY) {
        return mouseX >= avatarX && mouseX < avatarX + AVATAR_SIZE
                && mouseY >= avatarY && mouseY < avatarY + AVATAR_SIZE;
    }

    private static boolean isInRect(int mouseX, int mouseY, int x, int y, int width) {
        return width > 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HOVER_LINE_HEIGHT;
    }

    /** 角色行右侧金币显示预留的宽度：金币图标 8 + 每位数字 8 + 与角色名 2 像素间距；没有金币系统（-1）时为 0。 */
    private static int coinReservedWidth(int balance) {
        return balance >= 0 ? 8 + String.valueOf(balance).length() * 8 + 2 : 0;
    }

    /**
     * 直接绘制金币数：Wathe 的金币字形 + 数字（数字向左展开、每位间隔 8 像素，与原版 HUD 排版一致）。
     * 面板每秒刷新一次，不做原版的滚动动画，避免读数跳动。
     */
    private static void drawBalance(DrawContext context, TextRenderer font, int rightEdge, int y, int balance) {
        context.drawTextWithShadow(font, COIN_GLYPH, rightEdge - 8, y, 0xFFFFFFFF);
        String digits = String.valueOf(balance);
        int digitX = rightEdge - 16;
        for (int i = digits.length() - 1; i >= 0; i--) {
            context.drawTextWithShadow(font, String.valueOf(digits.charAt(i)), digitX, y, 0xFFFFFFFF);
            digitX -= 8;
        }
    }

    private static boolean canOpenSpectatorPanel(ClientPlayerEntity player, GameWorldComponent gwc) {
        return SpectatorStateHelper.isInGameRealSpectator(player, gwc);
    }

    private String resolveSortName(UUID uuid) {
        PlayerListEntry playerEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (playerEntry != null && playerEntry.getDisplayName() != null) {
            return playerEntry.getDisplayName().getString();
        }
        if (playerEntry != null) {
            return playerEntry.getProfile().getName();
        }
        return uuid.toString();
    }

    private int compareSpectatorEntries(UUID leftUuid, UUID rightUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameWorldComponent gwc = client.player != null
                ? GameWorldComponent.KEY.get(client.player.getWorld())
                : null;

        boolean leftDead = gwc != null && gwc.isPlayerDead(leftUuid);
        boolean rightDead = gwc != null && gwc.isPlayerDead(rightUuid);
        if (leftDead != rightDead) {
            return Boolean.compare(leftDead, rightDead);
        }

        if (leftDead) {
            long leftDeathTick = resolveDeathTick(serverSyncByUuid.get(leftUuid));
            long rightDeathTick = resolveDeathTick(serverSyncByUuid.get(rightUuid));
            int deathTickCompare = Long.compare(leftDeathTick, rightDeathTick);
            if (deathTickCompare != 0) {
                return deathTickCompare;
            }
        }

        return String.CASE_INSENSITIVE_ORDER.compare(resolveSortName(leftUuid), resolveSortName(rightUuid));
    }

    private static Text resolveNameText(UUID uuid, PlayerListEntry playerEntry) {
        if (playerEntry != null && playerEntry.getDisplayName() != null) {
            return playerEntry.getDisplayName();
        }
        if (playerEntry != null) {
            return Text.literal(playerEntry.getProfile().getName());
        }
        return Text.literal(uuid.toString().substring(0, 8));
    }

    private static Text resolveRoleText(ServerSyncData syncData) {
        if (syncData == null || syncData.roleTranslationKey().isBlank()) {
            return Text.translatable("screen.spectator_assist_panel.role_unknown");
        }
        return Text.translatable(syncData.roleTranslationKey());
    }

    private static int resolveRoleColor(ServerSyncData syncData) {
        if (syncData == null) {
            return 0xFFAAAAAA;
        }
        return syncData.roleColor();
    }

    private static Text resolveDeathReasonText(GameWorldComponent gwc, UUID uuid, boolean dead, ServerSyncData syncData) {
        if (!dead) {
            return Text.empty();
        }

        if (syncData != null && !syncData.deathReasonRaw().isBlank()) {
            Text reason = convertDeathReason(syncData.deathReasonRaw());
            if (reason != null) {
                if (syncData.deathAgeSeconds() >= 0) {
                    return Text.translatable("screen.spectator_assist_panel.death_with_age", reason, syncData.deathAgeSeconds());
                }
                return reason;
            }
        }

        if (gwc.isPlayerDead(uuid)) {
            return Text.translatable("screen.spectator_assist_panel.death_reason_unknown");
        }
        return Text.empty();
    }

    private static Text convertDeathReason(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            return null;
        }

        if (rawReason.startsWith("death_reason.")) {
            Text translated = Text.translatable(rawReason);
            if (!translated.getString().equals(rawReason)) {
                return translated;
            }
        }

        String normalized = rawReason.toLowerCase();
        String path = normalized;
        String namespacedPath = null;

        Identifier id = Identifier.tryParse(normalized);
        if (id != null) {
            path = id.getPath();
            namespacedPath = id.getNamespace() + "." + id.getPath();
        } else {
            int colon = path.indexOf(':');
            if (colon >= 0 && colon + 1 < path.length()) {
                path = path.substring(colon + 1);
            }
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < path.length()) {
                path = path.substring(dot + 1);
            }
        }

        List<String> keys = new ArrayList<>();
        if (namespacedPath != null) {
            keys.add("death_reason." + namespacedPath);
        }
        keys.add("death_reason." + normalized.replace(':', '.'));
        keys.add("death_reason." + path);
        keys.add("death_reason.noellesroles." + path);

        for (String key : keys) {
            Text translated = Text.translatable(key);
            if (!translated.getString().equals(key)) {
                return translated;
            }
        }
        return null;
    }

    private static List<Text> resolveReplayLines(List<Text> replayLines) {
        if (replayLines == null || replayLines.isEmpty()) {
            return List.of();
        }
        List<Text> lines = new ArrayList<>();
        for (Text replayLine : replayLines) {
            if (replayLine != null && !replayLine.getString().isBlank()) {
                lines.add(replayLine);
            }
        }
        return lines;
    }

    public static void applyServerSync(SpectatorInfoSyncS2CPacket payload) {
        if (ACTIVE_INSTANCE != null) {
            ACTIVE_INSTANCE.applyServerSyncInternal(payload);
        }
    }

    public static void applyReplayDetailSync(SpectatorReplayDetailSyncS2CPacket payload) {
        if (ACTIVE_INSTANCE != null) {
            ACTIVE_INSTANCE.applyReplayDetailSyncInternal(payload);
        }
    }

    private void applyServerSyncInternal(SpectatorInfoSyncS2CPacket payload) {
        long requestId = payload.requestId();
        if (requestId < lastAppliedRequestId) {
            return;
        }
        lastAppliedRequestId = requestId;

        serverSyncByUuid.clear();
        for (SpectatorInfoSyncS2CPacket.Entry entry : payload.entries()) {
            serverSyncByUuid.put(entry.uuid(), new ServerSyncData(
                    entry.roleTranslationKey(),
                    entry.roleColor(),
                    entry.deathReasonRaw(),
                    entry.deathTick(),
                    entry.deathAgeSeconds(),
                    entry.latestRelevantReplayTick(),
                    deserializeReplayText(entry.replaySummary()),
                    entry.balance()
            ));
        }
        pruneReplayDetailCache();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!canOpenSpectatorPanel(client.player, gwc)) {
            close();
            return;
        }

        rebuildEntries(client, gwc);
    }

    private void applyReplayDetailSyncInternal(SpectatorReplayDetailSyncS2CPacket payload) {
        UUID targetUuid = payload.targetUuid();
        long requestId = payload.requestId();
        long pendingRequestIdForTarget = pendingReplayDetailRequestIds.getOrDefault(targetUuid, -1L);
        long lastAppliedRequestIdForTarget = lastAppliedReplayDetailRequestIds.getOrDefault(targetUuid, -1L);
        if (requestId < lastAppliedRequestIdForTarget || (pendingRequestIdForTarget >= 0 && requestId < pendingRequestIdForTarget)) {
            return;
        }

        lastAppliedReplayDetailRequestIds.put(targetUuid, requestId);
        pendingReplayDetailRequestIds.remove(targetUuid);

        ServerSyncData syncData = serverSyncByUuid.get(targetUuid);
        long versionTick = syncData != null ? syncData.latestRelevantReplayTick() : -1L;
        replayDetailsByUuid.put(targetUuid, new ReplayDetailState(versionTick, toTextLines(payload.replayLines()), false));
    }

    private List<Text> buildReplayTooltip(EntryData entry) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.translatable("screen.spectator_assist_panel.replay_title"));

        ReplayDetailState detailState = replayDetailsByUuid.get(entry.uuid);
        boolean hasSummary = entry.replaySummary != null && !entry.replaySummary.getString().isBlank();
        boolean hasReplayVersion = entry.latestRelevantReplayTick >= 0L;

        if (detailState != null && !detailState.pending() && detailState.versionTick() == entry.latestRelevantReplayTick) {
            List<Text> detailedLines = resolveReplayLines(detailState.replayLines());
            if (detailedLines.isEmpty()) {
                tooltip.add(Text.translatable("screen.spectator_assist_panel.replay_summary_none"));
            } else {
                tooltip.addAll(detailedLines);
            }
            return tooltip;
        }

        if (hasSummary && hasReplayVersion) {
            tooltip.add(entry.replaySummary);
            requestReplayDetails(entry.uuid, entry.latestRelevantReplayTick);
            tooltip.add(Text.translatable("screen.spectator_assist_panel.replay_loading"));
        } else {
            tooltip.add(Text.translatable("screen.spectator_assist_panel.replay_summary_none"));
        }
        return tooltip;
    }

    private void requestReplayDetails(UUID targetUuid, long versionTick) {
        if (versionTick < 0L) {
            return;
        }

        ReplayDetailState currentState = replayDetailsByUuid.get(targetUuid);
        if (currentState != null && currentState.pending() && currentState.versionTick() == versionTick) {
            return;
        }
        if (currentState != null && !currentState.pending() && currentState.versionTick() == versionTick) {
            return;
        }

        long requestId = NEXT_REQUEST_ID++;
        pendingReplayDetailRequestIds.put(targetUuid, requestId);
        replayDetailsByUuid.put(targetUuid, new ReplayDetailState(versionTick, List.of(), true));
        ClientPlayNetworking.send(new SpectatorReplayDetailRequestC2SPacket(requestId, targetUuid));
    }

    private void pruneReplayDetailCache() {
        Iterator<Map.Entry<UUID, ReplayDetailState>> iterator = replayDetailsByUuid.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ReplayDetailState> entry = iterator.next();
            ServerSyncData syncData = serverSyncByUuid.get(entry.getKey());
            long currentVersionTick = syncData != null ? syncData.latestRelevantReplayTick() : -1L;
            if (entry.getValue().versionTick() != currentVersionTick) {
                iterator.remove();
                pendingReplayDetailRequestIds.remove(entry.getKey());
                lastAppliedReplayDetailRequestIds.remove(entry.getKey());
            }
        }
    }

    private static long resolveLatestRelevantReplayTick(ServerSyncData syncData) {
        if (syncData == null) {
            return -1L;
        }
        return syncData.latestRelevantReplayTick();
    }

    private static Text resolveReplaySummary(ServerSyncData syncData) {
        if (syncData == null) {
            return Text.empty();
        }
        return syncData.replaySummary();
    }

    private static List<Text> toTextLines(List<String> replayLines) {
        if (replayLines == null || replayLines.isEmpty()) {
            return List.of();
        }
        List<Text> lines = new ArrayList<>();
        for (String replayLine : replayLines) {
            lines.add(deserializeReplayText(replayLine));
        }
        return lines;
    }

    // 服务端将回放行序列化为文本组件 JSON，客户端反序列化后按自身语言设置本地化
    private static Text deserializeReplayText(String json) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (json == null || json.isBlank()) {
            return Text.empty();
        }
        if (client.world != null) {
            try {
                Text parsed = Text.Serialization.fromJson(json, client.world.getRegistryManager());
                if (parsed != null) {
                    return parsed;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return Text.literal(json);
    }

    private static long resolveDeathTick(ServerSyncData syncData) {
        if (syncData == null) {
            return Long.MAX_VALUE;
        }
        long deathTick = syncData.deathTick();
        return deathTick >= 0L ? deathTick : Long.MAX_VALUE;
    }

    private Layout getLayout() {
        int contentWidth = Math.min(this.width - PADDING * 2, 540);
        int listTop = 34;
        int listBottom = this.height - 60;
        int rowsPerPage = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        int columnWidth = (contentWidth - (COLUMNS - 1) * COLUMN_GAP) / COLUMNS;
        int startX = this.width / 2 - contentWidth / 2;
        return new Layout(startX, listTop, rowsPerPage, columnWidth);
    }

    private record EntryData(UUID uuid, Text nameText, Text roleText, int roleColor, boolean online, boolean dead, boolean self,
                             Text deathReasonText,
                             long latestRelevantReplayTick,
                             Text replaySummary,
                             Identifier skinTexture,
                             int balance) {
    }

    private record Layout(int startX, int listTop, int rowsPerPage, int columnWidth) {
    }

    private record ServerSyncData(String roleTranslationKey, int roleColor, String deathReasonRaw, long deathTick, int deathAgeSeconds, long latestRelevantReplayTick, Text replaySummary, int balance) {
    }

    private record ReplayDetailState(long versionTick, List<Text> replayLines, boolean pending) {
    }
}


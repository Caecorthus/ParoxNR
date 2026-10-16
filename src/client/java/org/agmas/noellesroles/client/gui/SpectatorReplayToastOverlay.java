package org.agmas.noellesroles.client.gui;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.packet.SpectatorInfoSyncS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SpectatorReplayToastOverlay {
    private static final long TOAST_DURATION_MS = 3000L;
    private static final int SCREEN_MARGIN = 5;
    private static final int STACK_SPACING = 2;
    private static final int MAX_TOASTS = 6;

    private static final List<ActiveToast> ACTIVE_TOASTS = new ArrayList<>();
    private static long lastSeenReplayTick = -1L;

    private static boolean awaitingSessionBaseline = true;
    private static long activeMatchStartTick = Long.MIN_VALUE;
    private static boolean renderedViaVoiceChatThisFrame;

    private SpectatorReplayToastOverlay() {
    }

    public static void beginHudFrame() {
        renderedViaVoiceChatThisFrame = false;
    }

    public static void markVoiceChatRendered() {
        renderedViaVoiceChatThisFrame = true;
    }

    public static boolean wasRenderedViaVoiceChatThisFrame() {
        return renderedViaVoiceChatThisFrame;
    }

    public static long getLastSeenReplayTick() {
        return lastSeenReplayTick;
    }

    public static void beginSpectatorSession() {
        ACTIVE_TOASTS.clear();
        lastSeenReplayTick = -1L;
        awaitingSessionBaseline = true;
        activeMatchStartTick = Long.MIN_VALUE;
    }

    public static void onSpectatorSync(SpectatorInfoSyncS2CPacket payload) {
        if (payload == null) {
            return;
        }

        if (activeMatchStartTick != payload.matchStartTick()) {
            ACTIVE_TOASTS.clear();
            lastSeenReplayTick = payload.latestReplayTick();
            awaitingSessionBaseline = false;
            activeMatchStartTick = payload.matchStartTick();
            return;
        }

        if (awaitingSessionBaseline) {
            lastSeenReplayTick = payload.latestReplayTick();
            awaitingSessionBaseline = false;
            return;
        }

        if (payload.replayToasts().isEmpty()) {
            lastSeenReplayTick = Math.max(lastSeenReplayTick, payload.latestReplayTick());
            return;
        }

        long now = System.currentTimeMillis();
        for (SpectatorInfoSyncS2CPacket.ReplayToast replayToast : payload.replayToasts()) {
            if (replayToast.worldTick() <= lastSeenReplayTick) {
                continue;
            }
            Text toastText = buildToastText(replayToast);
            ACTIVE_TOASTS.add(new ActiveToast(toastText, now + TOAST_DURATION_MS));
            if (ACTIVE_TOASTS.size() > MAX_TOASTS) {
                ACTIVE_TOASTS.remove(0);
            }
        }

        lastSeenReplayTick = Math.max(lastSeenReplayTick, payload.latestReplayTick());
    }

    public static void render(DrawContext context, TextRenderer font) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            ACTIVE_TOASTS.clear();
            return;
        }

        GameWorldComponent gwc = GameWorldComponent.KEY.get(player.getWorld());
        boolean isInGameSpectator = SpectatorStateHelper.isInGameRealSpectator(player, gwc);
        if (!isInGameSpectator) {
            ACTIVE_TOASTS.clear();
            return;
        }
        if (ACTIVE_TOASTS.isEmpty()) {
            return;
        }

        pruneExpired(System.currentTimeMillis());
        if (ACTIVE_TOASTS.isEmpty()) {
            return;
        }

        int rightX = context.getScaledWindowWidth() - SCREEN_MARGIN;
        int y = context.getScaledWindowHeight() - SCREEN_MARGIN;

        HudRenderHelper.pushAboveVoiceChatHudLayer(context);
        for (int i = ACTIVE_TOASTS.size() - 1; i >= 0; i--) {
            ActiveToast toast = ACTIVE_TOASTS.get(i);
            int width = font.getWidth(toast.text());
            y -= font.fontHeight;
            int x = rightX - width;
            context.drawTextWithShadow(font, toast.text(), x, y, 0xFFFFFFFF);
            y -= STACK_SPACING;
        }
        HudRenderHelper.popAboveVoiceChatHudLayer(context);
    }

    private static void pruneExpired(long now) {
        Iterator<ActiveToast> iterator = ACTIVE_TOASTS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expireAtMs() <= now) {
                iterator.remove();
            }
        }
    }

    private static Text buildToastText(SpectatorInfoSyncS2CPacket.ReplayToast replayToast) {
        Text targetRole = Text.translatable(replayToast.targetRoleKey());
        Text reason = resolveDeathReasonText(replayToast.deathReasonRaw());
        if (replayToast.actorRoleKey() == null || replayToast.actorRoleKey().isBlank()) {
            return Text.translatable("screen.spectator_assist_panel.toast.death", targetRole, reason);
        }

        Text actorRole = Text.translatable(replayToast.actorRoleKey());
        return Text.translatable("screen.spectator_assist_panel.toast.kill", actorRole, reason, targetRole);
    }

    private static Text resolveDeathReasonText(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            return Text.translatable("screen.spectator_assist_panel.death_reason_unknown");
        }

        if (rawReason.startsWith("death_reason.")) {
            Text translated = Text.translatable(rawReason);
            if (!translated.getString().equals(rawReason)) {
                return translated;
            }
        }

        String normalized = rawReason.toLowerCase();
        Identifier id = Identifier.tryParse(normalized);
        if (id != null) {
            String key = "death_reason." + id.getNamespace() + "." + id.getPath();
            Text translated = Text.translatable(key);
            if (!translated.getString().equals(key)) {
                return translated;
            }
        }

        Text direct = Text.translatable("death_reason." + normalized.replace(':', '.'));
        if (!direct.getString().equals("death_reason." + normalized.replace(':', '.'))) {
            return direct;
        }

        return Text.translatable("screen.spectator_assist_panel.death_reason_unknown");
    }

    private record ActiveToast(Text text, long expireAtMs) {
    }
}


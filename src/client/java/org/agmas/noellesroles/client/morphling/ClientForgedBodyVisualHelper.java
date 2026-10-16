package org.agmas.noellesroles.client.morphling;

import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.morphling.BodyForgeryWorldComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 赝造药水伪装尸体的客户端解析：本机视角下该显示哪个皮肤（理智正常的验尸官看破）。 */
public final class ClientForgedBodyVisualHelper {
    private ClientForgedBodyVisualHelper() {
    }

    public static @Nullable SkinTextures getForgedSkinTextures(PlayerBodyEntity body) {
        UUID fakeSkinUuid = getForgedSkinUuid(body);
        if (fakeSkinUuid == null) {
            return null;
        }
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(fakeSkinUuid);
        return entry != null ? entry.getSkinTextures() : null;
    }

    public static @Nullable UUID getForgedSkinUuid(PlayerBodyEntity body) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || viewer.getWorld() == null) {
            return null;
        }
        UUID fakeSkinUuid = BodyForgeryWorldComponent.KEY.get(viewer.getWorld()).getForgedSkin(body.getUuid());
        if (fakeSkinUuid == null || viewerSeesThroughForgery(viewer)) {
            return null;
        }
        return fakeSkinUuid;
    }

    /** 理智正常的验尸官看破赝造（与 CanSeeBodyRole 的判定一致）。 */
    public static boolean viewerSeesThroughForgery(ClientPlayerEntity viewer) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
        if (!gameWorld.isRole(viewer, Noellesroles.CORONER)) {
            return false;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(viewer) || SwallowedPlayerComponent.isPlayerSwallowed(viewer)) {
            return false;
        }
        return WatheClient.moodComponent != null && !WatheClient.moodComponent.isLowerThanMid();
    }
}

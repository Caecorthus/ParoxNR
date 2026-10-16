package org.agmas.noellesroles.client.util;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.gui.HallucinationHudRenderer;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;

/**
 * 饕餮玩家指示器（病原体同款罗盘）的客户端状态，按键2 开关。
 */
public final class TaotieIndicatorClientHelper {
    private static boolean indicatorEnabled = true;

    private TaotieIndicatorClientHelper() {}

    /** 累计吞噬人数达到 ⌊开局人数/6⌋ 后解锁。 */
    public static boolean isUnlocked(TaotiePlayerComponent comp) {
        int totalPlayers = comp.getTotalPlayersAtStart();
        return totalPlayers > 0 && comp.getTotalSwallowedCount() >= totalPlayers / 6;
    }

    public static boolean isEnabled() {
        return indicatorEnabled;
    }

    public static void toggle() {
        indicatorEnabled = !indicatorEnabled;
    }

    /** 罗盘开启、已解锁，且玩家存活、UI 未被隐匿时为激活状态。 */
    private static boolean isCompassActive(ClientPlayerEntity localPlayer, TaotiePlayerComponent comp) {
        return indicatorEnabled
                && isUnlocked(comp)
                && GameFunctions.isPlayerPlayingAndAlive(localPlayer)
                && !HallucinationHudRenderer.shouldSuppressSkillHintHud();
    }

    /** 罗盘激活时，饕餮时刻每秒倒计时广播不再占用 action bar。 */
    public static boolean shouldSuppressMomentCountdown(Text message) {
        if (!(message.getContent() instanceof TranslatableTextContent content)
                || !"tip.noellesroles.taotie_moment_countdown".equals(content.getKey())) {
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.TAOTIE)
                && isCompassActive(player, TaotiePlayerComponent.KEY.get(player));
    }

    public static void tick(ClientPlayerEntity localPlayer) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(localPlayer.getWorld());
        if (!gameWorld.isRole(localPlayer, Noellesroles.TAOTIE)) {
            indicatorEnabled = true;
            return;
        }

        TaotiePlayerComponent comp = TaotiePlayerComponent.KEY.get(localPlayer);
        if (!isUnlocked(comp)) {
            // 未解锁时恢复默认开启，保证新对局重新获得指示器时是开着的
            indicatorEnabled = true;
            return;
        }

        // 关闭、死亡或 UI 被隐匿时既不显示罗盘，也不覆盖 action bar
        if (!isCompassActive(localPlayer, comp)) {
            return;
        }

        PlayerEntity target = PlayerCompassHelper.findNearest(localPlayer, player ->
                !SpectatorStateHelper.isSpectatorLike(player)
                        && !player.isCreative()
                        && gameWorld.hasAnyRole(player)
                        && GameFunctions.isPlayerPlayingAndAlive(player)
                        && !SwallowedPlayerComponent.isPlayerSwallowed(player));

        Text actionBarText;
        if (target != null) {
            double distance = Math.sqrt(localPlayer.squaredDistanceTo(target));
            String direction = PlayerCompassHelper.directionArrow(localPlayer, target);
            String vertical = PlayerCompassHelper.verticalArrow(localPlayer, target);
            String verticalText = vertical.isEmpty() ? "" : " " + vertical;
            actionBarText = Text.translatable("tip.taotie.compass",
                    direction, (int) Math.round(distance), verticalText);
        } else {
            actionBarText = Text.translatable("tip.taotie.no_target");
        }

        // 在客户端 tick 内写入（先于同帧 HUD 渲染），确保罗盘稳定压过服务端每秒广播的饕餮时刻倒计时
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(actionBarText, false);
    }
}

package org.agmas.noellesroles.client.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 玩家罗盘（病原体/饕餮指示器）共用的目标搜索与方向箭头计算。
 */
public final class PlayerCompassHelper {
    private PlayerCompassHelper() {}

    @Nullable
    public static PlayerEntity findNearest(ClientPlayerEntity localPlayer, Predicate<PlayerEntity> predicate) {
        PlayerEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (PlayerEntity player : localPlayer.getWorld().getPlayers()) {
            if (player.equals(localPlayer)) continue;
            if (!predicate.test(player)) continue;
            double distance = localPlayer.squaredDistanceTo(player);
            if (distance < nearestDistanceSquared) {
                nearestDistanceSquared = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    /** 目标相对玩家视角的 8 方向箭头。 */
    public static String directionArrow(ClientPlayerEntity localPlayer, PlayerEntity target) {
        double dx = target.getX() - localPlayer.getX();
        double dz = target.getZ() - localPlayer.getZ();

        double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = localPlayer.getYaw() % 360;
        if (playerYaw < 0) playerYaw += 360;
        if (targetAngle < 0) targetAngle += 360;

        double relativeAngle = targetAngle - playerYaw;
        if (relativeAngle < -180) relativeAngle += 360;
        if (relativeAngle > 180) relativeAngle -= 360;

        if (relativeAngle >= -22.5 && relativeAngle < 22.5) {
            return "↑";
        } else if (relativeAngle >= 22.5 && relativeAngle < 67.5) {
            return "↗";
        } else if (relativeAngle >= 67.5 && relativeAngle < 112.5) {
            return "→";
        } else if (relativeAngle >= 112.5 && relativeAngle < 157.5) {
            return "↘";
        } else if (relativeAngle >= 157.5 || relativeAngle < -157.5) {
            return "↓";
        } else if (relativeAngle >= -157.5 && relativeAngle < -112.5) {
            return "↙";
        } else if (relativeAngle >= -112.5 && relativeAngle < -67.5) {
            return "←";
        } else {
            return "↖";
        }
    }

    /** 高度差超过 2 格时给出上/下箭头，否则为空。 */
    public static String verticalArrow(ClientPlayerEntity localPlayer, PlayerEntity target) {
        double dy = target.getY() - localPlayer.getY();
        if (dy > 2) return "↑";
        if (dy < -2) return "↓";
        return "";
    }
}

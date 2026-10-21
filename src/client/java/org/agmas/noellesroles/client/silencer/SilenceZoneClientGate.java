package org.agmas.noellesroles.client.silencer;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.silencer.SilenceZonePlayerComponent;

/**
 * 寂静装置无声区域的客户端声音闸门：
 * 本地玩家处于任意激活区域内时，屏蔽一切经由 SoundManager 播放的声音。
 * 结果按客户端世界 tick 缓存，避免每个声音实例都遍历玩家列表。
 */
public final class SilenceZoneClientGate {
    private SilenceZoneClientGate() {}

    private static long cachedWorldTime = Long.MIN_VALUE;
    private static boolean cachedResult = false;

    public static boolean shouldSuppressAllSounds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;
        long worldTime = client.world.getTime();
        if (worldTime != cachedWorldTime) {
            cachedWorldTime = worldTime;
            cachedResult = SilenceZonePlayerComponent.isPlayerInSilenceZone(client.player);
        }
        return cachedResult;
    }
}

package org.agmas.noellesroles.client.util;

import net.minecraft.client.MinecraftClient;

/**
 * 定身/冻结期间强制松开移动按键。客户端层面的防呆，服务端另有拦截兜底；
 * 松开后玩家仍可转动视角与切槽。
 */
public final class PlayerInputFreezeHelper {

    private PlayerInputFreezeHelper() {
    }

    public static void releaseMovementKeys(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
    }
}

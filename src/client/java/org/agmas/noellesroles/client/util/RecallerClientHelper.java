package org.agmas.noellesroles.client.util;

import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;

/**
 * 回溯者锚点可见性判定：仅回溯碎片持有者可见（背包或副手有碎片即可）。
 * 定案：严格按用户决定，不给观战者开后门。
 */
public final class RecallerClientHelper {
    private RecallerClientHelper() {
    }

    /** 镜像 VoodooShopHandler.hasDoll：主背包全扫 + 副手。 */
    public static boolean holdsRewindShard(PlayerEntity player) {
        return contains(player, ModItems.REWIND_SHARD);
    }

    /** HUD 提示行优先级用：是否持有锚点物品。 */
    public static boolean holdsAnchorItem(PlayerEntity player) {
        return contains(player, ModItems.RECALLER_ANCHOR);
    }

    public static boolean canSeeAnchor(PlayerEntity player) {
        return holdsRewindShard(player);
    }

    private static boolean contains(PlayerEntity player, net.minecraft.item.Item item) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(item)) {
                return true;
            }
        }
        return player.getOffHandStack().isOf(item);
    }
}

package org.agmas.noellesroles.recaller;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 回溯者商店：锚点（50 金币，限购至“不再持有锚点物品”）与回溯碎片（75 金币，不限购）。
 * 条目 id 一旦上线不要再改（库存/冷却按 id 记账）。
 * 不调 clearEntries()：无辜者条目表本来为空；避免双角色组合下误清杀手商店。
 */
public final class RecallerShopHandler {
    public static final String ANCHOR_ENTRY_ID = "recaller_anchor";
    public static final String SHARD_ENTRY_ID = "rewind_shard";
    public static final int ANCHOR_PRICE = 50;
    public static final int SHARD_PRICE = 75;
    private static final String ANCHOR_OWNED_DENY_KEY = "shop.error.noellesroles.recaller.anchor_owned";

    private RecallerShopHandler() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.RECALLER)) {
                return;
            }
            context.addEntry(new ShopEntry.Builder(
                    ANCHOR_ENTRY_ID,
                    ModItems.RECALLER_ANCHOR.getDefaultStack(),
                    ANCHOR_PRICE,
                    ShopEntry.Type.TOOL
            ).build());
            context.addEntry(new ShopEntry.Builder(
                    SHARD_ENTRY_ID,
                    ModItems.REWIND_SHARD.getDefaultStack(),
                    SHARD_PRICE,
                    ShopEntry.Type.TOOL
            ).build());
        });
    }

    /** 供 ShopPurchase.BEFORE 中央链调用：已持有锚点物品时拒购锚点（镜像 VoodooShopHandler.getDuplicateDenyReason）。 */
    public static String getAnchorDenyReason(PlayerEntity player, ShopEntry entry) {
        if (entry == null || !entry.stack().isOf(ModItems.RECALLER_ANCHOR)) {
            return null;
        }
        return hasAnchor(player) ? ANCHOR_OWNED_DENY_KEY : null;
    }

    /** 镜像 VoodooShopHandler.hasDoll：主背包全扫 + 副手。 */
    private static boolean hasAnchor(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.RECALLER_ANCHOR)) {
                return true;
            }
        }
        return player.getOffHandStack().isOf(ModItems.RECALLER_ANCHOR);
    }
}

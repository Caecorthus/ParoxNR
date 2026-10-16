package org.agmas.noellesroles.voodoo;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/** 巫毒师商店：只出售巫毒娃娃，价格随购买次数递增，封顶 200；已持有娃娃时不可重复购买。 */
public final class VoodooShopHandler {
    private static final String ENTRY_ID = "voodoo_doll";
    private static final int BASE_PRICE = 50;
    private static final int PRICE_STEP = 50;
    private static final int MAX_PRICE = 200;
    private static final String DUPLICATE_DENY_KEY = "shop.error.noellesroles.voodoo.already_owned";

    private VoodooShopHandler() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.VOODOO)) {
                return;
            }
            context.clearEntries();
            int purchases = VoodooPlayerComponent.KEY.get(player).getDollPurchases();
            int price = Math.min(BASE_PRICE + purchases * PRICE_STEP, MAX_PRICE);
            context.addEntry(new ShopEntry.Builder(
                    ENTRY_ID,
                    ModItems.VOODOO_DOLL.getDefaultStack(),
                    price,
                    ShopEntry.Type.TOOL
            ).onBuy(VoodooShopHandler::buyDoll).build());
        });
    }

    private static boolean buyDoll(PlayerEntity player) {
        if (!ShopEntry.insertStackInFreeSlot(player, ModItems.VOODOO_DOLL.getDefaultStack())) {
            return false;
        }
        VoodooPlayerComponent.KEY.get(player).addDollPurchase();
        return true;
    }

    /** 已持有巫毒娃娃（主背包或副手）时拒绝重复购买。 */
    public static String getDuplicateDenyReason(PlayerEntity player, ShopEntry entry) {
        if (entry == null || !entry.stack().isOf(ModItems.VOODOO_DOLL)) {
            return null;
        }
        return hasDoll(player) ? DUPLICATE_DENY_KEY : null;
    }

    private static boolean hasDoll(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.VOODOO_DOLL)) {
                return true;
            }
        }
        return player.getOffHandStack().isOf(ModItems.VOODOO_DOLL);
    }
}

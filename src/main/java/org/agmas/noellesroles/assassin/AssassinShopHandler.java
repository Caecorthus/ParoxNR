package org.agmas.noellesroles.assassin;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/** 刺客商店：在通用杀手商店基础上追加刺客之书（100 金币，不限购）。 */
public class AssassinShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.ASSASSIN)) {
                return;
            }
            context.addEntry(new ShopEntry.Builder(
                    "assassin_book",
                    ModItems.ASSASSIN_BOOK.getDefaultStack(),
                    100,
                    ShopEntry.Type.TOOL
            ).build());
        });
    }
}

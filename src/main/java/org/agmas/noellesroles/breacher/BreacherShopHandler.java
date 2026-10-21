package org.agmas.noellesroles.breacher;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.Noellesroles;

/**
 * 破袭者商店价格调整：左轮 270、手雷 300、疯魔 420，其余保持杀手商店默认。
 */
public class BreacherShopHandler {

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.BREACHER)) {
                return;
            }

            for (int i = 0; i < context.getEntries().size(); i++) {
                ShopEntry entry = context.getEntries().get(i);
                switch (entry.id()) {
                    case "revolver" -> context.setEntry(i,
                            new ShopEntry.Builder("revolver", WatheItems.REVOLVER.getDefaultStack(), 270, ShopEntry.Type.WEAPON).build());
                    case "grenade" -> context.setEntry(i,
                            new ShopEntry.Builder("grenade", WatheItems.GRENADE.getDefaultStack(), 300, ShopEntry.Type.WEAPON).build());
                    case "psycho_mode" -> context.setEntry(i,
                            new ShopEntry.Builder("psycho_mode", WatheItems.PSYCHO_MODE.getDefaultStack(), 420, ShopEntry.Type.WEAPON)
                                    .cooldown(GameConstants.getInTicks(5, 0))
                                    .onBuy(PlayerShopComponent::usePsychoMode)
                                    .build());
                    default -> {
                    }
                }
            }
        });
    }
}

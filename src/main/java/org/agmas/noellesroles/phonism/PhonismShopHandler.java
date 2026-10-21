package org.agmas.noellesroles.phonism;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 谎言音效师商店处理器：追加声音收集器（50 金币，每局限购一次）。
 * 右键安装于视野 5 格内玩家，再右键开关监听中继，空手右键持有者/尸体回收。
 */
public class PhonismShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.PHONISM)) {
                context.addEntry(new ShopEntry.Builder("sound_collector",
                        ModItems.SOUND_COLLECTOR.getDefaultStack(), 50, ShopEntry.Type.TOOL)
                        .stock(1)
                        .build());
            }
        });
    }
}

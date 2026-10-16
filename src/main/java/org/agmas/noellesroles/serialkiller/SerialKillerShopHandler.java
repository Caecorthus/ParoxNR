package org.agmas.noellesroles.serialkiller;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.Noellesroles;

import java.util.Set;

/**
 * 连环杀手商店处理器
 * - 商店只出售：刀、开锁器、撬棍、疯魔模式、裹尸袋、关灯
 * - 疯魔模式价格由 350 下调至 300
 */
public class SerialKillerShopHandler {
    private static final Set<String> ALLOWED_IDS = Set.of("knife", "lockpick", "crowbar", "psycho_mode", "body_bag", "blackout");

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.SERIAL_KILLER)) {
                // 移除默认杀手商店中的其余商品（枪、手雷、毒药、蝎子、便签、鞭炮）
                context.getEntries().removeIf(entry -> !ALLOWED_IDS.contains(entry.id()));

                // 疯魔模式价格改为300，其余行为（即时进入疯魔、5分钟购买冷却）与默认一致
                for (int i = 0; i < context.getEntries().size(); i++) {
                    if (context.getEntries().get(i).id().equals("psycho_mode")) {
                        context.setEntry(i, new ShopEntry.Builder("psycho_mode", WatheItems.PSYCHO_MODE.getDefaultStack(), 300, ShopEntry.Type.WEAPON)
                                .cooldown(GameConstants.getInTicks(5, 0))
                                .onBuy(PlayerShopComponent::usePsychoMode)
                                .build());
                        break;
                    }
                }
            }
        });
    }
}

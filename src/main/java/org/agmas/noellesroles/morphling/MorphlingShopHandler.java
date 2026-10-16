package org.agmas.noellesroles.morphling;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 变形者商店处理器
 * - 「万千变化」150：购买后 15 秒内 15 格范围实时皮肤乱序光环，购买冷却 45 秒
 * - 「赝造药水」75：对尸体使用，把尸体皮肤伪装成指定玩家
 */
public class MorphlingShopHandler {
    private static final String MYRIAD_ENTRY_ID = "morphling_myriad_changes";
    private static final int MYRIAD_PRICE = 150;
    private static final String POTION_ENTRY_ID = "morphling_forged_potion";
    private static final int POTION_PRICE = 75;

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.MORPHLING)) {
                return;
            }

            context.addEntry(0, new ShopEntry.Builder(POTION_ENTRY_ID,
                    ModItems.FORGED_POTION.getDefaultStack(), POTION_PRICE, ShopEntry.Type.TOOL)
                    .build());
            context.addEntry(0, new ShopEntry.Builder(MYRIAD_ENTRY_ID,
                    ModItems.MYRIAD_CHANGES.getDefaultStack(), MYRIAD_PRICE, ShopEntry.Type.TOOL)
                    .cooldown(GameConstants.getInTicks(0, 45))
                    .onBuy(MorphlingShopHandler::buyMyriadChanges)
                    .build());
        });
    }

    private static boolean buyMyriadChanges(PlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.MORPHLING)
                || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        MorphlingScramblePlayerComponent.KEY.get(player).startMyriad();
        return true;
    }
}

package org.agmas.noellesroles.dreameater;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 噬梦者商店：继承普通杀手商店，移除疯魔模式与毒药，蝎子降价为50，
 * 新增安眠药、3x蝎子与降帷。
 */
public class DreamEaterShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.DREAM_EATER)) return;

            context.getEntries().removeIf(entry ->
                    entry.id().equals("psycho_mode") || entry.id().equals("poison_vial"));

            int scorpionIndex = -1;
            for (int i = 0; i < context.getEntries().size(); i++) {
                if (context.getEntries().get(i).id().equals("scorpion")) {
                    scorpionIndex = i;
                    break;
                }
            }
            if (scorpionIndex >= 0) {
                context.setEntry(scorpionIndex,
                        new ShopEntry.Builder("scorpion", WatheItems.SCORPION.getDefaultStack(), 50, ShopEntry.Type.POISON).build());
                context.addEntry(scorpionIndex + 1,
                        new ShopEntry.Builder("sleeping_pill", ModItems.SLEEPING_PILL.getDefaultStack(), 50, ShopEntry.Type.POISON).build());
                context.addEntry(scorpionIndex + 2,
                        new ShopEntry.Builder("scorpion_x3", ModItems.SCORPION_X3.getDefaultStack(), 100, ShopEntry.Type.POISON)
                                .onBuy(DreamEaterShopHandler::giveThreeScorpions)
                                .build());
            }

            // 降帷：独立5分钟冷却（仅买家），半时关灯；黑灯进行中购买失败不扣费
            context.addEntry(new ShopEntry.Builder("curtain", ModItems.CURTAIN.getDefaultStack(), 250, ShopEntry.Type.TOOL)
                    .cooldown(GameConstants.getInTicks(5, 0))
                    .onBuy(DreamEaterShopHandler::buyCurtain)
                    .build());
        });
    }

    private static boolean giveThreeScorpions(PlayerEntity player) {
        boolean given = false;
        for (int i = 0; i < 3; i++) {
            given |= ShopEntry.insertStackInFreeSlot(player, WatheItems.SCORPION.getDefaultStack());
        }
        return given;
    }

    private static boolean buyCurtain(PlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld world)) return false;
        CurtainBlackoutHelper.pendingHalve = true;
        boolean success;
        try {
            success = WorldBlackoutComponent.KEY.get(world).triggerBlackout();
        } finally {
            CurtainBlackoutHelper.pendingHalve = false;
        }
        return success;
    }
}

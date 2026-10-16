package org.agmas.noellesroles.economist;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

public class EconomistShopHandler {
    private static final int REPORT_PRICE = 50;

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.ECONOMIST)) {
                return;
            }
            context.clearEntries();
            // 经济财报：购买即生效（不进入背包），限购1且观测期间（20秒）无法再次购买
            context.addEntry(new ShopEntry.Builder("economic_report",
                    ModItems.ECONOMIC_REPORT.getDefaultStack(), REPORT_PRICE, ShopEntry.Type.TOOL)
                    .stock(1)
                    .cooldown(EconomistPlayerComponent.OBSERVATION_TICKS)
                    .onBuy(buyer -> {
                        boolean started = EconomistPlayerComponent.KEY.get(buyer).beginObservation();
                        if (!started && buyer instanceof ServerPlayerEntity serverPlayer) {
                            serverPlayer.sendMessage(Text.translatable("tip.economist.no_target"), true);
                        }
                        return started;
                    })
                    .build());
        });
    }
}

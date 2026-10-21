package org.agmas.noellesroles.puppeteer;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/** 傀儡师商店：本体无法购买（商店隐藏），傀儡形态使用普通杀手商店。 */
public final class PuppeteerShopHandler {
    public static final String BODY_DENY_KEY = "shop.error.noellesroles.puppeteer.body";

    private PuppeteerShopHandler() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            if (isBodyForm(player)) {
                context.clearEntries();
            }
        });
    }

    /** 本体形态（未操纵傀儡）的傀儡师。 */
    public static boolean isBodyForm(PlayerEntity player) {
        return GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.PUPPETEER)
                && !PuppeteerPlayerComponent.get(player).isControlling();
    }
}

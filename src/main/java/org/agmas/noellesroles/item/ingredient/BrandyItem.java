package org.agmas.noellesroles.item.ingredient;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.item.IngredientItem;

public class BrandyItem extends IngredientItem {
    public BrandyItem(Settings settings) {
        super(settings, "brandy");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, EffectContext context) {
        player.addStatusEffect(new StatusEffectInstance(ModEffects.UNLIMITED_RANGE, context.scaleDuration(7 * 20), 0, false, false, true));
        addMoodBonus(player, 0.2f, context);
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xA0522D; // 赭褐
    }

    @Override
    public int getShopPrice() {
        return 75;
    }
}

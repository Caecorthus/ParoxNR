package org.agmas.noellesroles.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/** 购买即生效、不进入背包的商店图标物品，仅用 tooltip 说明效果。 */
public class ShopIconItem extends Item {
    private final String tooltipKey;

    public ShopIconItem(Settings settings, String tooltipKey) {
        super(settings);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : Text.translatable(this.tooltipKey).getString().split("\\R")) {
            // 与 wathe 商店物品描述一致的灰色（WatheItemTooltips.REGULAR_TOOLTIP_COLOR）
            tooltip.add(Text.literal(line).withColor(0x808080));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

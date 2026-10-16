package org.agmas.noellesroles.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class SleepingPillItem extends Item {
    public SleepingPillItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : Text.translatable("item.noellesroles.sleeping_pill.tooltip").getString().split("\\R")) {
            // 与 wathe 商店物品描述一致的灰色（WatheItemTooltips.REGULAR_TOOLTIP_COLOR）
            tooltip.add(Text.literal(line).withColor(0x808080));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

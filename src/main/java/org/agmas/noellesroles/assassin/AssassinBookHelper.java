package org.agmas.noellesroles.assassin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;

public final class AssassinBookHelper {
    private AssassinBookHelper() {}

    /** 玩家主背包或副手是否持有刺客之书。 */
    public static boolean hasBook(PlayerEntity player) {
        return findBook(player) != null;
    }

    /** 消耗一本刺客之书（主背包优先，其次副手）。 */
    public static boolean consumeBook(PlayerEntity player) {
        ItemStack stack = findBook(player);
        if (stack == null) {
            return false;
        }
        stack.decrement(1);
        return true;
    }

    private static ItemStack findBook(PlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(ModItems.ASSASSIN_BOOK)) {
                return stack;
            }
        }
        ItemStack offhand = player.getOffHandStack();
        return offhand.isOf(ModItems.ASSASSIN_BOOK) ? offhand : null;
    }
}

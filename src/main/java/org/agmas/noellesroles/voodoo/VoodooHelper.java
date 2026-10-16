package org.agmas.noellesroles.voodoo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;

public final class VoodooHelper {
    private VoodooHelper() {}

    /** 消耗手中（主手优先，其次副手）的一个巫毒娃娃。 */
    public static boolean consumeDoll(PlayerEntity player) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(ModItems.VOODOO_DOLL)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }
}

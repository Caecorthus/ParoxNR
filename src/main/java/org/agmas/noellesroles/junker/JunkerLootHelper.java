package org.agmas.noellesroles.junker;

import com.mojang.serialization.Codec;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;

/**
 * 拾荒者搜刮所得物品的标记与判定。
 * 标记用于两件事：阻碍再次搜刮、以及使用生效后销毁。
 */
public final class JunkerLootHelper {
    public static final ComponentType<Boolean> SEARCHED_LOOT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "junker_loot"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    private JunkerLootHelper() {}

    public static void init() {
    }

    public static void mark(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(SEARCHED_LOOT, true);
    }

    public static boolean isSearchedLoot(ItemStack stack) {
        return stack != null && !stack.isEmpty() && Boolean.TRUE.equals(stack.get(SEARCHED_LOOT));
    }

    /** 阻碍再次搜刮：搜刮所得物品，但房门钥匙、对讲机、中立万能钥匙除外。 */
    public static boolean blocksSearch(ItemStack stack) {
        if (!isSearchedLoot(stack)) {
            return false;
        }
        return !stack.isOf(WatheItems.KEY)
                && !stack.isOf(WatheItems.WALKIE_TALKIE)
                && !stack.isOf(ModItems.NEUTRAL_MASTER_KEY);
    }

    /** 使用生效后销毁：搜刮所得物品，但房门钥匙除外。 */
    public static boolean isDestroyableLoot(ItemStack stack) {
        return isSearchedLoot(stack) && !stack.isOf(WatheItems.KEY);
    }

    /** 能打开门的钥匙类道具（房门钥匙不在其中，它豁免销毁）。 */
    public static boolean isMasterKey(ItemStack stack) {
        return stack.isOf(ModItems.MASTER_KEY) || stack.isOf(ModItems.NEUTRAL_MASTER_KEY);
    }

    public static boolean hasBlockingLoot(PlayerEntity player) {
        return findFirstBlockingLoot(player) != null;
    }

    @Nullable
    public static ItemStack findFirstBlockingLoot(PlayerEntity player) {
        if (player == null) {
            return null;
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (blocksSearch(stack)) {
                return stack;
            }
        }
        return null;
    }

    /** 快捷栏（前9格）是否还有空位；搜刮所得物品需要放进快捷栏。 */
    public static boolean hasEmptyHotbarSlot(PlayerEntity player) {
        return findEmptyHotbarSlot(player) >= 0;
    }

    public static int findEmptyHotbarSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** 销毁一件搜刮所得物品；优先销毁主手对应的那一件。 */
    public static void destroy(PlayerEntity player, ItemStack target) {
        if (player == null || target == null || target.isEmpty()) {
            return;
        }
        ItemStack mainHand = player.getMainHandStack();
        if (isDestroyableLoot(mainHand) && mainHand.isOf(target.getItem())) {
            mainHand.decrement(1);
            return;
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack == target && isDestroyableLoot(stack)) {
                stack.decrement(1);
                return;
            }
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isDestroyableLoot(stack) && stack.isOf(target.getItem())) {
                stack.decrement(1);
                return;
            }
        }
    }

    /** 按物品类型找到玩家手上对应的搜刮所得物品（用于 recordItemUse 回调）。 */
    @Nullable
    public static ItemStack findDestroyableLootByItem(PlayerEntity player, Item item) {
        ItemStack mainHand = player.getMainHandStack();
        if (isDestroyableLoot(mainHand) && mainHand.isOf(item)) {
            return mainHand;
        }
        ItemStack offHand = player.getOffHandStack();
        if (isDestroyableLoot(offHand) && offHand.isOf(item)) {
            return offHand;
        }
        return null;
    }
}

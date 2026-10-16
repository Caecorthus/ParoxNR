package org.agmas.noellesroles.util;

import com.mojang.datafixers.util.Pair;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 隐藏装备工具类
 */
public final class HiddenEquipmentHelper {

    private HiddenEquipmentHelper() {}

    /**
     * 检查物品是否应该被隐藏（没有观察者上下文时，按“对所有人隐藏”处理）
     */
    public static boolean shouldHideItem(ItemStack stack, PlayerEntity holder) {
        return shouldHideItem(stack, holder, null);
    }

    /**
     * 检查物品是否应该对指定观察者隐藏
     *
     * @param observer 实际看着这件装备的玩家；传 null 表示没有观察者上下文
     */
    public static boolean shouldHideItem(ItemStack stack, PlayerEntity holder, @Nullable PlayerEntity observer) {
        if (stack.isEmpty()) return false;

        if (stack.isOf(ModItems.NEUTRAL_MASTER_KEY)) return true;
        if (stack.isOf(ModItems.ANTIDOTE)) return true;
        if (stack.isOf(ModItems.HALLUCINATION_MEDICINE)) return true;
        if (stack.isOf(ModItems.IRON_MAN_VIAL)) return true;
        if (stack.isOf(ModItems.VOODOO_DOLL)) return true;
        if (stack.isOf(ModItems.ATTENDANT_REGISTER)) return true;
        if (stack.isOf(WatheItems.WALKIE_TALKIE)) return true;
        if (stack.isOf(Items.WRITTEN_BOOK)) return true;

        if (stack.isOf(ModItems.TIMED_BOMB)) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(holder.getWorld());
            return gameWorld.isRole(holder, Noellesroles.BOMBER) && !BomberPlayerComponent.KEY.get(holder).hasBomb();
        }

        // 酒保商店物品和维修工具 — 始终隐藏
        if (stack.isOf(ModItems.RUM)) return true;
        if (stack.isOf(ModItems.GIN)) return true;
        if (stack.isOf(ModItems.VODKA)) return true;
        if (stack.isOf(ModItems.TEQUILA)) return true;
        if (stack.isOf(ModItems.WHISKEY)) return true;
        if (stack.isOf(ModItems.BRANDY)) return true;
        if (stack.isOf(ModItems.ICE_CUBE)) return true;
        if (stack.isOf(ModItems.LIQUEUR)) return true;
        if (stack.isOf(ModItems.SPECIAL_SPICE)) return true;
        if (stack.isOf(ModItems.REPAIR_TOOL)) return true;

        // 基酒 / 调好的酒 — 只有酒保能看见别人手里的酒，其他玩家一律看不见
        if (stack.isOf(ModItems.BASE_SPIRIT)) {
            return !isBartender(observer);
        }

        return false;
    }

    /**
     * 判断某个玩家当前是否是酒保
     */
    private static boolean isBartender(@Nullable PlayerEntity player) {
        if (player == null || player.getWorld() == null) return false;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.BARTENDER);
    }

    /**
     * 过滤装备数据包，将需要隐藏的物品替换为空
     *
     * @param packet   原始数据包
     * @param holder   物品持有者
     * @param observer 观察者（接收数据包的玩家）
     * @return 修改后的数据包，如果无需修改则返回 null
     */
    public static EntityEquipmentUpdateS2CPacket filterPacket(
            EntityEquipmentUpdateS2CPacket packet,
            PlayerEntity holder,
            ServerPlayerEntity observer
    ) {
        // 只对活着的生存模式玩家隐藏
        if (!GameFunctions.isPlayerPlayingAndAlive(observer)) {
            return null;
        }

        List<Pair<EquipmentSlot, ItemStack>> original = packet.getEquipmentList();
        List<Pair<EquipmentSlot, ItemStack>> filtered = new ArrayList<>(original.size());
        boolean modified = false;

        for (Pair<EquipmentSlot, ItemStack> pair : original) {
            EquipmentSlot slot = pair.getFirst();
            ItemStack stack = pair.getSecond();

            if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
                    && (SwallowedPlayerComponent.isPlayerSwallowed(holder)
                    || LooseEndPlayerComponent.KEY.get(holder).isOpeningPhased()
                    || shouldHideItem(stack, holder, observer))) {
                filtered.add(Pair.of(slot, ItemStack.EMPTY));
                modified = true;
            } else {
                filtered.add(pair);
            }
        }

        return modified ? new EntityEquipmentUpdateS2CPacket(packet.getEntityId(), filtered) : null;
    }
}

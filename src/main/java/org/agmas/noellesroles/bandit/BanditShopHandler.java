package org.agmas.noellesroles.bandit;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;

import java.util.List;

/**
 * 强盗商店处理器
 * - 删除手榴弹、疯魔模式、毒药、蝎子
 * - 手枪价格改为225；手枪后面插入投掷斧（175）
 * - 飞斧后面插入「回收飞斧」（125，每次回收1把，无冷却）
 */
public class BanditShopHandler {
    private static final String RECOVER_ENTRY_ID = "bandit_recover_axes";
    private static final int RECOVER_PRICE = 125;
    private static final String RECOVER_NOTHING_KEY = "shop.error.noellesroles.bandit.nothing_to_recover";
    private static final String RECOVER_HOTBAR_FULL_KEY = "shop.error.noellesroles.bandit.hotbar_full";

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BANDIT)) {
                // 删除手榴弹、疯魔模式、毒药、蝎子
                context.getEntries().removeIf(entry -> {
                    String id = entry.id();
                    return id.equals("grenade") || id.equals("psycho_mode") ||
                           id.equals("poison_vial") || id.equals("scorpion");
                });

                // 手枪价格改为225，飞斧（175）插入手枪后面，回收飞斧（125）紧随其后
                for (int i = 0; i < context.getEntries().size(); i++) {
                    if (context.getEntries().get(i).id().equals("revolver")) {
                        context.setEntry(i, new ShopEntry.Builder("revolver", WatheItems.REVOLVER.getDefaultStack(), 225, ShopEntry.Type.WEAPON).build());
                        context.addEntry(i + 1, new ShopEntry(ModItems.THROWING_AXE.getDefaultStack(), 175, ShopEntry.Type.WEAPON));
                        context.addEntry(i + 2, new ShopEntry.Builder(RECOVER_ENTRY_ID, ModItems.RECOVER_AXE.getDefaultStack(), RECOVER_PRICE, ShopEntry.Type.TOOL)
                                .onBuy(BanditShopHandler::buyRecoverAxes)
                                .build());
                        break;
                    }
                }
            }
        });
    }

    /**
     * 购买前的拒绝理由（接入 ShopPurchase.BEFORE）：世界无自有飞斧、快捷栏无空位。
     * 金币不足走商店通用「购买失败」提示。
     */
    public static String getRecoverDenyReason(PlayerEntity player, ShopEntry entry) {
        if (entry == null || !RECOVER_ENTRY_ID.equals(entry.id())) {
            return null;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.BANDIT)) {
            return null;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return null;
        }
        if (BanditSkillHelper.findRecoverableAxes(serverPlayer).isEmpty()) {
            return RECOVER_NOTHING_KEY;
        }
        if (findFirstFreeHotbarSlot(player) < 0) {
            return RECOVER_HOTBAR_FULL_KEY;
        }
        return null;
    }

    /**
     * 回收购买：回收世界中的 1 把自有飞斧到快捷栏。扣款由商店流程完成；失败返回 false 不扣款。
     */
    private static boolean buyRecoverAxes(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        List<ThrowingAxeEntity> axes = BanditSkillHelper.findRecoverableAxes(serverPlayer);
        if (axes.isEmpty()) {
            return false;
        }
        int freeSlot = findFirstFreeHotbarSlot(player);
        if (freeSlot < 0) {
            return false;
        }

        axes.get(0).discard();
        player.getInventory().setStack(freeSlot, new ItemStack(ModItems.THROWING_AXE));
        player.getInventory().markDirty();
        BanditPlayerComponent.KEY.get(player).setRecoverableAxes(axes.size() - 1);

        player.sendMessage(Text.translatable("tip.noellesroles.bandit.recovered", 1), true);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 购买本身已有商店标准回放记录（SHOP_PURCHASE），不再额外记录技能事件
        return true;
    }

    private static int findFirstFreeHotbarSlot(PlayerEntity player) {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}

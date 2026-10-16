package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.dreameater.SleepingPillHelper;
import org.agmas.noellesroles.dreameater.SleepingPillPlateComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 安眠药的下毒与取餐。
 * 下毒：与毒药瓶一致的手感（音效/一次性/记录），但通过世界组件把「本盘用安眠药下毒」记录下来；
 * 不绑定噬梦者身份，也不消耗毒理学家的餐盘预防护。
 * 取餐：从安眠药盘取出的食物带 SLEEPING_PILL_POISONER 标记（不写 wathe 的 POISONER），
 * 食用时视为一次噬梦者技能命中，而不是中毒。
 * 创造模式：完全放行，走 wathe 自带的创造分支（手持物品放入盘子），与毒药瓶一致 —— 便于搭建测试场景。
 */
@Mixin(FoodPlatterBlock.class)
public abstract class SleepingPillPlateMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$sleepingPillPlate(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        // 创造模式放行：wathe 的 onUse 在此之前有创造分支，手持非空物品右键是「放入盘子」（addItem 拷贝入库、不消耗），
        // 而不是下毒。与幻觉药/酒保饮料的同类 mixin 保持一致。
        if (world.isClient || player.isCreative()) return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof BeveragePlateBlockEntity plate)) return;
        ItemStack handStack = player.getStackInHand(Hand.MAIN_HAND);

        // === 下毒：手持安眠药，条件与毒药瓶一致（盘非空、未下毒） ===
        if (handStack.isOf(ModItems.SLEEPING_PILL) && plate.getPoisoner() == null && !plate.getStoredItems().isEmpty()) {
            String poisonerUuid = player.getUuidAsString();
            plate.setPoisoner(poisonerUuid);
            SleepingPillPlateComponent pillComponent = SleepingPillPlateComponent.KEY.get(world);
            pillComponent.markPlate(pos, poisonerUuid);
            SleepingPillPlateComponent.KEY.sync(world);
            handStack.decrement(1);
            player.playSoundToPlayer(SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.5f, 1f);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                GameRecordManager.putBlockPos(extra, "pos", pos);
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.SLEEPING_PILL), null, extra);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // === 取餐：空手从安眠药盘取出食物时携带安眠药标记 ===
        if (handStack.isEmpty()) {
            String poisoner = plate.getPoisoner();
            if (poisoner == null) return;
            SleepingPillPlateComponent pillComponent = SleepingPillPlateComponent.KEY.get(world);
            if (!pillComponent.isPillPlate(pos, poisoner)) return;

            List<ItemStack> platter = plate.getStoredItems();
            if (platter.isEmpty()) return;

            // 与 wathe 原版一致：背包里已有同类物品时不可取
            boolean hasPlatterItem = false;
            for (ItemStack platterItem : platter) {
                for (int i = 0; i < player.getInventory().size(); i++) {
                    if (player.getInventory().getStack(i).getItem() == platterItem.getItem()) {
                        hasPlatterItem = true;
                        break;
                    }
                }
                if (hasPlatterItem) break;
            }
            if (hasPlatterItem) return;

            ItemStack randomItem = platter.get(world.random.nextInt(platter.size())).copy();
            randomItem.setCount(1);
            randomItem.set(DataComponentTypes.MAX_STACK_SIZE, 1);
            SleepingPillHelper.markFood(randomItem, poisoner);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // sleeping_pill 标记让回放显示蓝色「投药」而不是默认的绿色「投毒」
                NbtCompound extra = new NbtCompound();
                extra.putBoolean("sleeping_pill", true);
                GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(randomItem.getItem()), pos, poisoner, extra);
            }
            plate.setPoisoner(null);
            pillComponent.clearPlate(pos);
            SleepingPillPlateComponent.KEY.sync(world);
            player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
            player.setStackInHand(Hand.MAIN_HAND, randomItem);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}

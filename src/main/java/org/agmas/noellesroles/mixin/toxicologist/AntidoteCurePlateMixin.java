package org.agmas.noellesroles.mixin.toxicologist;

import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import org.agmas.noellesroles.item.AntidoteItem;
import org.agmas.noellesroles.toxicologist.AntidotePlateShieldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoodPlatterBlock.class)
public abstract class AntidoteCurePlateMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void antidoteCurePlate(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient) return;

        if (!player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.ANTIDOTE)) return;
        if (player.getItemCooldownManager().isCoolingDown(ModItems.ANTIDOTE)) return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof BeveragePlateBlockEntity plate)) return;

        if (plate.getPoisoner() != null) {
            // 已中毒：清除毒素
            plate.setPoisoner(null);
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.2F);
            player.getItemCooldownManager().set(ModItems.ANTIDOTE, AntidoteItem.COOLDOWN_TICKS);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "cure_plate");
                GameRecordManager.putBlockPos(extra, "pos", pos);
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.ANTIDOTE), null, extra);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        } else {
            // 未中毒：施加预防护，使下一次下毒无效。静音，不留记录
            AntidotePlateShieldComponent shieldComp = AntidotePlateShieldComponent.KEY.get(world);
            if (!shieldComp.hasShield(pos)) {
                shieldComp.applyShield(pos);
                AntidotePlateShieldComponent.KEY.sync(world);
                player.getItemCooldownManager().set(ModItems.ANTIDOTE, AntidoteItem.COOLDOWN_TICKS);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}

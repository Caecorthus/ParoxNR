package org.agmas.noellesroles.mixin.dreameater;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.dreameater.DreamEaterPlayerComponent;
import org.agmas.noellesroles.dreameater.SleepingPillHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * 食用安眠药食物：视为一次噬梦者技能命中。
 * 该类食物不携带 wathe 的 POISONER 组件，因此不会触发原版中毒。
 */
@Mixin(PlayerEntity.class)
public abstract class SleepFoodEatMixin {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void noellesroles$sleepFoodHit(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        String poisoner = SleepingPillHelper.getPoisoner(stack);
        if (poisoner == null) return;
        if (!((Object) this instanceof ServerPlayerEntity serverPlayer)) return;

        UUID applierUuid = null;
        try {
            applierUuid = UUID.fromString(poisoner);
        } catch (IllegalArgumentException ignored) {
        }
        DreamEaterPlayerComponent.KEY.get(serverPlayer).applySkillHit(applierUuid);
    }
}

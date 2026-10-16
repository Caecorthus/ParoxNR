package org.agmas.noellesroles.mixin.junker;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.junker.JunkerLootDestroyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 蓄力类物品（刀等）松手结束使用时，销毁作为搜刮所得的手持物品。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJunkerUseMixin {

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void noellesroles$destroyChargedSearchedLoot(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity serverPlayer) {
            JunkerLootDestroyHandler.onChargedItemReleased(serverPlayer);
        }
    }
}

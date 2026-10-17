package org.agmas.noellesroles.mixin.push;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.util.PushAttributionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家被其他玩家成功打中（含持毒针/刀出拳、防暴盾推撞）时记录推人者，供坠车归属使用。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPushAttributionMixin {

    @Inject(method = "setAttacker", at = @At("TAIL"))
    private void noellesroles$recordPusher(LivingEntity attacker, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity victim && attacker instanceof ServerPlayerEntity pusher) {
            PushAttributionTracker.recordHit(victim, pusher);
        }
    }
}

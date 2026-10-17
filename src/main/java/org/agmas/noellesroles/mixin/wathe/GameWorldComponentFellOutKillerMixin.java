package org.agmas.noellesroles.mixin.wathe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.util.PushAttributionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 坠车击杀：原版 lastAttacker（5 秒）已过期时，改用推人归属记录里的推人者作为凶手，
 * 让回放、击杀金币与犯罪推理学家的判定都把"被推下列车"算到推人者头上。
 */
@Mixin(GameWorldComponent.class)
public abstract class GameWorldComponentFellOutKillerMixin {

    @WrapOperation(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;)V"
            )
    )
    private void noellesroles$creditPusherForFellOut(ServerPlayerEntity victim, boolean spawnBody, ServerPlayerEntity killer,
                                                     Identifier deathReason, Operation<Void> original) {
        if (killer == null && GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason)) {
            killer = PushAttributionTracker.getPusher(victim);
        }
        original.call(victim, spawnBody, killer, deathReason);
    }
}

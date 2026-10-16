package org.agmas.noellesroles.client.mixin.dualrole;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbientLoop;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.client.screen.DualRoleSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 双选界面打开期间让环境音（列车运行声等）判定为"不应播放"，
 * 音源会在淡出时间内静音，界面关闭后自动淡入恢复。
 */
@Mixin(BackgroundAmbientLoop.class)
public abstract class AmbienceDualSelectionMuteMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/ratatouille/client/util/ambience/BackgroundAmbience$PlayPredicate;shouldPlay(Lnet/minecraft/client/network/ClientPlayerEntity;)Z"
            )
    )
    private boolean noellesroles$muteAmbienceDuringDualSelection(BackgroundAmbience.PlayPredicate predicate, ClientPlayerEntity player, Operation<Boolean> original) {
        if (DualRoleSelectionScreen.isSelectionScreenOpen()) {
            return false;
        }
        return original.call(predicate, player);
    }
}

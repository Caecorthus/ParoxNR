package org.agmas.noellesroles.client.mixin.dualrole;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.client.screen.DualRoleSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 双选期间阻止环境音在淡出结束后被重启（否则会以满音量重新出现后再次淡出，
 * 产生周期性爆音）；界面关闭后 tryStarting 恢复正常，音源自动重新淡入。
 */
@Mixin(BackgroundAmbience.class)
public abstract class AmbienceDualSelectionRestartMixin {

    @WrapOperation(
            method = "tryStarting",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/ratatouille/client/util/ambience/BackgroundAmbience$PlayPredicate;shouldPlay(Lnet/minecraft/client/network/ClientPlayerEntity;)Z"
            )
    )
    private boolean noellesroles$blockAmbienceRestartDuringDualSelection(BackgroundAmbience.PlayPredicate predicate, ClientPlayerEntity player, Operation<Boolean> original) {
        if (DualRoleSelectionScreen.isSelectionScreenOpen()) {
            return false;
        }
        return original.call(predicate, player);
    }
}

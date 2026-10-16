package org.agmas.noellesroles.mixin.dualrole;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 双角色选择期间暂停各模式的回合主循环（杀手被动金钱、胜负判定、模式事件等），
 * 让这些按 tick 推进的资源从选择结束才开始计。
 */
@Mixin(GameWorldComponent.class)
public abstract class GameWorldComponentServerTickFreezeMixin {

    @WrapOperation(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/GameMode;tickServerGameLoop(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;)V"
            )
    )
    private void noellesroles$freezeModeLoopDuringDualRoleSelection(GameMode gameMode, ServerWorld world, GameWorldComponent gameComponent, Operation<Void> original) {
        if (!DualRoleSelectionWorldComponent.isSelectionFreezeActive(world)) {
            original.call(gameMode, world, gameComponent);
        }
    }
}

package org.agmas.noellesroles.mixin.dualrole;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 双角色选择期间暂停物品冷却计时（含客户端热键栏读秒动画），
 * 避免开局给左轮/小刀设的冷却在选择期间被白掉。
 * 全游戏只有 PlayerEntity#tick 一处调用 ItemCooldownManager#update，
 * 客户端经 ClientPlayerEntity#tick → super.tick() 同样覆盖。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityCooldownFreezeMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ItemCooldownManager;update()V")
    )
    private void noellesroles$freezeItemCooldownsDuringDualRoleSelection(ItemCooldownManager manager, Operation<Void> original) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!DualRoleSelectionWorldComponent.isSelectionFreezeActive(self.getWorld())) {
            original.call(manager);
        }
    }
}

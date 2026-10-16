package org.agmas.noellesroles.mixin.dualrole;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 双角色选择期间暂停心情系统：任务生成计时不推进，第一条任务从选择结束才开始排。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class PlayerMoodComponentFreezeMixin {

    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$freezeDuringDualRoleSelection(CallbackInfo ci) {
        if (DualRoleSelectionWorldComponent.isSelectionFreezeActive(this.player.getWorld())) {
            ci.cancel();
        }
    }
}

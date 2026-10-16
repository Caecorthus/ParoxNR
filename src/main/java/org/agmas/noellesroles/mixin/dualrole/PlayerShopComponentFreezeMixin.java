package org.agmas.noellesroles.mixin.dualrole;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 双角色选择期间暂停商店计时：商店条目的开局冷却（initialCooldown）不被白掉。
 */
@Mixin(PlayerShopComponent.class)
public abstract class PlayerShopComponentFreezeMixin {

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

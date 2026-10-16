package org.agmas.noellesroles.mixin.dualrole;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import net.minecraft.world.World;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 双角色选择期间暂停对局计时：服务端倒计时与客户端 HUD 读数一起停住，
 * 避免选择耗掉的 5 秒从回合时长里白扣。
 */
@Mixin(GameTimeComponent.class)
public abstract class GameTimeComponentFreezeMixin {

    @Shadow
    @Final
    public World world;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$freezeDuringDualRoleSelection(CallbackInfo ci) {
        if (DualRoleSelectionWorldComponent.isSelectionFreezeActive(this.world)) {
            ci.cancel();
        }
    }
}

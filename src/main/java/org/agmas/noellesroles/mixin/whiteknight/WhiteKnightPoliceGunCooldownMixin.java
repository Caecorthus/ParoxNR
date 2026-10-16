package org.agmas.noellesroles.mixin.whiteknight;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 白骑士沿用 Wathe 现成的警类枪冷分支：把"义警/老兵不套用乘客 15 秒冷却"的判定一并覆盖白骑士，
 * 使其直接拿到 ITEM_COOLDOWNS 的标准 10 秒冷却（不另设冷却数值）。
 * <p>
 * receive 方法内共 4 处 isRole 调用：前两处（ordinal 0/1）是开枪命中后的理智扣除判定，白骑士必须照常扣理智；
 * 后两处（ordinal 2/3）才是冷却判定，这里只改 ordinal 2（义警那次）。
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class WhiteKnightPoliceGunCooldownMixin {

    @ModifyExpressionValue(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isRole(Lnet/minecraft/class_1657;Ldev/doctor4t/wathe/api/Role;)Z",
                    ordinal = 2
            ),
            remap = false
    )
    private boolean noellesroles$whiteKnightUsesPoliceGunCooldown(boolean original,
                                                                  @Local(argsOnly = true) ServerPlayNetworking.Context context) {
        if (original) {
            return true;
        }
        return GameWorldComponent.KEY.get(context.player().getWorld())
                .isRole(context.player(), Noellesroles.WHITE_KNIGHT);
    }
}

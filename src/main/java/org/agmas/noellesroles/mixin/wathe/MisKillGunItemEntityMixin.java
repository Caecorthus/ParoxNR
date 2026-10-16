package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.util.MisKillGunCooldownHelper;
import org.agmas.noellesroles.util.MisKillGunMark;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 误杀冷却惩罚：误杀者死于误杀惩罚后遗留的枪被拾起时，给拾取者 15 秒左轮冷却。
 * 注入点选在 sendPickup 调用处——只有 insertStack 成功才会走到，拾取必然成立。
 */
@Mixin(ItemEntity.class)
public abstract class MisKillGunItemEntityMixin implements MisKillGunMark {

    @Unique
    private boolean noellesroles$misKillGun = false;

    @Override
    public void noellesroles$markMisKillGun() {
        this.noellesroles$misKillGun = true;
    }

    @Inject(
            method = "onPlayerCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendPickup(Lnet/minecraft/entity/Entity;I)V"
            )
    )
    private void noellesroles$applyMisKillGunCooldown(PlayerEntity player, CallbackInfo ci) {
        if (!this.noellesroles$misKillGun) {
            return;
        }
        if (player.isCreative()) {
            return;
        }
        if (!ConfigWorldComponent.KEY.get(player.getWorld()).misKillGunCooldown) {
            return;
        }
        this.noellesroles$misKillGun = false;
        player.getItemCooldownManager().set(WatheItems.REVOLVER, MisKillGunCooldownHelper.PICKUP_COOLDOWN_TICKS);
    }
}

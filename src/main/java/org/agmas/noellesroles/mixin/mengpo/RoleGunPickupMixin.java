package org.agmas.noellesroles.mixin.mengpo;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 按职业实时禁止捡枪：孟婆与圣徒碰到地上的枪不会捡起。
 * 按当前职业判断而不是写进 Wathe 的禁止捡枪名单，转职后立即恢复（名单没有移除接口，误杀惩罚也用它）。
 */
@Mixin(ItemEntity.class)
public abstract class RoleGunPickupMixin {
    @Shadow
    public abstract ItemStack getStack();

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventRoleGunPickup(PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative() || player.getWorld().isClient) {
            return;
        }
        if (!this.getStack().isIn(WatheItemTags.GUNS)) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(player, Noellesroles.MENGPO) || gameWorld.isRole(player, Noellesroles.SAINT)) {
            ci.cancel();
        }
    }
}

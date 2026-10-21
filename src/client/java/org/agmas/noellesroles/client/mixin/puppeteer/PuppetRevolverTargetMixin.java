package org.agmas.noellesroles.client.mixin.puppeteer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 左轮可以射中傀儡 / 本体替身（射程与原版一致，白兰地「射程无限制」期间同样放大）。 */
@Mixin(targets = "dev.doctor4t.wathe.item.RevolverItem")
public class PuppetRevolverTargetMixin {
    @Inject(method = "getGunTarget", at = @At("RETURN"), cancellable = true, remap = false)
    private static void noellesroles$targetPuppet(PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        double range = user.hasStatusEffect(ModEffects.UNLIMITED_RANGE) ? 2048.0 : 30.0;
        cir.setReturnValue(PuppeteerClient.preferPuppetTarget(user, cir.getReturnValue(), range));
    }
}

package org.agmas.noellesroles.client.mixin.puppeteer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 掌心雷可以射中傀儡 / 本体替身（射程 7 格，与原版一致）。 */
@Mixin(targets = "dev.doctor4t.wathe.item.DerringerItem")
public class PuppetDerringerTargetMixin {
    @Inject(method = "getGunTarget", at = @At("RETURN"), cancellable = true, remap = false)
    private static void noellesroles$targetPuppet(PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        cir.setReturnValue(PuppeteerClient.preferPuppetTarget(user, cir.getReturnValue(), 7.0));
    }
}

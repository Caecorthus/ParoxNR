package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.morphling.ClientForgedBodyVisualHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 赝造药水：按本机视角替换尸体皮肤（理智正常的验尸官看破）。 */
@Mixin(PlayerBodyEntityRenderer.class)
public class ForgedBodySkinMixin {

    @Inject(method = "getTexture(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void noellesroles$forgedBodySkin(PlayerBodyEntity body, CallbackInfoReturnable<Identifier> cir) {
        SkinTextures forgedTextures = ClientForgedBodyVisualHelper.getForgedSkinTextures(body);
        if (forgedTextures != null) {
            cir.setReturnValue(forgedTextures.texture());
        }
    }
}

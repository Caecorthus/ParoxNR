package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.morphling.ClientMorphlingScrambleHelper;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(ElytraFeatureRenderer.class)
public class MorphlingElytraRendererMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures noellesroles$wrapElytraTexture(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        SkinTextures shuffledTextures = ClientMorphlingScrambleHelper.getScrambledSkinTextures(instance);
        if (shuffledTextures != null) {
            return shuffledTextures;
        }
        if (!GameWorldComponent.KEY.get(instance.getWorld()).isRunning()) {
            return original.call(instance);
        }

        MorphlingPlayerComponent component = MorphlingPlayerComponent.KEY.get(instance);
        if (component.getMorphTicks() > 0) {
            UUID disguiseUuid = component.disguise;
            if (disguiseUuid != null) {
                PlayerEntity target = instance.getWorld().getPlayerByUuid(disguiseUuid);
                if (target instanceof AbstractClientPlayerEntity disguisePlayer) {
                    return disguisePlayer.getSkinTextures();
                }
                var cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
                if (cachedEntry != null) {
                    return cachedEntry.getSkinTextures();
                }
            }
        }

        return original.call(instance);
    }
}

package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.morphling.ClientMorphlingScrambleHelper;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unchecked")
@Mixin(EntityRenderDispatcher.class)
public class MorphlingRendererDispatchMixin {
    @Shadow
    private Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity>> modelRenderers;

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void noellesroles$morphlingModelSwap(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRunning()) {
            return;
        }

        SkinTextures.Model targetModel = null;
        PlayerListEntry shuffledEntry = ClientMorphlingScrambleHelper.getScrambledEntry(player);
        if (shuffledEntry != null) {
            targetModel = shuffledEntry.getSkinTextures().model();
        }

        if (targetModel == null) {
            UUID puppetDisguise = PuppeteerClient.getPuppetDisguise(player);
            if (puppetDisguise != null) {
                targetModel = PuppeteerClient.getSkinTextures(puppetDisguise).model();
            }
        }

        if (targetModel == null) {
            MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(player);
            if (morphComp.getMorphTicks() > 0 && morphComp.disguise != null) {
                PlayerEntity disguisePlayer = player.getWorld().getPlayerByUuid(morphComp.disguise);
                if (disguisePlayer instanceof AbstractClientPlayerEntity disguiseClient) {
                    targetModel = disguiseClient.getSkinTextures().model();
                } else {
                    PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(morphComp.disguise);
                    if (cachedEntry != null) {
                        targetModel = cachedEntry.getSkinTextures().model();
                    }
                }
            }
        }

        if (targetModel != null) {
            EntityRenderer<? extends PlayerEntity> renderer = this.modelRenderers.get(targetModel);
            if (renderer != null) {
                cir.setReturnValue((EntityRenderer<? super T>) renderer);
            }
        }
    }
}

package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.morphling.ClientMorphlingScrambleHelper;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntityRenderer.class)
public abstract class MorphlingRendererMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    void noellesroles$swapTexture(AbstractClientPlayerEntity player, CallbackInfoReturnable<Identifier> cir) {
        PlayerListEntry shuffledEntry = ClientMorphlingScrambleHelper.getScrambledEntry(player);
        if (shuffledEntry != null) {
            cir.setReturnValue(shuffledEntry.getSkinTextures().texture());
            cir.cancel();
            return;
        }
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRunning()) {
            return;
        }

        MorphlingPlayerComponent morphComponent = MorphlingPlayerComponent.KEY.get(player);
        if (morphComponent.getMorphTicks() <= 0 || morphComponent.disguise == null) {
            return;
        }

        UUID disguiseUuid = morphComponent.disguise;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && disguiseUuid.equals(client.player.getUuid())) {
            cir.setReturnValue(client.player.getSkinTextures().texture());
            cir.cancel();
            return;
        }

        AbstractClientPlayerEntity disguiseEntity = (AbstractClientPlayerEntity) player.getEntityWorld().getPlayerByUuid(disguiseUuid);
        if (disguiseEntity != null) {
            cir.setReturnValue(disguiseEntity.getSkinTextures().texture());
            cir.cancel();
            return;
        }

        PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
        if (cachedEntry != null) {
            cir.setReturnValue(cachedEntry.getSkinTextures().texture());
            cir.cancel();
        }
    }

    @WrapOperation(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    SkinTextures noellesroles$swapArmTexture(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        SkinTextures shuffledTextures = ClientMorphlingScrambleHelper.getScrambledSkinTextures(instance);
        if (shuffledTextures != null) {
            return shuffledTextures;
        }
        if (!GameWorldComponent.KEY.get(instance.getWorld()).isRunning()) {
            return original.call(instance);
        }

        MorphlingPlayerComponent morphComponent = MorphlingPlayerComponent.KEY.get(instance);
        if (morphComponent.getMorphTicks() > 0 && morphComponent.disguise != null) {
            AbstractClientPlayerEntity disguiseEntity = (AbstractClientPlayerEntity) instance.getEntityWorld().getPlayerByUuid(morphComponent.disguise);
            if (disguiseEntity != null) {
                return disguiseEntity.getSkinTextures();
            }

            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(morphComponent.disguise);
            if (cachedEntry != null) {
                return cachedEntry.getSkinTextures();
            }
        }
        return original.call(instance);
    }
}

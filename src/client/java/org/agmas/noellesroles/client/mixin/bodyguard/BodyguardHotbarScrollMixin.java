package org.agmas.noellesroles.client.mixin.bodyguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class BodyguardHotbarScrollMixin {

    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockBodyguardStanceHotbarScroll(double scrollAmount, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && BodyguardPlayerComponent.KEY.get(client.player).isStanceActive()) {
            ci.cancel();
        }
    }
}

package org.agmas.noellesroles.client.mixin.bodyguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class BodyguardLookInputMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockBodyguardStanceLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && (Object) this == client.player
                && BodyguardPlayerComponent.KEY.get(client.player).isStanceActive()) {
            ci.cancel();
        }
    }
}

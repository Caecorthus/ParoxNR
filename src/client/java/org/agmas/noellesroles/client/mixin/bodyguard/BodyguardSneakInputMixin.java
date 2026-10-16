package org.agmas.noellesroles.client.mixin.bodyguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class BodyguardSneakInputMixin {

    // 姿态期间每 tick 在 Input.tick 之后把潜行输入钉回 true(本地 isSneaking()==input.sneaking)
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void noellesroles$holdSneakWhileBodyguardStance(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && (Object) this == client.player
                && BodyguardPlayerComponent.KEY.get(client.player).isStanceActive()) {
            client.player.input.sneaking = true;
        }
    }
}

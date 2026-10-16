package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameFunctions.class)
public abstract class GameFunctionsMatchLengthMixin {
    @ModifyArg(
            method = "startGame",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameTimeComponent;setResetTime(I)V"
            ),
            index = 0
    )
    private static int noellesroles$applyConfiguredMatchLength(int originalTicks) {
        int seconds = NoellesRolesConfig.HANDLER.instance().matchLengthSeconds;
        return seconds > 0 ? seconds * 20 : originalTicks;
    }
}

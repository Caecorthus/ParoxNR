package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldBlackoutComponent.BlackoutDetails.class)
public interface BlackoutDetailsAccessor {
    @Accessor("time")
    int noellesroles$getTime();

    @Accessor("time")
    void noellesroles$setTime(int time);
}

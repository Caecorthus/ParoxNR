package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WorldBlackoutComponent.class)
public interface WorldBlackoutAccessor {
    @Accessor("ticks")
    int noellesroles$getTicks();

    @Accessor("ticks")
    void noellesroles$setTicks(int ticks);

    @Accessor("blackouts")
    List<WorldBlackoutComponent.BlackoutDetails> noellesroles$getBlackouts();
}

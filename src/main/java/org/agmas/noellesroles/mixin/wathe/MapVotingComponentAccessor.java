package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.cca.MapVotingComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(MapVotingComponent.class)
public interface MapVotingComponentAccessor {
    @Accessor("availableModes")
    List<MapVotingComponent.VotingModeEntry> noellesroles$getAvailableModes();

    @Accessor("availableMaps")
    List<MapVotingComponent.VotingMapEntry> noellesroles$getAvailableMaps();

    @Accessor("unavailableMaps")
    List<MapVotingComponent.UnavailableMapEntry> noellesroles$getUnavailableMaps();

    @Accessor("voteCounts")
    int[] noellesroles$getVoteCounts();

    @Accessor("voteCounts")
    void noellesroles$setVoteCounts(int[] voteCounts);

    @Accessor("selectedMapIndex")
    int noellesroles$getSelectedMapIndex();

    @Accessor("selectedMapIndex")
    void noellesroles$setSelectedMapIndex(int selectedMapIndex);

    @Accessor("votingActive")
    boolean noellesroles$isVotingActive();

    @Accessor("roulettePhase")
    boolean noellesroles$isRoulettePhase();

    @Invoker("finishSelection")
    void noellesroles$invokeFinishSelection();

    @Invoker("endVoting")
    void noellesroles$invokeEndVoting(boolean ignoreMinPlayers);
}

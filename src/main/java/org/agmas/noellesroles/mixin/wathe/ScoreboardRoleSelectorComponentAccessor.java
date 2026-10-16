package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.List;

@Mixin(ScoreboardRoleSelectorComponent.class)
public interface ScoreboardRoleSelectorComponentAccessor {
    @Invoker("getAvailablePlayers")
    ArrayList<ServerPlayerEntity> noellesroles$invokeGetAvailablePlayers(
            ServerWorld world, GameWorldComponent gameWorld, List<ServerPlayerEntity> players);

    @Invoker("createSelectionContext")
    RoleSelectionContext noellesroles$invokeCreateSelectionContext(
            ServerWorld world, GameWorldComponent gameWorld, List<ServerPlayerEntity> players);
}

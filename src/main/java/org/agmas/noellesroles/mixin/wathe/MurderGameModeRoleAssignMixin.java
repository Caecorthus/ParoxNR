package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.agmas.noellesroles.util.OpeningKnockbackSuppressionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Replaces MurderGameMode.initializeGame() with custom role assignment
 * that supports dual role selection. RoleAssigned.EVENT is deferred
 * until after the player chooses their role.
 */
@Mixin(value = MurderGameMode.class, remap = false)
public abstract class MurderGameModeRoleAssignMixin {

    @Inject(method = "initializeGame", at = @At("HEAD"), cancellable = true)
    public void replaceInitializeGame(ServerWorld serverWorld, GameWorldComponent gameWorldComponent, List<ServerPlayerEntity> players, CallbackInfo ci) {
        DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(serverWorld);

        // Role assignment (copy of assignRolesAndGetKillerCount)
        for (ServerPlayerEntity player : players) {
            gameWorldComponent.addRole(player, WatheRoles.NO_ROLE);
        }
        ScoreboardRoleSelectorComponent roleSelector = ScoreboardRoleSelectorComponent.KEY.get(serverWorld.getScoreboard());
        roleSelector.assignForcedRoles(serverWorld, gameWorldComponent, players);
        int killerCount = roleSelector.assignKillers(serverWorld, gameWorldComponent, players,
                (int) Math.floor((double) players.size() / gameWorldComponent.getKillerDividend()));
        roleSelector.assignVigilantes(serverWorld, gameWorldComponent, players,
                (int) Math.floor((double) players.size() / gameWorldComponent.getVigilanteDividend()));
        roleSelector.assignNeutrals(serverWorld, gameWorldComponent, players,
                (int) Math.floor((double) players.size() / gameWorldComponent.getNeutralDividend()));
        roleSelector.assignCivilians(serverWorld, gameWorldComponent, players);

        gameWorldComponent.setInitialKillerCount(killerCount);

        // Set item cooldowns (from original initializeGame)
        OpeningKnockbackSuppressionHelper.reset(serverWorld.getServer());
        for (ServerPlayerEntity player : players) {
            player.getItemCooldownManager().set(WatheItems.REVOLVER, GameConstants.getInTicks(1, 0));
            // 开局刀冷却45秒；冷却期间禁用持械近战击退与出拳放行（持刀且刀不在冷却时恢复击退）
            player.getItemCooldownManager().set(WatheItems.KNIFE, OpeningKnockbackSuppressionHelper.OPENING_KNIFE_COOLDOWN_TICKS);
            OpeningKnockbackSuppressionHelper.startOpeningWindow(player);
        }

        // Check if dual role selection is active (set by ON_GAME_START)
        if (DualRoleSelectionWorldComponent.isDeferringRoleEvents()) {
            // Store assigned roles for later (don't fire RoleAssigned.EVENT yet)
            for (ServerPlayerEntity player : players) {
                DualRoleSelectionWorldComponent.storePendingAssignment(
                        player.getUuid(), gameWorldComponent.getRole(player));
            }
            // Welcome announcement will be sent after selection
        } else {
            // Fire RoleAssigned.EVENT normally (no dual role)
            for (ServerPlayerEntity player : players) {
                RoleAssigned.EVENT.invoker().assignRole(player, gameWorldComponent.getRole(player));
            }
            // Send welcome announcements
            int totalPlayers = players.size();
            for (ServerPlayerEntity player : players) {
                var role = gameWorldComponent.getRole(player);
                String roleId = role != null ? role.identifier().toString() : WatheRoles.CIVILIAN.identifier().toString();
                ServerPlayNetworking.send(player, new AnnounceWelcomePayload(roleId, killerCount, totalPlayers - killerCount));
            }
        }

        ci.cancel(); // Always cancel - we've done everything the original method does
    }
}

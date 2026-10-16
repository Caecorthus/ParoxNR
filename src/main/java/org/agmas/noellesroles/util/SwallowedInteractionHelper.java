package org.agmas.noellesroles.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

public final class SwallowedInteractionHelper {

    // Default rule: swallowed players cannot be targeted by interactions or skills
    // unless the caller opts into one of the explicit white-listed exceptions below.
    public enum TargetingRule {
        DEFAULT,
        VOODOO_TARGET,
        ASSASSIN_GUESS_TARGET,
        CRIMINAL_REASONER_SUSPECT,
        PHONISM_TARGET
    }

    private SwallowedInteractionHelper() {}

    public static boolean blocksActor(@Nullable PlayerEntity actor) {
        return actor != null && SwallowedPlayerComponent.isPlayerSwallowed(actor);
    }

    public static boolean blocksTarget(@Nullable Entity target) {
        return target instanceof PlayerEntity player && isUntargetablePlayer(player);
    }

    public static boolean blocksTargetForViewer(@Nullable PlayerEntity viewer, @Nullable Entity target) {
        return target instanceof PlayerEntity player && blocksPlayerTargetForViewer(viewer, player);
    }

    public static boolean blocksPlayerTarget(@Nullable PlayerEntity target) {
        return blocksPlayerTarget(target, TargetingRule.DEFAULT);
    }

    public static boolean blocksPlayerTargetForViewer(@Nullable PlayerEntity viewer, @Nullable PlayerEntity target) {
        if (target == null) {
            return false;
        }
        if (viewer != null
                && !target.getUuid().equals(viewer.getUuid())
                && LooseEndPlayerComponent.KEY.get(viewer).isOpeningPhased()) {
            return true;
        }
        return blocksPlayerTarget(target);
    }

    public static boolean blocksPlayerTarget(@Nullable PlayerEntity target, TargetingRule rule) {
        if (target == null || !isUntargetablePlayer(target)) {
            return false;
        }
        return !isWhitelisted(rule);
    }

    // These roles intentionally keep access to swallowed targets for their game logic.
    private static boolean isWhitelisted(TargetingRule rule) {
        return switch (rule) {
            case DEFAULT -> false;
            case VOODOO_TARGET, ASSASSIN_GUESS_TARGET, CRIMINAL_REASONER_SUSPECT, PHONISM_TARGET -> true;
        };
    }

    public static boolean isSwallowedTarget(@Nullable PlayerEntity target) {
        return target != null && SwallowedPlayerComponent.isPlayerSwallowed(target);
    }

    public static boolean isUntargetablePlayer(@Nullable PlayerEntity target) {
        return target != null && (SwallowedPlayerComponent.isPlayerSwallowed(target) || LooseEndPlayerComponent.KEY.get(target).isOpeningPhased());
    }
}

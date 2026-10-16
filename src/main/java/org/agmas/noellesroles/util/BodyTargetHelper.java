package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.scavenger.ScavengerBodyHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public final class BodyTargetHelper {
    public static final double DEFAULT_RANGE = 2.0D;
    public static final double SKILL_RANGE = 5.0D;
    public static final double SPECTATOR_RANGE = 8.0D;
    public static final double JUNKER_RANGE = 3.0D;

    private BodyTargetHelper() {}

    public static double getTargetRange(PlayerEntity player) {
        if (GameFunctions.isPlayerSpectatingOrCreative(player)) {
            return SPECTATOR_RANGE;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(player, Noellesroles.VULTURE) || gameWorld.isRole(player, Noellesroles.FERRYMAN)) {
            return SKILL_RANGE;
        }
        if (gameWorld.isRole(player, Noellesroles.JUNKER)) {
            return JUNKER_RANGE;
        }

        return DEFAULT_RANGE;
    }

    public static boolean canPlayerSeeBody(PlayerEntity player, PlayerBodyEntity body) {
        if (player == null || body == null || player.getWorld() != body.getWorld()) {
            return false;
        }

        if (GameFunctions.isPlayerSpectatingOrCreative(player)) {
            return true;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role role = gameWorld.getRole(player);
        if (role == null) {
            return false;
        }

        if (role.getMoodType() == Role.MoodType.REAL
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
            if (moodComponent.isLowerThanDepressed()) {
                return false;
            }
        }

        if (!ScavengerBodyHelper.isHiddenBody(body)) {
            return true;
        }

        Faction faction = role.getFaction();
        return faction == Faction.KILLER || faction == Faction.NEUTRAL;
    }

    @Nullable
    public static PlayerBodyEntity findTargetBody(PlayerEntity player, double range) {
        return findTargetBody(player, range, body -> true);
    }

    @Nullable
    public static PlayerBodyEntity findTargetBody(PlayerEntity player, double range, Predicate<PlayerBodyEntity> filter) {
        EntityHitResult hitResult = raycastBody(player, range, filter);
        if (hitResult != null && hitResult.getEntity() instanceof PlayerBodyEntity body) {
            return body;
        }
        return null;
    }

    @Nullable
    public static EntityHitResult raycastBody(PlayerEntity player, double range, Predicate<PlayerBodyEntity> filter) {
        Vec3d eyePos = player.getEyePos();
        Vec3d endPos = eyePos.add(player.getRotationVec(1.0F).multiply(range));
        HitResult blockHit = player.getWorld().raycast(new RaycastContext(
                eyePos,
                endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        double maxDistanceSq = range * range;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistanceSq = eyePos.squaredDistanceTo(blockHit.getPos());
        }

        Box searchBox = player.getBoundingBox().stretch(player.getRotationVec(1.0F).multiply(range)).expand(1.0D);

        final double finalMaxDistanceSq = maxDistanceSq;

        return player.getWorld().getEntitiesByClass(PlayerBodyEntity.class, searchBox,
                        body -> filter.test(body) && canPlayerSeeBody(player, body))
                .stream()
                .map(body -> raycastBody(eyePos, endPos, body))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(hit -> eyePos.squaredDistanceTo(hit.getPos()) <= finalMaxDistanceSq)
                .min(Comparator.comparingDouble(hit -> eyePos.squaredDistanceTo(hit.getPos())))
                .orElse(null);
    }

    private static Optional<EntityHitResult> raycastBody(Vec3d eyePos, Vec3d endPos, PlayerBodyEntity body) {
        return body.getBoundingBox()
                .expand(0.1D)
                .raycast(eyePos, endPos)
                .map(hitPos -> new EntityHitResult(body, hitPos));
    }
}

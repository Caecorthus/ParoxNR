package org.agmas.noellesroles.ferryman;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilter;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.FerrymanBodyAgeSyncS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class FerrymanHelper {
    public static final int COUNTER_STUN_AMPLIFIER = 6;
    public static final int CORPSE_RANGE = 5;
    public static final int DECOMPOSED_AGE = GameConstants.TIME_TO_DECOMPOSITION + GameConstants.DECOMPOSING_TIME;
    public static final int FERRY_TO_SKELETON_TICKS = GameConstants.getInTicks(0, 10);
    private static final Map<ServerWorld, Map<UUID, FerriedBodyProgress>> FERRIED_BODY_PROGRESS = new WeakHashMap<>();

    private FerrymanHelper() {}

    public static boolean isReactionDeathReason(Identifier deathReason) {
        return deathReason != GameConstants.DeathReasons.FELL_OUT_OF_TRAIN
                && deathReason != GameConstants.DeathReasons.ESCAPED
                && deathReason != Noellesroles.DEATH_REASON_ASSASSINATED
                && deathReason != Noellesroles.DEATH_REASON_VOODOO;
    }

    public static boolean canSpawn(dev.doctor4t.wathe.api.RoleSelectionContext ctx) {
        Role vulture = WatheRoles.getRole(Noellesroles.VULTURE_ID);
        return vulture == null || !ctx.isRoleAssigned(vulture);
    }

    public static PlayerBodyEntity findTargetBody(ServerPlayerEntity player, FerrymanPlayerComponent ferrymanComponent) {
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F).normalize();
        PlayerBodyEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PlayerBodyEntity body : player.getWorld().getEntitiesByType(
                TypeFilter.equals(PlayerBodyEntity.class),
                player.getBoundingBox().expand(CORPSE_RANGE),
                corpse -> corpse.getPlayerUuid() != null
                        && !ferrymanComponent.hasFerriedBody(corpse.getUuid())
                        && corpse.age < DECOMPOSED_AGE)) {
            Vec3d toBody = body.getPos().add(0.0, 0.6, 0.0).subtract(eyePos);
            double distance = toBody.length();
            if (distance > CORPSE_RANGE || distance <= 0.001D) continue;

            double alignment = look.dotProduct(toBody.normalize());
            if (alignment < 0.78D) continue;
            if (!player.canSee(body)) continue;

            double score = alignment - distance * 0.02D;
            if (score > bestScore) {
                bestScore = score;
                best = body;
            }
        }

        return best;
    }

    public static void markBodyFerried(ServerWorld world, PlayerBodyEntity body) {
        if (body.age >= DECOMPOSED_AGE) {
            return;
        }

        // 引渡优先于不腐处理：解除防腐登记，让该尸体照常加速白骨化
        FerrymanPreserveWorldComponent.KEY.get(world).unpreserve(body.getUuid());

        FERRIED_BODY_PROGRESS
                .computeIfAbsent(world, ignored -> new HashMap<>())
                .put(body.getUuid(), new FerriedBodyProgress(body.age, world.getTime()));
        syncBodyAge(world, body);
    }

    private static void updateFerriedBodies(ServerWorld world) {
        Map<UUID, FerriedBodyProgress> ferriedBodies = FERRIED_BODY_PROGRESS.get(world);
        if (ferriedBodies == null || ferriedBodies.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, FerriedBodyProgress>> iterator = ferriedBodies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FerriedBodyProgress> entry = iterator.next();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof PlayerBodyEntity body)) {
                iterator.remove();
                continue;
            }

            FerriedBodyProgress progress = entry.getValue();
            long elapsed = Math.max(0L, now - progress.startTime());
            int desiredAge;
            if (elapsed >= FERRY_TO_SKELETON_TICKS) {
                desiredAge = DECOMPOSED_AGE;
            } else {
                float ratio = elapsed / (float) FERRY_TO_SKELETON_TICKS;
                desiredAge = progress.startAge() + Math.round((DECOMPOSED_AGE - progress.startAge()) * ratio);
            }

            desiredAge = Math.max(body.age, desiredAge);
            if (body.age != desiredAge) {
                body.age = desiredAge;
                syncBodyAge(world, body);
            }

            if (desiredAge >= DECOMPOSED_AGE) {
                iterator.remove();
            }
        }

        if (ferriedBodies.isEmpty()) {
            FERRIED_BODY_PROGRESS.remove(world);
        }
    }

    /**
     * 不腐处理中的尸体：每tick把age钳制回冻结值，使其永不腐化、永远可被引渡与乘务登记。
     * age同步沿用 {@link #syncBodyAge} 广播给所有玩家，因此不腐对全体阵营成立；
     * “被处理过”的名单与涂蜡特效则只发给验尸官、杀手与中立（见 FerrymanPreserveWorldComponent）。
     */
    public static void updatePreservedBodies(ServerWorld world) {
        FerrymanPreserveWorldComponent preserveComponent = FerrymanPreserveWorldComponent.KEY.get(world);
        if (preserveComponent.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Integer> entry : preserveComponent.snapshot().entrySet()) {
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof PlayerBodyEntity body)) {
                preserveComponent.unpreserve(entry.getKey());
                continue;
            }

            int frozenAge = entry.getValue();
            if (body.age > frozenAge) {
                body.age = frozenAge;
                syncBodyAge(world, body);
            }
        }
    }

    private static void syncBodyAge(ServerWorld world, PlayerBodyEntity body) {
        FerrymanBodyAgeSyncS2CPacket syncPacket = new FerrymanBodyAgeSyncS2CPacket(body.getId(), body.age);
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, syncPacket);
        }
    }

    /**
     * Handles ferryman reaction logic in KillPlayer.BEFORE.
     * Returns a KillResult if the kill should be cancelled, null otherwise.
     */
    /**
     * Handles ferryman pending death resolution in the tick event.
     */
    public static void handleTick(ServerWorld world, GameWorldComponent gc) {
        updateFerriedBodies(world);
        updatePreservedBodies(world);

        for (UUID uuid : gc.getAllWithRole(Noellesroles.FERRYMAN)) {
            PlayerEntity ferrymanEntity = world.getPlayerByUuid(uuid);
            if (!(ferrymanEntity instanceof ServerPlayerEntity ferryman) || !GameFunctions.isPlayerPlayingAndAlive(ferryman)) continue;

            FerrymanPlayerComponent ferrymanComponent = FerrymanPlayerComponent.KEY.get(ferryman);
            if (ferrymanComponent.isReactionActive()) continue;

            Identifier pendingDeathReason = ferrymanComponent.getPendingDeathReason();
            FerrymanPlayerComponent.ReactionType pendingReactionType = ferrymanComponent.getPendingReactionType();
            if (pendingReactionType == FerrymanPlayerComponent.ReactionType.DEATH && pendingDeathReason == null) continue;
            if (pendingReactionType != FerrymanPlayerComponent.ReactionType.DEATH
                    && ferrymanComponent.getPendingAttackerUuid() == null) continue;

            ServerPlayerEntity attacker = null;
            UUID attackerUuid = ferrymanComponent.getPendingAttackerUuid();
            if (attackerUuid != null) {
                PlayerEntity attackerEntity = world.getPlayerByUuid(attackerUuid);
                if (attackerEntity instanceof ServerPlayerEntity serverAttacker) {
                    attacker = serverAttacker;
                }
            }

            ferrymanComponent.clearReaction();
            AbilityPlayerComponent.KEY.get(ferryman).setCooldown(1);
            if (pendingReactionType == FerrymanPlayerComponent.ReactionType.COUNTER_SWALLOW) {
                if (attacker instanceof ServerPlayerEntity taotie) {
                    var taotieComponent = org.agmas.noellesroles.taotie.TaotiePlayerComponent.KEY.get(taotie);
                    taotieComponent.forceSwallowPlayer(ferryman);
                }
            } else {
                GameFunctions.killPlayer(ferryman, true, attacker, pendingDeathReason);
            }
        }
    }

    @Nullable
    public static KillPlayer.KillResult handleBeforeKill(PlayerEntity victim, PlayerEntity killer, Identifier deathReason, GameWorldComponent gameWorldComponent) {
        if (!(victim instanceof ServerPlayerEntity serverVictim)) return null;
        if (!gameWorldComponent.isRole(serverVictim, Noellesroles.FERRYMAN)) return null;
        if (!GameFunctions.isPlayerPlayingAndAlive(serverVictim)) return null;
        if (SwallowedPlayerComponent.isPlayerSwallowed(serverVictim)) return null;
        if (!isReactionDeathReason(deathReason)) return null;

        FerrymanPlayerComponent ferrymanComponent = FerrymanPlayerComponent.KEY.get(serverVictim);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(serverVictim);
        if (ferrymanComponent.isReactionActive() || abilityComponent.getCooldown() > 0) return null;

        UUID attackerUuid = killer != null ? killer.getUuid() : null;
        if (ferrymanComponent.beginReaction(attackerUuid, deathReason)) {
            serverVictim.getWorld().sendEntityStatus(serverVictim, EntityStatuses.ADD_PORTAL_PARTICLES);
            return KillPlayer.KillResult.cancel();
        }

        return null;
    }

    public static boolean beginSwallowReaction(ServerPlayerEntity victim, @Nullable ServerPlayerEntity taotie, GameWorldComponent gameWorldComponent) {
        if (!gameWorldComponent.isRole(victim, Noellesroles.FERRYMAN)) return false;
        if (!GameFunctions.isPlayerPlayingAndAlive(victim)) return false;
        if (SwallowedPlayerComponent.isPlayerSwallowed(victim)) return false;

        FerrymanPlayerComponent ferrymanComponent = FerrymanPlayerComponent.KEY.get(victim);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(victim);
        if (ferrymanComponent.isReactionActive() || abilityComponent.getCooldown() > 0) return false;

        UUID attackerUuid = taotie != null ? taotie.getUuid() : null;
        if (ferrymanComponent.beginSwallowReaction(attackerUuid)) {
            victim.getWorld().sendEntityStatus(victim, EntityStatuses.ADD_PORTAL_PARTICLES);
            return true;
        }

        return false;
    }

    private record FerriedBodyProgress(int startAge, long startTime) {}
}

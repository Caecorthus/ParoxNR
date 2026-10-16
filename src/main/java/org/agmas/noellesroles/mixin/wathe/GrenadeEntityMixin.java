package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import org.agmas.noellesroles.hallucination.HallucinationDummyInteractionHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.AreaDamageImmunityHelper;
import org.agmas.noellesroles.util.GrenadeBlastVisibilityHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(GrenadeEntity.class)
public class GrenadeEntityMixin {

    /**
     * 爆炸开始时正处于被吞状态的玩家快照。
     * Wathe 手雷在 onCollision 里一次性收集受害者列表后逐个 killPlayer：
     * 若饕餮在列表中被先击杀，KillPlayer.AFTER 会立刻把被吞者释放到爆炸点，
     * 轮到被吞者时他已不再是"被吞"状态，会被同一颗手雷误杀。
     * 快照保证爆炸瞬间在肚子里的玩家整颗手雷全程免疫，无论中途是否被释放。
     */
    @Unique
    private final List<UUID> noellesroles$swallowedAtExplosion = new ArrayList<>();

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void noellesroles$snapshotAndKillHallucinationDummies(HitResult hitResult, CallbackInfo ci) {
        GrenadeEntity grenade = (GrenadeEntity) (Object) this;
        if (grenade.getWorld() instanceof ServerWorld serverWorld) {
            for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                if (SwallowedPlayerComponent.isPlayerSwallowed(p)) {
                    noellesroles$swallowedAtExplosion.add(p.getUuid());
                }
            }
        }

        Entity owner = grenade.getOwner();
        if (!(owner instanceof ServerPlayerEntity viewer)) {
            return;
        }
        Box explosionBox = grenade.getBoundingBox().expand(3.0D);
        HallucinationDummyInteractionHelper.killDummiesInBox(
                viewer,
                explosionBox,
                GameConstants.DeathReasons.GRENADE,
                true,
                null,
                null,
                null
        );
    }

    @Redirect(
            method = "onCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;)V"
            )
    )
    private void noellesroles$skipAreaDamageImmunePlayers(
            ServerPlayerEntity victim,
            boolean deadBody,
            ServerPlayerEntity killer,
            net.minecraft.util.Identifier deathReason
    ) {
        if (AreaDamageImmunityHelper.isImmuneToAreaDamage(victim)
                || noellesroles$swallowedAtExplosion.contains(victim.getUuid())
                || !GrenadeBlastVisibilityHelper.hasBlastLineOfSight((GrenadeEntity) (Object) this, victim)) {
            return;
        }
        GameFunctions.killPlayer(victim, deadBody, killer, deathReason);
    }
}

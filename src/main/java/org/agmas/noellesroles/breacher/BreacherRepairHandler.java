package org.agmas.noellesroles.breacher;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.agmas.noellesroles.packet.SchulteClickC2SPacket;
import org.agmas.noellesroles.packet.SchulteExitC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * 右键【破坏点】标记进入舒尔特方格修复小游戏，以及小游戏点击/退出包的服务端入口。
 */
public class BreacherRepairHandler {

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                noellesroles$onUseMarker(player, entity));

        ServerPlayNetworking.registerGlobalReceiver(SchulteClickC2SPacket.ID, (payload, context) ->
                BreacherWorldComponent.KEY.get(context.player().getWorld())
                        .handleRepairClick(context.player(), payload.pointId(), payload.level(), payload.number()));

        ServerPlayNetworking.registerGlobalReceiver(SchulteExitC2SPacket.ID, (payload, context) ->
                BreacherWorldComponent.KEY.get(context.player().getWorld())
                        .handleRepairExit(context.player(), payload.pointId(), payload.level()));
    }

    private static ActionResult noellesroles$onUseMarker(net.minecraft.entity.player.PlayerEntity player, Entity entity) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        if (!BreacherWorldComponent.isBreacherMarker(entity)) return ActionResult.PASS;
        if (!(serverPlayer.getWorld() instanceof ServerWorld serverWorld)) return ActionResult.SUCCESS;

        BreacherWorldComponent worldComp = BreacherWorldComponent.KEY.get(serverWorld);
        BreacherWorldComponent.PointState point = worldComp.findPointByMarker(entity.getUuid());
        if (point == null) return ActionResult.PASS;

        if (!GameFunctions.isPlayerPlayingAndAlive(serverPlayer)
                || SwallowedPlayerComponent.isPlayerSwallowed(serverPlayer)) {
            return ActionResult.SUCCESS;
        }

        worldComp.startOrJoinRepair(serverPlayer, point.id);
        return ActionResult.SUCCESS;
    }
}

package org.agmas.noellesroles.mixin.corruptcop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.gangsterstar.GangsterStarPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunShootPayload.Receiver.class)
public abstract class CorruptCopGunCooldownMixin {
    private static final int LOOSE_END_REVOLVER_COOLDOWN_TICKS = 20 * 60 * 3;

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, remap = false)
    private void noellesroles$blockGunOnSwallowedPlayer(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (SwallowedInteractionHelper.blocksActor(player)) {
            ci.cancel();
            return;
        }

        Entity target = player.getServerWorld().getEntityById(payload.target());
        if (SwallowedInteractionHelper.blocksTargetForViewer(player, target)) {
            ci.cancel();
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        ItemStack stack = player.getMainHandStack();
        boolean isGangsterStarRevolverShot = gameWorldComponent.isRole(player, Noellesroles.GANGSTER_STAR)
                && stack.isOf(WatheItems.REVOLVER);
        // 客户端也会提前拦截用于即时反馈，但最终仍以服务端判定为准。
        if (isGangsterStarRevolverShot
                && !GangsterStarPlayerComponent.KEY.get(player).hasRevolverShot()) {
            player.sendMessage(net.minecraft.text.Text.translatable("tip.noellesroles.gangster_star.no_shots"), true);
            // 空仓音效与德林加一致（服务端广播，周围玩家也能听见）；冷却中保持静默
            if (!player.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER)) {
                player.getWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                        WatheSounds.ITEM_REVOLVER_CLICK, SoundCategory.PLAYERS,
                        0.5f, 1f + player.getRandom().nextFloat() * .1f - .05f);
            }
            ci.cancel();
            return;
        }

        if (isGangsterStarRevolverShot
                && !player.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER)) {
            // 服务端确认本次左轮请求可执行后立即扣弹，未命中也会消耗一次可用次数。
            GangsterStarPlayerComponent.KEY.get(player).consumeRevolverShot();
        }
    }

    /**
     * 黑警时刻期间枪冷却变为2秒
     */
    @Inject(method = "receive", at = @At("RETURN"), remap = false)
    private void modifyGunCooldownForCorruptCop(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player, Noellesroles.CORRUPT_COP)) {
            CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(player);
            int customCooldown = corruptCopComp.getGunCooldown();
            if (customCooldown > 0) {
                // 重置为黑警时刻的冷却时间
                player.getItemCooldownManager().set(WatheItems.REVOLVER, customCooldown);
            }
        }

        if (gameWorldComponent.isRole(player, dev.doctor4t.wathe.api.WatheRoles.LOOSE_END)) {
            LooseEndPlayerComponent looseEndComponent = LooseEndPlayerComponent.KEY.get(player);
            int queuedCooldown = looseEndComponent.consumeQueuedRevolverCooldown();
            if (queuedCooldown > 0) {
                player.getItemCooldownManager().set(WatheItems.REVOLVER, queuedCooldown);
            }
        }
    }

    @Inject(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/class_3222;ZLnet/minecraft/class_3222;Lnet/minecraft/class_2960;)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            ),
            remap = false
    )
    private void noellesroles$handleLooseEndRevolverHit(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        Entity target = player.getServerWorld().getEntityById(payload.target());
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player, dev.doctor4t.wathe.api.WatheRoles.LOOSE_END)
                && target instanceof PlayerEntity targetPlayer
                && dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
            LooseEndPlayerComponent looseEndComponent = LooseEndPlayerComponent.KEY.get(player);
            looseEndComponent.consumeOneRevolver();
            looseEndComponent.queueRevolverCooldown(LOOSE_END_REVOLVER_COOLDOWN_TICKS);
        }

    }
}

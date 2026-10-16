package org.agmas.noellesroles.client.mixin.attendant;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 手持乘务登记表右键尸体时，让原版交互绕过尸体不可被准星选中的限制。 */
@Mixin(MinecraftClient.class)
public abstract class AttendantBodyInteractionMixin {
    private static final double REGISTER_RANGE = 3.0D;

    @Shadow public ClientPlayerInteractionManager interactionManager;
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;
    @Shadow public HitResult crosshairTarget;
    @Shadow private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$interactVisibleBodyWithAttendantRegister(CallbackInfo ci) {
        if (this.interactionManager == null || this.player == null || this.world == null) {
            return;
        }
        if (this.interactionManager.isBreakingBlock() || this.player.isRiding()) {
            return;
        }
        if (this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            return;
        }

        for (Hand hand : Hand.values()) {
            ItemStack stack = this.player.getStackInHand(hand);
            if (!stack.isOf(ModItems.ATTENDANT_REGISTER) || !stack.isItemEnabled(this.world.getEnabledFeatures())) {
                continue;
            }

            EntityHitResult bodyHit = BodyTargetHelper.raycastBody(this.player, REGISTER_RANGE, body -> true);
            if (bodyHit == null || !(bodyHit.getEntity() instanceof PlayerBodyEntity body)) {
                continue;
            }
            if (!this.world.getWorldBorder().contains(body.getBlockPos())) {
                continue;
            }

            ActionResult result = this.interactionManager.interactEntityAtLocation(this.player, body, bodyHit, hand);
            if (!result.isAccepted()) {
                result = this.interactionManager.interactEntity(this.player, body, hand);
            }
            if (!result.isAccepted()) {
                continue;
            }

            this.itemUseCooldown = 4;
            if (result.shouldSwingHand()) {
                this.player.swingHand(hand);
            }
            ci.cancel();
            return;
        }
    }
}

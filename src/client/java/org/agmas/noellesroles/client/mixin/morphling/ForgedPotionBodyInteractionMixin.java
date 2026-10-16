package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.ForgedPotionItem;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 手持已指定目标的赝造药水右键尸体时，让原版交互绕过尸体不可被准星选中的限制。 */
@Mixin(MinecraftClient.class)
public abstract class ForgedPotionBodyInteractionMixin {
    @Shadow public ClientPlayerInteractionManager interactionManager;
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;
    @Shadow public HitResult crosshairTarget;
    @Shadow private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$interactBodyWithForgedPotion(CallbackInfo ci) {
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
            if (!stack.isOf(ModItems.FORGED_POTION) || !stack.isItemEnabled(this.world.getEnabledFeatures())) {
                continue;
            }
            // 未指定目标时不认领这次点击，让原版落到 use() 打开选人界面
            if (ForgedPotionItem.getTarget(stack) == null) {
                continue;
            }

            EntityHitResult bodyHit = BodyTargetHelper.raycastBody(this.player, BodyTargetHelper.SKILL_RANGE, body -> true);
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

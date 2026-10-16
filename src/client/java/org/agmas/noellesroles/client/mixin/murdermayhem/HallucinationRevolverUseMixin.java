package org.agmas.noellesroles.client.mixin.murdermayhem;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.hallucination.ClientHallucinationState;
import org.agmas.noellesroles.gangsterstar.GangsterStarPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.doctor4t.wathe.item.RevolverItem")
public class HallucinationRevolverUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void noellesroles$shootHallucinationDummy(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        if (!world.isClient) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != user) {
            return;
        }
        // RevolverItem 会先在客户端播放后座和枪口反馈，再等待服务端拒绝数据包。
        // 黑帮巨星没有剩余子弹时在本地跳过开火反馈，但仍发送请求，由服务端广播空仓音效。
        if (hand == Hand.MAIN_HAND
                && user.getMainHandStack().isOf(WatheItems.REVOLVER)
                && GameWorldComponent.KEY.get(user.getWorld()).isRole(user, Noellesroles.GANGSTER_STAR)
                && !GangsterStarPlayerComponent.KEY.get(user).hasRevolverShot()) {
            HitResult collision = RevolverItem.getGunTarget(user);
            ClientPlayNetworking.send(new GunShootPayload(RevolverItem.resolveTargetFromHitResult(world, collision)));
            user.sendMessage(Text.translatable("tip.noellesroles.gangster_star.no_shots"), true);
            cir.setReturnValue(TypedActionResult.fail(user.getStackInHand(hand)));
            return;
        }
        Identifier deathReason = GameConstants.DeathReasons.GUN;
        if (!ClientHallucinationState.tryHitDummy(client, 30.0D, deathReason)) {
            return;
        }
        user.setPitch(user.getPitch() - 4.0F);
        cir.setReturnValue(TypedActionResult.consume(user.getStackInHand(hand)));
    }
}

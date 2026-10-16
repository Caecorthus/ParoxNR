package org.agmas.noellesroles.client.mixin.toxicologist;

import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.toxicologist.AntidotePlateShieldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 毒理学家视野：带解毒剂预防护的餐盘/饮品托盘持续冒出明绿色（高兴村民）粒子。
 * 护盾数据通过世界组件仅同步给毒理学家，这里再做角色校验作为兜底。
 */
@Mixin(BeveragePlateBlockEntity.class)
public class AntidoteShieldPlateViewMixin {

    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void shieldParticles(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof BeveragePlateBlockEntity)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(client.player, Noellesroles.TOXICOLOGIST)) return;

        if (!AntidotePlateShieldComponent.KEY.get(world).hasShield(pos)) return;

        if (world.random.nextInt(5) != 0) return;
        world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F,
                0.0F, 0.15F, 0.0F);
    }
}

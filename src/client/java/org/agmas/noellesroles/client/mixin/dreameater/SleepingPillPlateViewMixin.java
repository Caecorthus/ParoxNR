package org.agmas.noellesroles.client.mixin.dreameater;

import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModParticles;
import org.agmas.noellesroles.dreameater.SleepingPillPlateComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 安眠药餐盘的蓝色粒子。
 * 安眠药盘由世界组件标记（按「下毒物品」区分），与普通毒药盘互不影响：
 * - 杀手阵营与旁观信息视角：显示蓝色粒子，并压掉原版红色毒粒子；
 * - 毒理学家：什么都不显示（同样压掉原版红色，因为安眠药盘 poisoner 非空本会走 CanSeePoison 红粒子）；
 * - 其他乘客：本来不可见，不处理。
 * order = 900 低于 wathe 客户端 mixin 的默认 1000，先应用先执行，HEAD 处 cancel 可跳过原版红色粒子。
 */
@Mixin(BeveragePlateBlockEntity.class)
public class SleepingPillPlateViewMixin {

    @Inject(method = "clientTick", at = @At("HEAD"), order = 900, cancellable = true)
    private static void noellesroles$sleepingPillParticles(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof BeveragePlateBlockEntity tray)) return;
        String poisoner = tray.getPoisoner();
        if (poisoner == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        SleepingPillPlateComponent pillComponent = SleepingPillPlateComponent.KEY.get(world);
        if (!pillComponent.isPillPlate(pos, poisoner)) return;

        // 安眠药盘：隐藏原版红色毒粒子
        ci.cancel();

        // 仅杀手阵营与旁观信息视角可见蓝色粒子；毒理学家不可见
        if (!WatheClient.isKiller() && !WatheClient.canSeeSpectatorInformation()) return;

        if (world.getRandom().nextBetween(0, 20) < 17) return;
        world.addParticle(
                ModParticles.SLEEPING_PILL_POISON,
                pos.getX() + 0.5f,
                pos.getY(),
                pos.getZ() + 0.5f,
                0f, 0.05f, 0f
        );
    }
}

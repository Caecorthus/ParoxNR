package org.agmas.noellesroles.client.mixin.wathe;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.gui.KnifeChargeHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 准星附近的匕首条分工：
 * 蓄力时 Wathe 的匕首条不绘制，由 {@link KnifeChargeHudRenderer} 在完全相同的位置顶上（准星下方 5 像素）；
 * 非蓄力时保持 Wathe 原样（冷却进度条 / 命中目标提示）。
 * 蓄力期间 Wathe 的 target 标记不再触发，因此这里补上准星的命中变体，仅在整把匕首变红时显示。
 */
@Mixin(CrosshairRenderer.class)
public class KnifeChargeHudMixin {
    @Unique
    private static final Identifier TARGET_CROSSHAIR = Identifier.of("wathe", "hud/crosshair_target");

    /** 主手匕首且正在蓄力时让 isOf(KNIFE) 返回 false，整个匕首分支就不会执行 */
    @WrapOperation(
            method = "renderCrosshair",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z")
    )
    private static boolean noellesroles$hideWatheKnifeBar(ItemStack stack, Item item, Operation<Boolean> original) {
        if (item == WatheItems.KNIFE && KnifeChargeHudRenderer.isCharging(MinecraftClient.getInstance().player)) {
            return false;
        }
        return original.call(stack, item);
    }

    /** 蓄力完成且瞄准可刺杀目标（整把匕首变红）时，准星切换成 Wathe 的命中变体 */
    @ModifyExpressionValue(
            method = "renderCrosshair",
            at = @At(value = "FIELD", target = "Ldev/doctor4t/wathe/client/gui/CrosshairRenderer;CROSSHAIR:Lnet/minecraft/util/Identifier;")
    )
    private static Identifier noellesroles$showTargetCrosshair(Identifier original) {
        return KnifeChargeHudRenderer.isChargingAtTarget(MinecraftClient.getInstance().player)
                ? TARGET_CROSSHAIR
                : original;
    }

    /** TAIL 只会命中方法末尾的 return，非第一人称的提前返回不会触发，此时矩阵也已回到 HUD 基准坐标系 */
    @Inject(method = "renderCrosshair", at = @At("TAIL"), remap = false)
    private static void noellesroles$renderKnifeChargeBar(
            MinecraftClient client,
            ClientPlayerEntity player,
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        KnifeChargeHudRenderer.render(player, context);
    }
}

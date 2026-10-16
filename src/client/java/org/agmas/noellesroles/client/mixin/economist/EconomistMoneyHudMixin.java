package org.agmas.noellesroles.client.mixin.economist;

import dev.doctor4t.wathe.client.gui.StoreRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.economist.EconomistClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 经济学家的观测行：绘制在右上角金币行正下方，
 * 金币部分复用原版金币字形与数字渲染器，显示"当前观测目标：[金币图标+数量]"。
 */
@Mixin(StoreRenderer.class)
public abstract class EconomistMoneyHudMixin {

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void noellesroles$renderEconomistObservation(TextRenderer renderer,
                                                               ClientPlayerEntity player,
                                                               DrawContext context,
                                                               float delta,
                                                               CallbackInfo ci) {
        if (!EconomistClientState.isObserving()) {
            return;
        }

        int observedBalance = EconomistClientState.getObservedBalance();
        int colour = 0xFFFFFFFF;
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() - 12,
                EconomistClientState.OBSERVATION_LINE_Y, 0);

        EconomistClientState.view().render(renderer, context, 0, 0, colour, delta);

        Text label = Text.translatable("hud.economist.observed_target");
        int moneyWidth = 8 + String.valueOf(observedBalance).length() * 8;
        context.getMatrices().translate(-moneyWidth - 2, 0, 0);
        context.drawTextWithShadow(renderer, label, -renderer.getWidth(label), 0, colour);
        context.getMatrices().pop();
    }
}

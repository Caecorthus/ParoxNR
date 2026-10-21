package org.agmas.noellesroles.client.mixin.puppeteer;

import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 准星对准傀儡 / 本体替身时，像对准玩家一样在准星下方显示名字（傀儡显示死者名，替身显示傀儡师名），
 * 位置与缩放同 Wathe 的玩家名牌；Wathe 因光线过暗不显示名牌时这里也不会执行（TAIL 只在正常结尾触发）。
 */
@Mixin(RoleNameRenderer.class)
public abstract class PuppetNameTagMixin {
    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void noellesroles$renderPuppetName(TextRenderer renderer, ClientPlayerEntity player, DrawContext context,
                                                      RenderTickCounter tickCounter, CallbackInfo ci) {
        PuppetEntity puppet = PuppeteerClient.findCrosshairPuppet(player);
        if (puppet == null) {
            return;
        }
        Text name = PuppeteerClient.getName(puppet.getSkinUuid());
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, name, -renderer.getWidth(name) / 2, 16, 0xFFFFFFFF);
        context.getMatrices().pop();
    }
}

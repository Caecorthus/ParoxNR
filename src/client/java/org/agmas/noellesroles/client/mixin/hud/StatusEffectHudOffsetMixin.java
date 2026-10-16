package org.agmas.noellesroles.client.mixin.hud;

import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.economist.EconomistClientState;
import org.agmas.noellesroles.client.gui.TopRightInfoHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 当右上角金币 HUD 或信息块可见时，把状态效果图标整体下移到它们下方。
 */
@Mixin(InGameHud.class)
public abstract class StatusEffectHudOffsetMixin {
    private static final int MONEY_HUD_BOTTOM_Y = 18;

    @ModifyArg(
            method = "renderStatusEffectOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V",
                    ordinal = 0
            ),
            index = 2
    )
    private int noellesroles$offsetAmbientEffectBackgroundY(int y) {
        return noellesroles$offsetStatusEffectHudY(y);
    }

    @ModifyArg(
            method = "renderStatusEffectOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V",
                    ordinal = 1
            ),
            index = 2
    )
    private int noellesroles$offsetNormalEffectBackgroundY(int y) {
        return noellesroles$offsetStatusEffectHudY(y);
    }

    @ModifyArg(
            method = "method_18620",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawSprite(IIIIILnet/minecraft/client/texture/Sprite;)V"
            ),
            index = 1
    )
    private static int noellesroles$offsetEffectSpriteY(int y) {
        return noellesroles$offsetStatusEffectHudY(y - 3) + 3;
    }

    private static int noellesroles$offsetStatusEffectHudY(int originalY) {
        int bottomY = -1;
        if (noellesroles$shouldShiftStatusEffectsBelowMoneyHud()) {
            bottomY = MONEY_HUD_BOTTOM_Y;
            if (EconomistClientState.isObserving()) {
                bottomY += EconomistClientState.OBSERVATION_LINE_HEIGHT;
            }
        }
        bottomY = Math.max(bottomY, TopRightInfoHudRenderer.getHudBottomY());
        if (bottomY < 0) {
            return originalY;
        }
        return Math.max(originalY, bottomY);
    }

    private static boolean noellesroles$shouldShiftStatusEffectsBelowMoneyHud() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            return false;
        }
        if (WatheClient.trainComponent == null || !WatheClient.trainComponent.hasHud()) {
            return false;
        }
        return CanSeeMoney.EVENT.invoker().canSee(player) == CanSeeMoney.Result.ALLOW;
    }
}

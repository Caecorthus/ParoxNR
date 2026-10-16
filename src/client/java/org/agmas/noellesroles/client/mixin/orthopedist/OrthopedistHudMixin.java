package org.agmas.noellesroles.client.mixin.orthopedist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.gui.HallucinationHudRenderer;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class OrthopedistHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Unique
    private boolean noellesroles$cachedIsOrthopedist = false;
    @Unique
    private long noellesroles$lastRoleCheckTick = -1;

    @Inject(method = "render", at = @At("TAIL"))
    public void noellesroles$renderOrthopedistHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (HallucinationHudRenderer.shouldSuppressSkillHintHud()) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        PlayerEntity localPlayer = MinecraftClient.getInstance().player;
        long currentTick = localPlayer.getWorld().getTime();
        if (currentTick != noellesroles$lastRoleCheckTick) {
            noellesroles$lastRoleCheckTick = currentTick;
            noellesroles$cachedIsOrthopedist = GameWorldComponent.KEY.get(localPlayer.getWorld()).isRole(localPlayer, Noellesroles.ORTHOPEDIST);
        }
        if (!noellesroles$cachedIsOrthopedist) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(localPlayer);
        int drawY = context.getScaledWindowHeight();
        Text line;

        if (ability.getCooldown() > 0) {
            line = Text.translatable("tip.noellesroles.cooldown", ability.getCooldown() / 20);
        } else {
            PlayerEntity target = NoellesrolesClient.crosshairTarget;
            if (target != null && NoellesrolesClient.crosshairTargetDistance <= 3.0 && localPlayer.canSee(target)) {
                line = Text.translatable("tip.orthopedist.buff", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText(), target.getName());
            } else {
                line = Text.translatable("tip.orthopedist.no_target");
            }
        }

        drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
        HudRenderHelper.drawRightAligned(context, getTextRenderer(), line, 0, drawY, Noellesroles.ORTHOPEDIST.color());
    }
}

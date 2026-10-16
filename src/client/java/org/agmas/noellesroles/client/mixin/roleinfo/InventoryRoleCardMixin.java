package org.agmas.noellesroles.client.mixin.roleinfo;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.gui.InventoryRoleCardOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the book-styled role card under the inventory panel.
 * Hooked into the background pass so item tooltips still draw over it.
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class InventoryRoleCardMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    public InventoryRoleCardMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    void noellesroles$renderRoleCard(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryRoleCardOverlay.render(context, MinecraftClient.getInstance().textRenderer, this.height);
    }
}

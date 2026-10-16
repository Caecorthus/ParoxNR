package org.agmas.noellesroles.mixin.bodyguard;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayNetworkHandler.class)
public class BodyguardSelectedSlotMixin {
    @Shadow
    public ServerPlayerEntity player;

    // 姿态期间禁止切换热栏槽位(服务端兜底,防改包)
    @WrapMethod(method = "onUpdateSelectedSlot")
    private void noellesroles$blockBodyguardStanceSlotSwitch(UpdateSelectedSlotC2SPacket packet, Operation<Void> original) {
        if (BodyguardPlayerComponent.KEY.get(this.player).isStanceActive()
                && packet.getSelectedSlot() != this.player.getInventory().selectedSlot) {
            return;
        }
        original.call(packet);
    }
}

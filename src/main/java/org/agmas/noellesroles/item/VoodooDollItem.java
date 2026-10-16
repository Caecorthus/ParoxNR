package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.OpenVoodooPanelS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.List;

public class VoodooDollItem extends Item {
    public VoodooDollItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(user, Noellesroles.VOODOO) && !gameWorld.isRole(user, Noellesroles.JUNKER)) {
            return TypedActionResult.pass(stack);
        }
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (user instanceof ServerPlayerEntity serverUser
                && GameFunctions.isPlayerPlayingAndAlive(user)
                && !SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            ServerPlayNetworking.send(serverUser, new OpenVoodooPanelS2CPacket());
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.voodoo_doll.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}

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
import org.agmas.noellesroles.packet.OpenAssassinBookPanelS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.List;

public class AssassinBookItem extends Item {
    /** 猜测结算后物品冷却：30 秒。 */
    public static final int COOLDOWN_TICKS = 20 * 30;

    public AssassinBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(user, Noellesroles.ASSASSIN) && !gameWorld.isRole(user, Noellesroles.JUNKER)) {
            return TypedActionResult.pass(stack);
        }
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (user instanceof ServerPlayerEntity serverUser
                && GameFunctions.isPlayerPlayingAndAlive(user)
                && !SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            ServerPlayNetworking.send(serverUser, new OpenAssassinBookPanelS2CPacket());
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.assassin_book.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}

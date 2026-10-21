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
import org.agmas.noellesroles.packet.OpenSilenceZonePanelS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.List;

/**
 * 寂静装置：静语者商店道具。
 * 右键打开选人菜单，点击头像即在目标玩家周围创造 25 秒的无声区域；使用后冷却 30 秒。
 */
public class SilenceDeviceItem extends Item {
    /** 使用后物品冷却：30 秒。 */
    public static final int COOLDOWN_TICKS = 20 * 30;

    public SilenceDeviceItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(user, Noellesroles.SILENCER)) {
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
            ServerPlayNetworking.send(serverUser, new OpenSilenceZonePanelS2CPacket());
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // 按 \n 手动分行并统一灰色渲染（tooltip 不会自动处理换行符）
        String full = Text.translatable("item.noellesroles.silence_device.tooltip").getString();
        for (String line : full.split("\n")) {
            tooltip.add(Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.recaller.RecallerAnchorWorldComponent;
import org.agmas.noellesroles.recaller.RecallerSounds;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.List;

/**
 * 回溯者商店道具【锚点】：右键在当前位置精确坐标放置锚点。
 * 场上同时只存在一个锚点，后放置的取代先前的；音效与提示仅操作者本人可感知。
 */
public class RecallerAnchorItem extends Item {
    public RecallerAnchorItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity serverUser)
                || !GameFunctions.isPlayerPlayingAndAlive(serverUser)
                || SwallowedPlayerComponent.isPlayerSwallowed(serverUser)) {
            return TypedActionResult.pass(stack);
        }

        RecallerAnchorWorldComponent comp = RecallerAnchorWorldComponent.KEY.get(world);
        comp.placeAnchor(serverUser); // 内含取代旧锚点
        stack.decrement(1);

        RecallerSounds.playToSelf(serverUser, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, 0.9F, 1.0F);
        serverUser.sendMessage(Text.translatable("tip.recaller.anchor_placed"), true);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "place");
        extra.putDouble("x", serverUser.getX());
        extra.putDouble("y", serverUser.getY());
        extra.putDouble("z", serverUser.getZ());
        GameRecordManager.recordSkillUse(serverUser, Noellesroles.RECALLER_ID, null, extra);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // tooltip 内嵌换行需拆行渲染（同 ShopIconItem）；灰色对齐 wathe 商店物品描述
        for (String line : Text.translatable("item.noellesroles.recaller_anchor.tooltip").getString().split("\\R")) {
            tooltip.add(Text.literal(line).withColor(0x808080));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

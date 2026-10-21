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
 * 回溯者商店道具【回溯碎片】：右键捏碎传送到锚点并消耗 1 个（无冷却，消耗即代价）。
 * 场上无锚点时只提示、不消耗。持有者可透视锚点；死亡后掉落，任何玩家可拾取使用。
 * 不广播粒子/声音，防止向旁人泄露传送与锚点位置（旧技能静默风格）。
 */
public class RewindShardItem extends Item {
    public RewindShardItem(Settings settings) {
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
        if (!comp.isPlaced()) {
            serverUser.sendMessage(Text.translatable("tip.recaller.no_anchor"), true);
            return TypedActionResult.success(stack); // 不消耗
        }

        // 先取坐标再传送：回放记录用锚点坐标，不能用传送后的玩家坐标
        double ax = comp.getX();
        double ay = comp.getY();
        double az = comp.getZ();
        comp.teleportShardUser(serverUser);
        stack.decrement(1);

        RecallerSounds.playToSelf(serverUser, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "teleport");
        extra.putDouble("x", ax);
        extra.putDouble("y", ay);
        extra.putDouble("z", az);
        GameRecordManager.recordSkillUse(serverUser, Noellesroles.RECALLER_ID, null, extra);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // tooltip 内嵌换行需拆行渲染（同 ShopIconItem）；灰色对齐 wathe 商店物品描述
        for (String line : Text.translatable("item.noellesroles.rewind_shard.tooltip").getString().split("\\R")) {
            tooltip.add(Text.literal(line).withColor(0x808080));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

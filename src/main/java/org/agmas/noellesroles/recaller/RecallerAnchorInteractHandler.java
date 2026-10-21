package org.agmas.noellesroles.recaller;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * 回溯者潜行右键锚点实体 → 回收锚点并返还锚点物品（回收权只属于回溯者职业，与谁放置无关）。
 *
 * 可行性（勿再怀疑）：原版 MinecraftClient.doItemUse 对 ENTITY 准星目标无条件调用
 * interactEntityAtLocation（1.21.1 字节码核实：isSneaking 只是写进包的旗标；
 * “潜行+手持物品跳过交互”只存在于方块的 interactBlock 路径）。服务端
 * ServerPlayNetworkHandler 的 processInteract 同样无潜行门控——与 BreacherRepairHandler 已上线行为一致。
 *
 * 行为说明：
 * - 回溯者潜行右键锚点=回收（即使手里拿着碎片，实体交互事件先于物品 use()，回收优先）；
 * - 非潜行右键锚点=PASS，落到物品 use()：若手持碎片会原地传送（无害、符合宽松版语义）；
 * - 锚点对非持有者隐形但 canHit()=true，准星仍可命中——非回溯者命中后得到 PASS（静默，
 *   不泄露锚点存在）；回溯者凭记忆位置盲回收也可行（回收后碎片透视可再确认）。
 */
public final class RecallerAnchorInteractHandler {
    private RecallerAnchorInteractHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                noellesroles$onUseAnchor(player, world, hand, entity));
    }

    private static ActionResult noellesroles$onUseAnchor(net.minecraft.entity.player.PlayerEntity player,
                                                         net.minecraft.world.World world, Hand hand,
                                                         net.minecraft.entity.Entity entity) {
        if (!(entity instanceof RecallerAnchorEntity)) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS; // 防主/副手双触发
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS; // 客户端不预测，服务端权威
        if (!player.isSneaking()) return ActionResult.PASS; // 非潜行右键→放行走原版链

        RecallerAnchorWorldComponent comp = RecallerAnchorWorldComponent.KEY.get(world);
        if (!comp.isPlaced() || !comp.matchesMarker(entity.getUuid())) return ActionResult.PASS; // 过期实体
        if (!GameFunctions.isPlayerPlayingAndAlive(serverPlayer)
                || SwallowedPlayerComponent.isPlayerSwallowed(serverPlayer)) return ActionResult.FAIL;
        // 仅回溯者职业可回收（定案修改：不再要求是放置者本人）；非回溯者静默无反应，不泄露锚点存在
        if (!dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(world)
                .isRole(serverPlayer, Noellesroles.RECALLER)) return ActionResult.PASS;

        // ---- 回收 ----（先取锚点坐标：retrieveAnchor 会清状态）
        double ax = comp.getX();
        double ay = comp.getY();
        double az = comp.getZ();
        if (!comp.retrieveAnchor()) return ActionResult.PASS;
        ItemStack refund = new ItemStack(ModItems.RECALLER_ANCHOR);
        if (!serverPlayer.giveItemStack(refund)) {
            serverPlayer.dropItem(refund, false); // 背包满则掉落脚边
        }
        RecallerSounds.playToSelf(serverPlayer, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 0.9F, 1.0F);
        serverPlayer.sendMessage(Text.translatable("tip.recaller.retrieved"), true);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "retrieve");
        extra.putDouble("x", ax);
        extra.putDouble("y", ay);
        extra.putDouble("z", az);
        GameRecordManager.recordSkillUse(serverPlayer, Noellesroles.RECALLER_ID, null, extra);
        return ActionResult.SUCCESS;
    }
}

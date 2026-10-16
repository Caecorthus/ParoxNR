package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AdventureUsable;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;

import java.util.List;

/**
 * 工程师维修工具
 * 可修复被撬的门、上锁/解锁门，3分钟冷却
 * 非潜行的修复/解锁通过 DoorInteraction.EVENT 在 Noellesroles.registerEvents() 中处理；
 * 潜行上锁必须走 useOnBlock：潜行手持物品时原版会跳过方块交互，事件不会触发（开锁器堵门同理）
 */
public class RepairToolItem extends Item implements AdventureUsable {
    public static final int COOLDOWN_TICKS = 3600; // 3 minutes

    public enum DoorAction {
        REPAIR, UNLOCK, LOCK
    }

    public RepairToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof SmallDoorBlock)) {
            return ActionResult.PASS;
        }
        BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
        if (!(world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity)) {
            return ActionResult.PASS;
        }

        DoorAction action;
        if (entity.isBlasted()) {
            action = DoorAction.REPAIR;
        } else if (entity.isJammed()) {
            action = DoorAction.UNLOCK;
        } else if (player.isSneaking()) {
            action = DoorAction.LOCK;
        } else {
            return ActionResult.PASS;
        }

        applyToDoor(player, world, lowerPos, entity, action);
        return ActionResult.SUCCESS;
    }

    /**
     * 对门执行一次维修工具操作。
     * 物品交互路径在客户端也会调用（客户端预测），因此状态变更不分端；音效/提示/冷却/记录仅服务端执行。
     */
    public static void applyToDoor(PlayerEntity player, World world, BlockPos lowerPos, DoorBlockEntity entity, DoorAction action) {
        switch (action) {
            case REPAIR -> {
                entity.setBlasted(false);
                if (entity.isOpen()) {
                    entity.toggle(false);
                }
                entity.sync();
                SmallDoorBlockEntity neighbor = getNeighbor(world, lowerPos, entity);
                if (neighbor != null && neighbor.isBlasted()) {
                    neighbor.setBlasted(false);
                    if (neighbor.isOpen()) {
                        neighbor.toggle(false);
                    }
                    neighbor.sync();
                }
            }
            case UNLOCK -> {
                entity.setJammed(0);
                entity.sync();
                SmallDoorBlockEntity neighbor = getNeighbor(world, lowerPos, entity);
                if (neighbor != null && neighbor.isJammed()) {
                    neighbor.setJammed(0);
                    neighbor.sync();
                }
            }
            case LOCK -> {
                entity.setJammed(GameConstants.JAMMED_DOOR_TIME);
                if (entity.isOpen()) {
                    entity.toggle(false);
                }
                entity.sync();
                SmallDoorBlockEntity neighbor = getNeighbor(world, lowerPos, entity);
                if (neighbor != null) {
                    neighbor.setJammed(GameConstants.JAMMED_DOOR_TIME);
                    if (neighbor.isOpen()) {
                        neighbor.toggle(false);
                    }
                    neighbor.sync();
                }
            }
        }

        if (world.isClient) {
            return;
        }
        player.getItemCooldownManager().set(ModItems.REPAIR_TOOL, COOLDOWN_TICKS);
        player.sendMessage(Text.translatable(switch (action) {
            case REPAIR -> "tip.engineer.repaired";
            case UNLOCK -> "tip.engineer.unlocked";
            case LOCK -> "tip.engineer.locked";
        }), true);
        SoundEvent sound = action == DoorAction.REPAIR ? WatheSounds.BLOCK_DOOR_TOGGLE : WatheSounds.ITEM_LOCKPICK_DOOR;
        float pitch = action == DoorAction.LOCK ? 1f : 1.2f;
        world.playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f,
                sound, SoundCategory.BLOCKS, 1f, pitch);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            GameRecordManager.putBlockPos(extra, "pos", lowerPos);
            extra.putString("action", switch (action) {
                case REPAIR -> "repair";
                case UNLOCK -> "unlock";
                case LOCK -> "lock";
            });
            GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.REPAIR_TOOL), null, extra);
        }
    }

    private static SmallDoorBlockEntity getNeighbor(World world, BlockPos lowerPos, DoorBlockEntity entity) {
        if (!(entity instanceof SmallDoorBlockEntity)) {
            return null;
        }
        return SmallDoorBlock.getNeighborDoorEntity(world.getBlockState(lowerPos), world, lowerPos);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : Text.translatable("item.noellesroles.repair_tool.tooltip").getString().split("\\R")) {
            tooltip.add(Text.literal(line).withColor(0x808080));
        }
    }
}

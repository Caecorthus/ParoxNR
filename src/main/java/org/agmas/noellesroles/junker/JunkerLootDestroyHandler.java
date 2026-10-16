package org.agmas.noellesroles.junker;

import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.Scheduler;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 拾荒者搜刮所得物品的「使用生效后销毁」，以及潜行右键空气的主动销毁。
 * 消耗品使用时本身就会消失，这里处理可以重复使用的物品：
 * 门类道具、攻击类、蓄力类、刺杀成功的刀、以及 Wathe 记录的成功使用。
 */
public final class JunkerLootDestroyHandler {
    private static final int DESTROY_DELAY_TICKS = 1;
    private static final double REACH_DISTANCE = 4.5D;
    private static final Map<UUID, Long> lastDestroyRecordTicks = new HashMap<>();

    private JunkerLootDestroyHandler() {}

    public static void register() {
        // 门类道具（万能钥匙/中立万能钥匙/撬锁器/撬棍）：门确实被打开后才销毁
        DoorInteraction.EVENT.register(context -> {
            if (!context.isServerSide()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            ItemStack handItem = context.getHandItem();
            if (!JunkerLootHelper.isDestroyableLoot(handItem)) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            // 只有真的把这件道具用在门上才算一次使用（普通开关门不算）
            boolean doorToolUsed = context.getInteractionType().usesTool()
                    || (JunkerLootHelper.isMasterKey(handItem)
                        && (context.requiresKey() || context.getDoorType() == DoorInteraction.DoorType.TRAIN_DOOR));
            if (!doorToolUsed) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }

            PlayerEntity player = context.getPlayer();
            DoorBlockEntity door = context.getEntity();
            boolean wasOpen = door.isOpen();
            scheduleDestroyAfterDoorOpens(player, handItem.getItem(), door, wasOpen);
            return DoorInteraction.DoorInteractionResult.PASS;
        });

        // 攻击类（球棒、毒针、防暴盾等）：攻击真正生效后销毁
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            ItemStack mainHand = player.getMainHandStack();
            if (!JunkerLootHelper.isDestroyableLoot(mainHand)) {
                return ActionResult.PASS;
            }
            if (!canAttackTakeEffect(serverPlayer, mainHand, entity)) {
                return ActionResult.PASS;
            }
            scheduleDestroy(player, mainHand);
            return ActionResult.PASS;
        });

        // 搜刮所得的刀：刺杀成功（击杀）后销毁，空刀保留
        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            if (killer == null || !GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
                return;
            }
            ItemStack mainHand = killer.getMainHandStack();
            if (JunkerLootHelper.isDestroyableLoot(mainHand)) {
                scheduleDestroy(killer, mainHand);
            }
        });

        // 主动销毁：潜行 + 右键空气
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != Hand.MAIN_HAND) {
                return TypedActionResult.pass(ItemStack.EMPTY);
            }
            ItemStack stack = player.getStackInHand(hand);
            if (!JunkerLootHelper.isSearchedLoot(stack) || !player.isSneaking() || !isAimingAtAir(player)) {
                return TypedActionResult.pass(stack);
            }
            if (!world.isClient) {
                Text name = stack.getName().copy();
                recordDestroy(player, stack, "manual");
                stack.decrement(1);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(Text.translatable("tip.junker.destroyed", name), true);
                }
            }
            return TypedActionResult.success(stack);
        });
    }

    /** 与 Wathe 的攻击判定保持一致，只有会真正生效的攻击才算一次使用（刀另行按刺杀成功处理）。 */
    private static boolean canAttackTakeEffect(ServerPlayerEntity player, ItemStack mainHand, Entity target) {
        if (player.isCreative()) {
            return true;
        }
        if (mainHand.isOf(WatheItems.BAT) && target instanceof ServerPlayerEntity
                && player.getAttackCooldownProgress(0.5F) >= 1.0F) {
            return true;
        }
        // 本模组通过 AllowPlayerPunching 放行了持毒针/防暴盾的出拳
        return target instanceof PlayerEntity
                && (mainHand.isOf(ModItems.POISON_NEEDLE) || mainHand.isOf(ModItems.RIOT_SHIELD));
    }

    /** Wathe 的成功使用路径（刀刺杀、开枪、尸体袋、音符、撬锁器、撬棍、鞭炮、手雷、下毒等）都会走到这里。 */
    public static void onItemUsed(ServerPlayerEntity player, Item item) {
        ItemStack loot = JunkerLootHelper.findDestroyableLootByItem(player, item);
        if (loot != null) {
            scheduleDestroy(player, loot);
        }
    }

    /** 蓄力类物品松手即算一次使用；刀改为刺杀成功后才销毁，这里跳过。 */
    public static void onChargedItemReleased(ServerPlayerEntity player) {
        ItemStack active = player.getActiveItem();
        if (!JunkerLootHelper.isDestroyableLoot(active) || active.isOf(WatheItems.KNIFE)) {
            return;
        }
        scheduleDestroy(player, active);
    }

    private static boolean isAimingAtAir(PlayerEntity player) {
        return player.raycast(REACH_DISTANCE, 0.0F, false).getType() == HitResult.Type.MISS;
    }

    private static void scheduleDestroyAfterDoorOpens(PlayerEntity player, Item item, DoorBlockEntity door, boolean wasOpen) {
        Scheduler.schedule(() -> {
            if (player.isRemoved() || door.isRemoved() || door.isOpen() == wasOpen) {
                return;
            }
            if (player.getMainHandStack().isOf(item)) {
                recordDestroy(player, player.getMainHandStack(), "used");
                JunkerLootHelper.destroy(player, player.getMainHandStack());
            }
        }, DESTROY_DELAY_TICKS);
    }

    private static void scheduleDestroy(@Nullable PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        Scheduler.schedule(() -> {
            if (player.isRemoved()) {
                return;
            }
            if (stack.isEmpty()) {
                // 消耗品在使用过程中就已经消失，同样算作使用后销毁
                recordDestroy(player, stack, "used");
                return;
            }
            if (JunkerLootHelper.isDestroyableLoot(stack)) {
                recordDestroy(player, stack, "used");
                JunkerLootHelper.destroy(player, stack);
            }
        }, DESTROY_DELAY_TICKS);
    }

    /** 回放记录：搜刮所得物品被销毁（manual=主动销毁，used=使用生效后销毁）。 */
    private static void recordDestroy(PlayerEntity player, ItemStack stack, String action) {
        // 被消耗掉的物品数量为0但仍保留物品类型，可以正常记录
        if (!(player instanceof ServerPlayerEntity serverPlayer) || stack.getItem() == Items.AIR) {
            return;
        }
        // 同一件物品可能被多个钩子同时判定为已使用，同一tick内只记录一次
        long now = serverPlayer.getWorld().getTime();
        Long lastRecordTick = lastDestroyRecordTicks.get(serverPlayer.getUuid());
        if (lastRecordTick != null && lastRecordTick == now) {
            return;
        }
        lastDestroyRecordTicks.put(serverPlayer.getUuid(), now);
        GameRecordManager.event("junker_loot_destroyed")
                .actor(serverPlayer)
                .put("action", action)
                .put("item", Registries.ITEM.getId(stack.getItem()).toString())
                .put("item_name", Text.Serialization.toJsonString(stack.getName(), serverPlayer.getRegistryManager()))
                .record();
    }
}

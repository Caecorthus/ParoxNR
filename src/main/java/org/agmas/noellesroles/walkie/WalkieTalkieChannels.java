package org.agmas.noellesroles.walkie;

import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 对讲机频道规则：频道范围 0~33；杀手阵营开局共享同一个随机非 0 频道。
 */
public final class WalkieTalkieChannels {
    /** 频道范围 0~33，共 34 个 */
    public static final int MAX_CHANNEL = 33;
    private static final int KILLER_CHANNEL_MIN = 1;

    private static int killerChannel = KILLER_CHANNEL_MIN;

    private WalkieTalkieChannels() {
    }

    public static void rollForNewRound() {
        killerChannel = KILLER_CHANNEL_MIN
                + ThreadLocalRandom.current().nextInt(MAX_CHANNEL - KILLER_CHANNEL_MIN + 1);
    }

    /**
     * 延迟一 tick 后再套用频道：Wathe 也挂在 RoleAssigned 上发放对讲机，而两个监听器的先后顺序不保证
     * （实测本模组的监听器先跑），立即套用会扑空、对讲机停在默认频道 0。
     */
    public static void scheduleKillerChannel(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        UUID uuid = serverPlayer.getUuid();
        Scheduler.schedule(() -> {
            if (serverPlayer.getServer() == null) {
                return;
            }
            if (serverPlayer.getServer().getPlayerManager().getPlayer(uuid) instanceof ServerPlayerEntity current) {
                applyKillerChannel(current);
            }
        }, 1);
    }

    private static void applyKillerChannel(PlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            setChannel(stack);
        }
        for (ItemStack stack : player.getInventory().offHand) {
            setChannel(stack);
        }
    }

    private static void setChannel(ItemStack stack) {
        if (stack.isOf(WatheItems.WALKIE_TALKIE)) {
            stack.set(WatheDataComponentTypes.WALKIE_TALKIE, new WalkieTalkieComponent(killerChannel));
        }
    }
}

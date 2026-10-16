package org.agmas.noellesroles.client.taskpoint;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.item.KeyItem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 任务 → TaskPointProvider 注册表。默认注册 EAT/DRINK；
 * SLEEP 对应的房间门由常驻门通道处理，OUTSIDE 暂无目标点。
 * 新任务点通过 {@link #register(TaskPointProvider)} 扩展。
 */
public final class TaskPointProviders {

    private static final Map<PlayerMoodComponent.Task, TaskPointProvider> PROVIDERS = new EnumMap<>(PlayerMoodComponent.Task.class);

    private TaskPointProviders() {
    }

    public static void registerDefaults() {
        register(new TaskPointProvider() {
            @Override
            public PlayerMoodComponent.Task task() {
                return PlayerMoodComponent.Task.EAT;
            }

            @Override
            public List<BlockPos> targets(ClientPlayerEntity player, TaskPointCache cache) {
                return cache.getFoodPlatters();
            }
        });
        register(new TaskPointProvider() {
            @Override
            public PlayerMoodComponent.Task task() {
                return PlayerMoodComponent.Task.DRINK;
            }

            @Override
            public List<BlockPos> targets(ClientPlayerEntity player, TaskPointCache cache) {
                return cache.getDrinkTrays();
            }
        });
    }

    public static void register(TaskPointProvider provider) {
        PROVIDERS.put(provider.task(), provider);
    }

    public static TaskPointProvider get(PlayerMoodComponent.Task task) {
        return PROVIDERS.get(task);
    }

    /**
     * 主手是否持有房间钥匙；是则返回钥匙 lore 第一行（即门 keyName 匹配串）。
     */
    public static String getHeldRoomKeyName(ClientPlayerEntity player) {
        var stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof KeyItem)) return null;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;
        String line = lore.lines().getFirst().getString();
        return line.isEmpty() ? null : line;
    }
}

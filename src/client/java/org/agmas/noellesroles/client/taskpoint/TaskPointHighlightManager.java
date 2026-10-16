package org.agmas.noellesroles.client.taskpoint;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务点高亮状态机：B 键开/关切换。开启后同时高亮玩家当前所有任务的目标点，
 * 只读取客户端同步的 PlayerMoodComponent.tasks（当前未完成任务），
 * 定期扫描已加载区块得到目标点快照。
 * 房间门高亮为常驻通道：主手持对应房间钥匙即描边，不受开关影响。
 */
public final class TaskPointHighlightManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("noellesroles-taskpoint");
    private static final int SCAN_INTERVAL_TICKS = 20;

    private static String lastCacheLogSignature = "";
    private static boolean defaultsRegistered = false;
    private static boolean enabled = false;
    private static TaskPointCache cache;
    private static int scanCooldown = 0;

    private TaskPointHighlightManager() {
    }

    public static void register() {
        if (!defaultsRegistered) {
            TaskPointProviders.registerDefaults();
            defaultsRegistered = true;
        }
        ClientTickEvents.END_CLIENT_TICK.register(TaskPointHighlightManager::tick);
    }

    public static void onKeyPressed(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !WatheClient.isPlayerPlayingAndAlive()) return;

        if (enabled) {
            deactivate(player);
            return;
        }
        enabled = true;
        scanCooldown = 1;
        cache = TaskPointCache.scan(client);
        logCacheChanges();
        sendActionBar(player, "tip.noellesroles.taskpoint.enabled",
                NoellesrolesClient.taskPointHighlightBind.getBoundKeyLocalizedText());
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || !WatheClient.isPlayerPlayingAndAlive()) {
            reset();
            return;
        }
        // 吃/喝通道开启，或门常驻通道持钥匙时，都需要目标点快照
        if (!enabled && TaskPointProviders.getHeldRoomKeyName(player) == null) {
            return;
        }
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL_TICKS;
            cache = TaskPointCache.scan(client);
            logCacheChanges();
        }
    }

    private static void deactivate(ClientPlayerEntity player) {
        reset();
        sendActionBar(player, "tip.noellesroles.taskpoint.off");
    }

    public static void reset() {
        lastCacheLogSignature = "";
        enabled = false;
        cache = null;
        scanCooldown = 0;
    }

    private static void sendActionBar(ClientPlayerEntity player, String key, Object... args) {
        player.sendMessage(Text.translatable(key, args), true);
    }

    /**
     * 扫描结果发生变化时打印目标点数量与坐标样例，便于排查高亮位置是否正确。
     */
    private static void logCacheChanges() {
        if (cache == null) return;
        String signature = cache.getFoodPlatters().size() + "/"
                + cache.getDrinkTrays().size() + "/"
                + cache.getDoors().size();
        if (signature.equals(lastCacheLogSignature)) return;
        lastCacheLogSignature = signature;
        LOGGER.info("taskpoint scan food={} drink={} doors={} sampleFood={} sampleDrink={}",
                cache.getFoodPlatters().size(), cache.getDrinkTrays().size(), cache.getDoors().size(),
                samplePos(cache.getFoodPlatters()), samplePos(cache.getDrinkTrays()));
    }

    private static String samplePos(List<BlockPos> positions) {
        if (positions.isEmpty()) return "[]";
        List<String> samples = new ArrayList<>();
        for (int i = 0; i < Math.min(positions.size(), 3); i++) {
            BlockPos p = positions.get(i);
            samples.add("[" + p.getX() + "," + p.getY() + "," + p.getZ() + "]");
        }
        return String.join(" ", samples);
    }

    // ---- 渲染读取接口 ----

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 开关开启时，玩家当前所有任务目标点的并集。
     * 每个任务的目标各自去重（同 X,Z 列取 Y 最高），避免不同类型任务点恰好同列时互相覆盖。
     */
    public static List<BlockPos> resolveRenderTargets(ClientPlayerEntity player) {
        if (!enabled || cache == null) return List.of();
        List<BlockPos> targets = new ArrayList<>();
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        for (PlayerMoodComponent.Task task : mood.tasks.keySet()) {
            TaskPointProvider provider = TaskPointProviders.get(task);
            if (provider != null) {
                targets.addAll(keepHighestPerColumn(provider.targets(player, cache)));
            }
        }
        return targets;
    }

    /**
     * 常驻房间门高亮目标：主手持房间钥匙时，返回与该钥匙匹配的门。
     * 不依赖任务与开关，任何时刻持钥匙即返回（供渲染器全时段描边）。
     */
    public static List<BlockPos> resolveDoorTargets(ClientPlayerEntity player) {
        if (cache == null) return List.of();
        String heldKeyName = TaskPointProviders.getHeldRoomKeyName(player);
        if (heldKeyName == null) return List.of();
        List<BlockPos> result = new ArrayList<>();
        for (TaskPointCache.DoorEntry door : cache.getDoors()) {
            if (heldKeyName.equals(door.keyName())) {
                result.add(door.lowerPos());
            }
        }
        return keepHighestPerColumn(result);
    }

    /**
     * 地图下方存在用于复制的原档地图，其点位与地图内点位 X,Z 相同、Y 更低，只保留 Y 最高的那个。
     */
    private static List<BlockPos> keepHighestPerColumn(List<BlockPos> targets) {
        if (targets.size() <= 1) return targets;
        Map<Long, BlockPos> highest = new HashMap<>();
        for (BlockPos pos : targets) {
            long column = ((long) pos.getX() << 32) | (pos.getZ() & 0xFFFFFFFFL);
            BlockPos existing = highest.get(column);
            if (existing == null || pos.getY() > existing.getY()) {
                highest.put(column, pos);
            }
        }
        return new ArrayList<>(highest.values());
    }
}

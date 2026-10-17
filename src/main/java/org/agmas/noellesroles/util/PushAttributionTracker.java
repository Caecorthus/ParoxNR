package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 推人坠车归属：记录每名玩家最后一次被其他玩家打中（推开）的情况。
 * <p>
 * Wathe 判定坠车时用原版 lastAttacker 作为凶手，而它只在被打后保留 100 tick（5 秒）。
 * 被推出列车后若 5 秒内还没跌破游玩区域下界（落差大、低重力、先落在车外再滑落等），
 * 凶手就会丢失，坠车被当成"自行坠落"，犯罪推理学家也随之把凶手记成死者本人。
 * <p>
 * 这里把归属窗口延长为：被打后一直处于空中则持续有效（上限 30 秒）；
 * 落地后再保留 5 秒；从未离地则与原版一致，被打后 5 秒内有效。
 */
public final class PushAttributionTracker {
    /** 与原版 lastAttacker 的保留时长一致。 */
    private static final int GRACE_TICKS = 100;
    /** 单次推人归属的最长有效期，防止异常情况下记录长期残留。 */
    private static final int MAX_TRACK_TICKS = GameConstants.getInTicks(0, 30);

    private static final Map<UUID, Push> pushes = new HashMap<>();

    private PushAttributionTracker() {}

    /** 玩家被其他玩家成功打中（原版 setAttacker）时调用，后一次覆盖前一次。 */
    public static void recordHit(ServerPlayerEntity victim, ServerPlayerEntity pusher) {
        if (victim == pusher || !GameFunctions.isPlayerPlayingAndAlive(victim)) {
            return;
        }
        pushes.put(victim.getUuid(), new Push(pusher.getUuid(), victim.getServer().getTicks()));
    }

    /** 每 tick 跟踪被推者是否离地/落地，并清理失效记录。 */
    public static void tick(MinecraftServer server) {
        if (pushes.isEmpty()) {
            return;
        }
        int now = server.getTicks();
        Iterator<Map.Entry<UUID, Push>> iterator = pushes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Push> entry = iterator.next();
            Push push = entry.getValue();
            ServerPlayerEntity victim = server.getPlayerManager().getPlayer(entry.getKey());
            if (victim == null || !GameFunctions.isPlayerPlayingAndAlive(victim) || !push.isValid(now)) {
                iterator.remove();
                continue;
            }
            if (push.landedTick >= 0) {
                continue;
            }
            // 只认被打后的第一段腾空：落地后起跳不会再续期
            if (!victim.isOnGround()) {
                push.airborne = true;
            } else if (push.airborne) {
                push.landedTick = now;
            }
        }
    }

    /** 返回仍对该玩家坠车负责的推人者；没有或已失效时返回 null。 */
    @Nullable
    public static ServerPlayerEntity getPusher(ServerPlayerEntity victim) {
        Push push = pushes.get(victim.getUuid());
        MinecraftServer server = victim.getServer();
        if (push == null || server == null || !push.isValid(server.getTicks())) {
            return null;
        }
        return server.getPlayerManager().getPlayer(push.pusher);
    }

    public static void reset() {
        pushes.clear();
    }

    private static final class Push {
        private final UUID pusher;
        private final int hitTick;
        private boolean airborne;
        private int landedTick = -1;

        private Push(UUID pusher, int hitTick) {
            this.pusher = pusher;
            this.hitTick = hitTick;
        }

        private boolean isValid(int now) {
            if (now - this.hitTick > MAX_TRACK_TICKS) {
                return false;
            }
            if (this.landedTick >= 0) {
                return now - this.landedTick <= GRACE_TICKS;
            }
            return this.airborne || now - this.hitTick <= GRACE_TICKS;
        }
    }
}

package org.agmas.noellesroles.phonism;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 声音收集器的静态登记表。
 * 语音/世界声音事件频率很高，中继查询只查这张表，不逐包遍历玩家：
 * - INSTALLATIONS：目标 → 音效师（安装关系，防双音效师叠装；监听关闭时依然保留，等待回收）
 * - ACTIVE_TAPS：目标 → 音效师（仅 relayActive 时注册，中继热路径查这张表）
 * 失效（音效师死亡/掉线/被吞噬、目标死亡/掉线、游戏结束、回收）在查询入口惰性校验摘除，
 * 组件 serverTick 也会主动校验；目标死亡后收集器状态留在尸体上等待回收（collectorTarget 不清，仅中继停）。
 */
public final class SoundCollectorHelper {
    private static final Map<UUID /*收集目标*/, UUID /*音效师*/> INSTALLATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID /*收集目标*/, ServerPlayerEntity /*音效师*/> ACTIVE_TAPS = new ConcurrentHashMap<>();

    private SoundCollectorHelper() {}

    /** 热路径守卫：没有任何活跃中继时，所有中继逻辑立即跳过。 */
    public static boolean hasActiveTaps() {
        return !ACTIVE_TAPS.isEmpty();
    }

    public static void install(UUID target, UUID phonism) {
        INSTALLATIONS.put(target, phonism);
    }

    /** 回收/重置：安装关系与中继登记一并移除（调用方即该安装的属主）。 */
    public static void uninstall(UUID target) {
        INSTALLATIONS.remove(target);
        ACTIVE_TAPS.remove(target);
    }

    public static void registerTap(UUID target, ServerPlayerEntity phonism) {
        ACTIVE_TAPS.put(target, phonism);
    }

    public static void unregisterTap(UUID target) {
        ACTIVE_TAPS.remove(target);
    }

    /** 仅当登记表项确属该音效师时摘除（惰性校验用，避免误摘接管者的新表项）。 */
    public static void unregisterTap(UUID target, PlayerEntity phonism) {
        ACTIVE_TAPS.remove(target, phonism);
    }

    /**
     * 目标身上是否已有别人的收集器（防双音效师叠装）。
     * 属主已掉线或其组件状态早已不指向该目标时，视为过期登记：清掉并放行。
     */
    public static boolean isTargetTaken(World world, UUID target, UUID self) {
        UUID owner = INSTALLATIONS.get(target);
        if (owner == null || owner.equals(self)) {
            return false;
        }
        PlayerEntity ownerEntity = world.getPlayerByUuid(owner);
        if (ownerEntity == null) {
            INSTALLATIONS.remove(target, owner);
            return false;
        }
        if (target.equals(PhonismPlayerComponent.KEY.get(ownerEntity).getCollectorTarget())) {
            return true;
        }
        INSTALLATIONS.remove(target, owner);
        return false;
    }

    /**
     * 中继查询入口：命中且仍有效才返回音效师；失效则惰性摘除表项并把对方的中继标记翻停。
     */
    @Nullable
    public static ServerPlayerEntity getPhonismForTarget(UUID targetUuid) {
        ServerPlayerEntity phonism = ACTIVE_TAPS.get(targetUuid);
        if (phonism == null) {
            return null;
        }
        if (isTapValid(phonism, targetUuid)) {
            return phonism;
        }
        ACTIVE_TAPS.remove(targetUuid, phonism);
        if (!phonism.isRemoved()) {
            // 本查询可能来自非主线程（sendPacket 允许异步调用），组件状态翻停排队回主线程
            phonism.getServer().execute(() ->
                    PhonismPlayerComponent.KEY.get(phonism).stopRelayIfTargets(targetUuid));
        }
        return null;
    }

    /** 中继是否仍然有效：音效师与目标都必须在局、存活、未被吞噬，且音效师仍是谎言音效师。 */
    public static boolean isTapValid(PlayerEntity phonism, UUID targetUuid) {
        if (phonism.isRemoved() || phonism.getWorld() == null) {
            return false;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(phonism)) {
            return false;
        }
        if (SwallowedPlayerComponent.isPlayerSwallowed(phonism)) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(phonism.getWorld());
        if (!gameWorld.isRole(phonism, Noellesroles.PHONISM)) {
            return false;
        }
        PlayerEntity target = phonism.getWorld().getPlayerByUuid(targetUuid);
        if (target == null || target.isRemoved()) {
            return false;
        }
        return GameFunctions.isPlayerPlayingAndAlive(target);
    }
}

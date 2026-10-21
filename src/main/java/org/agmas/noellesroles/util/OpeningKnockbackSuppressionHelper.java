package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.packet.OpeningKnockbackSuppressS2CPacket;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * 开局刀冷却期间的攻击击退压制：
 * 禁用 wathe 的两条击退来源 —— LivingEntityMixin#wathe$addKnockbackWithKnife 的
 * 持刀攻击击退属性（在 takeKnockback 落点屏蔽其效果）与 AllowPlayerPunching 的出拳放行，
 * 即近战命中只保留受伤动画，不会把受害者击飞。
 * <p>
 * 压制期间若玩家主手持有刀且刀不在冷却（如开局内 scavenger 买刀清除了冷却），
 * 则立即恢复该玩家的击退能力。
 * <p>
 * 压制按玩家各自的开局刀冷却存续：tick 中发现冷却已结束就将玩家移出记录，
 * 因此双角色选择期间冷却被冻结时压制也随之保留。
 */
public final class OpeningKnockbackSuppressionHelper {
    /** 开局刀冷却时长（45 秒），与 Wathe 原版开局冷却一致。 */
    public static final int OPENING_KNIFE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);

    private static final Set<UUID> openingWindowPlayers = new HashSet<>();

    /**
     * 客户端本地镜像：仅"本人是否处于窗口"一位。
     * attack()/出拳的本地预测只在攻击者自己的客户端执行，服务端把窗口开/关
     * 同步过来即可让本地音效判定与权威判定一致（远程玩家实体在客户端命中时
     * damage() 会直接返回 false，不会走到这些判定）。
     */
    private static boolean localSuppressionActive;

    private OpeningKnockbackSuppressionHelper() {}

    /** 客户端接收 OpeningKnockbackSuppressS2CPacket 时写入本人窗口镜像。 */
    public static void setLocalSuppressionActive(boolean active) {
        localSuppressionActive = active;
    }

    /** 对局开始时调用：玩家进入开局击退压制窗口。 */
    public static void startOpeningWindow(ServerPlayerEntity player) {
        openingWindowPlayers.add(player.getUuid());
        ServerPlayNetworking.send(player, new OpeningKnockbackSuppressS2CPacket(true));
    }

    /** 每 tick 检查开局冷却是否结束；结束后玩家永久离开压制窗口。 */
    public static void tick(MinecraftServer server) {
        if (openingWindowPlayers.isEmpty()) {
            return;
        }
        Iterator<UUID> iterator = openingWindowPlayers.iterator();
        while (iterator.hasNext()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (!player.getItemCooldownManager().isCoolingDown(WatheItems.KNIFE)) {
                ServerPlayNetworking.send(player, new OpeningKnockbackSuppressS2CPacket(false));
                iterator.remove();
            }
        }
    }

    /** 攻击者的近战击退是否应被压制；持刀且刀不在冷却时立即恢复击退。 */
    public static boolean isSuppressed(PlayerEntity player) {
        boolean inWindow = player.getWorld().isClient
                ? localSuppressionActive
                : openingWindowPlayers.contains(player.getUuid());
        if (!inWindow) {
            return false;
        }
        return !(player.getMainHandStack().isOf(WatheItems.KNIFE)
                && !player.getItemCooldownManager().isCoolingDown(WatheItems.KNIFE));
    }

    /** 对局结束/新对局开始时调用：清空压制记录并同步关闭各在线玩家的客户端镜像。 */
    public static void reset(MinecraftServer server) {
        for (UUID uuid : openingWindowPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                ServerPlayNetworking.send(player, new OpeningKnockbackSuppressS2CPacket(false));
            }
        }
        openingWindowPlayers.clear();
    }
}

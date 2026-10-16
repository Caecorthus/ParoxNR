package org.agmas.noellesroles.dreameater;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.RoleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 「永眠」（噬梦者三击入梦后睡死）击杀的队友奖励。
 * <p>
 * 凶手本人的击杀奖励由 wathe 原版发放（凶手 +100，见 {@code GameFunctions.killPlayer}），
 * 本模组已不再对永眠击杀做 -50 修正，因此凶手总计拿满 100。
 * 这里只负责**额外的**50 金币：由除凶手以外的存活杀手队友平分
 * （整除 + 余数补给前面几位，与 {@code PassengerDeathCompensationHelper} 同口径），
 * 并给每位收钱的队友发一条 actionbar 提示。
 * <p>
 * 只在死亡真正结算后调用（挂在 {@code KillPlayer.AFTER} 上），
 * 所以被圣徒/保镖等 BEFORE 逻辑拦下的「未遂」死亡不会发钱。
 */
public final class DreamEaterKillRewardHelper {

    private DreamEaterKillRewardHelper() {}

    /** 队友平分的总额；凶手本人不参与分配（他拿的是原版的 100）。 */
    public static final int TEAMMATE_REWARD_TOTAL = 50;

    private static final String MESSAGE_KEY = "tip.dream_eater.eternal_sleep_shared";

    /**
     * @param victim 死于永眠的玩家（仅用于取世界与记录凶手）
     * @param killer 凶手（可能为 null：噬梦者已离线/被吞/角色被顶掉）
     */
    public static void payout(PlayerEntity victim, PlayerEntity killer) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);

        List<ServerPlayerEntity> teammates = new ArrayList<>();
        // 遍历全世界的玩家再按「实际杀手角色」过滤，而不是用 getAllKillerTeamPlayers()：
        // 后者包含中立阵营（秃鹫/渡鸦等），与「其他杀手队友」的口径不符
        // （CommanderHelper.isPayoutRecipient 用的也是同一口径）。
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
            if (player.isSpectator()) continue;
            if (killer != null && player.getUuid().equals(killer.getUuid())) continue;
            if (SwallowedPlayerComponent.isPlayerSwallowed(player)) continue;
            Role role = gameWorld.getRole(player);
            if (!RoleUtils.isActualKillerRole(role)) continue;
            teammates.add(player);
        }
        if (teammates.isEmpty()) {
            return;
        }

        Text notice = Text.translatable(MESSAGE_KEY);
        int each = TEAMMATE_REWARD_TOTAL / teammates.size();
        int remainder = TEAMMATE_REWARD_TOTAL % teammates.size();
        for (int i = 0; i < teammates.size(); i++) {
            ServerPlayerEntity teammate = teammates.get(i);
            int amount = each + (i < remainder ? 1 : 0);
            if (amount > 0) {
                PlayerShopComponent.KEY.get(teammate).addToBalance(amount);
            }
            teammate.sendMessage(notice, true);
        }
    }
}

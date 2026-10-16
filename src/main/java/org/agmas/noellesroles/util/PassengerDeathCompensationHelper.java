package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 乘客白白阵亡补偿：乘客阵营玩家若死于杀手/中立阵营以外之手（内耗击杀、开枪误杀乘客的小脑死亡、
 * 坠落列车、退出游戏、理智崩溃等），每个因此阵亡的乘客都会让存活的杀手阵营玩家平分 50 金币。
 * <p>
 * 白骑士走专属规则：被中立击杀时每位存活杀手 +50；小脑死亡/内耗时每位存活杀手 +150（替换通用的"共50平分"），
 * 并让所有存活乘客听到三声钟声。
 */
public final class PassengerDeathCompensationHelper {
    private PassengerDeathCompensationHelper() {}

    private static final int REWARD_TOTAL = 50;
    private static final int WHITE_KNIGHT_NEUTRAL_KILL_REWARD = 50;
    private static final int WHITE_KNIGHT_FALLEN_REWARD = 150;
    private static final int BELL_TOLL_COUNT = 3;
    private static final int BELL_TOLL_INTERVAL_TICKS = 20;
    /** 白骑士的杀手奖励与提示延迟结算，让乘客阵营先一步知道死讯。 */
    private static final int KILLER_PAYOUT_DELAY_TICKS = 20 * 10;

    public static void handleAfterKill(PlayerEntity victim, PlayerEntity killer, Identifier deathReason, GameWorldComponent gameComponent) {
        Role victimRole = gameComponent.getRole(victim);
        if (victimRole == null) {
            return;
        }
        if (victimRole.equals(Noellesroles.WHITE_KNIGHT)) {
            handleWhiteKnightDeath(victim, killer, deathReason, gameComponent);
            return;
        }
        if (!victimRole.isInnocent()) {
            return; // 只统计乘客阵营死者
        }
        if (killer != null) {
            Role killerRole = gameComponent.getRole(killer);
            // 杀手/中立阵营造成的死亡走各自的击杀奖励，不触发补偿
            if (killerRole != null && !killerRole.isInnocent()) {
                return;
            }
        } else if (deathReason == GameConstants.DeathReasons.POISON) {
            // 毒杀必然出自下毒玩家（杀手/中立）或猎人捕兽夹，并非白白阵亡
            return;
        }
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        // 被饕餮吞食后死亡属于中立阵营所为
        if (SwallowedPlayerComponent.KEY.get(victim).isSwallowed()) {
            return;
        }

        List<ServerPlayerEntity> recipients = collectAliveKillers(world, gameComponent);
        if (recipients.isEmpty()) {
            return;
        }

        int each = REWARD_TOTAL / recipients.size();
        int remainder = REWARD_TOTAL % recipients.size();
        for (int i = 0; i < recipients.size(); i++) {
            ServerPlayerEntity recipient = recipients.get(i);
            PlayerShopComponent.KEY.get(recipient).addToBalance(each + (i < remainder ? 1 : 0));
            recipient.sendMessage(Text.translatable("tip.passenger_loss.reward"), true);
        }
    }

    /** 白骑士专属死亡规则。 */
    private static void handleWhiteKnightDeath(PlayerEntity victim, PlayerEntity killer, Identifier deathReason, GameWorldComponent gameComponent) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        // 无直接凶手时，用下毒者参与阵营判定
        PlayerEntity effectiveKiller = killer;
        boolean poisonDeath = deathReason == GameConstants.DeathReasons.POISON;
        if (effectiveKiller == null && poisonDeath) {
            UUID poisonerUuid = PlayerPoisonComponent.KEY.get(victim).poisoner;
            if (poisonerUuid != null) {
                effectiveKiller = victim.getWorld().getPlayerByUuid(poisonerUuid);
            }
        }

        Role killerRole = effectiveKiller == null ? null : gameComponent.getRole(effectiveKiller);
        if (killerRole != null) {
            if (!killerRole.isInnocent()) {
                if (RoleUtils.isActualKillerRole(killerRole)) {
                    // 杀手阵营的击杀（含毒杀等间接方式）：扣回一半赏金，只留 50
                    PlayerShopComponent.KEY.get(effectiveKiller)
                            .addToBalance(-(GameConstants.MONEY_PER_KILL / 2));
                    return;
                }
                // 被中立阵营击杀：每位存活杀手 +50（延迟结算）
                scheduleKillerPayout(world, gameComponent, WHITE_KNIGHT_NEUTRAL_KILL_REWARD,
                        "tip.white_knight.killed_by_neutral");
                return;
            }
        } else if (poisonDeath) {
            // 无凶手的毒杀属于杀手/中立所为，不是白白阵亡
            return;
        }

        if (SwallowedPlayerComponent.KEY.get(victim).isSwallowed()) {
            return; // 被饕餮吞食属于中立阵营所为
        }

        // 小脑死亡 / 内耗：钟声立刻响起，每位存活杀手 +150 延迟结算
        playFuneralBells(world, gameComponent);
        scheduleKillerPayout(world, gameComponent, WHITE_KNIGHT_FALLEN_REWARD, "tip.white_knight.fallen");
    }

    private static List<ServerPlayerEntity> collectAliveKillers(ServerWorld world, GameWorldComponent gameComponent) {
        List<ServerPlayerEntity> recipients = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
            if (!RoleUtils.isActualKillerRole(gameComponent.getRole(player))) continue;
            if (SwallowedPlayerComponent.KEY.get(player).isSwallowed()) continue;
            recipients.add(player);
        }
        return recipients;
    }

    /** 白骑士的杀手奖励与提示延迟10秒结算，确保乘客阵营先得知死讯。 */
    private static void scheduleKillerPayout(ServerWorld world, GameWorldComponent gameComponent, int amount, String messageKey) {
        Scheduler.schedule(() -> {
            for (ServerPlayerEntity recipient : collectAliveKillers(world, gameComponent)) {
                PlayerShopComponent.KEY.get(recipient).addToBalance(amount);
                recipient.sendMessage(Text.translatable(messageKey, amount), true);
            }
        }, KILLER_PAYOUT_DELAY_TICKS);
    }

    /** 白骑士陨落时，让所有旁观玩家与存活乘客阵营玩家立刻听到三声原版钟声。 */
    private static void playFuneralBells(ServerWorld world, GameWorldComponent gameComponent) {
        for (int toll = 0; toll < BELL_TOLL_COUNT; toll++) {
            Scheduler.schedule(() -> {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!isBellAudience(player, gameComponent)) continue;
                    player.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 1.0F, 1.0F);
                }
            }, toll * BELL_TOLL_INTERVAL_TICKS + 1);
        }
    }

    /** 钟声听众：所有旁观玩家 + 存活的乘客阵营玩家。 */
    private static boolean isBellAudience(ServerPlayerEntity player, GameWorldComponent gameComponent) {
        if (player.isSpectator()) {
            return true;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        Role role = gameComponent.getRole(player);
        return role != null && role.isInnocent();
    }
}

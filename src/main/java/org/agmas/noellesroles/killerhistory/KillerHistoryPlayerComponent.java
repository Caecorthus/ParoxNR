package org.agmas.noellesroles.killerhistory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

/**
 * 记录玩家最近对局的杀手阵营历史，用于动态调整抽到杀手的权重。
 * killerHistoryMask 低 6 位为位掩码，bit0 表示最近一局是否杀手阵营。
 */
public class KillerHistoryPlayerComponent implements Component {
    public static final ComponentKey<KillerHistoryPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "killer_history"),
            KillerHistoryPlayerComponent.class
    );

    private int consecutiveNonKillerGames = 0;
    private int killerHistoryMask = 0;

    public KillerHistoryPlayerComponent(PlayerEntity player) {
    }

    /** 对局结束时调用：记录本局是否处于杀手阵营 */
    public void recordGame(boolean wasKillerTeam) {
        killerHistoryMask = ((killerHistoryMask << 1) | (wasKillerTeam ? 1 : 0))
                & ((1 << KillerHistoryCalculator.HISTORY_WINDOW) - 1);
        consecutiveNonKillerGames = wasKillerTeam ? 0 : consecutiveNonKillerGames + 1;
    }

    /** 调试命令用：直接设置"没玩到杀手的局数"，同时清空近 6 场记录 */
    public void setConsecutiveNonKillerGames(int streak) {
        this.consecutiveNonKillerGames = Math.max(0, streak);
        this.killerHistoryMask = 0;
    }

    public int getConsecutiveNonKillerGames() {
        return consecutiveNonKillerGames;
    }

    public int getKillerGamesLast6() {
        return Integer.bitCount(killerHistoryMask);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("consecutiveNonKillerGames", consecutiveNonKillerGames);
        tag.putInt("killerHistoryMask", killerHistoryMask);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.consecutiveNonKillerGames = tag.contains("consecutiveNonKillerGames") ? tag.getInt("consecutiveNonKillerGames") : 0;
        this.killerHistoryMask = tag.contains("killerHistoryMask") ? tag.getInt("killerHistoryMask") : 0;
    }
}

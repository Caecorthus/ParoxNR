package org.agmas.noellesroles.morphling;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 变形者「万千变化」状态：施法者侧的光环窗口 + 观看者侧的皮肤乱序门闸。
 * 组件只同步给本人；观看者的门闸由范围内施法者每 tick 续期，10 tick 不续期即自动失效，
 * 因此施法者死亡/掉线也不会留下残留。
 */
public class MorphlingScramblePlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<MorphlingScramblePlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "morphling_scramble"), MorphlingScramblePlayerComponent.class);

    public static final int MYRIAD_DURATION_TICKS = GameConstants.getInTicks(0, 15);
    private static final double MYRIAD_RADIUS_SQUARED = 15.0D * 15.0D;
    private static final int SCRAMBLE_REFRESH_TICKS = 10;

    private final PlayerEntity player;
    private int myriadTicks = 0;
    private int scrambledTicks = 0;

    public MorphlingScramblePlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.myriadTicks = 0;
        this.scrambledTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void startMyriad() {
        this.myriadTicks = MYRIAD_DURATION_TICKS;
        this.sync();
    }

    public boolean isMyriadActive() {
        return this.myriadTicks > 0;
    }

    public boolean isScrambled() {
        return this.scrambledTicks > 0;
    }

    @Override
    public void serverTick() {
        if (this.myriadTicks > 0) {
            if (!GameFunctions.isPlayerPlayingAndAlive(this.player)) {
                this.myriadTicks = 0;
                this.sync();
            } else {
                --this.myriadTicks;
                refreshNearbyViewers();
            }
        }

        if (this.scrambledTicks > 0 && --this.scrambledTicks == 0) {
            this.sync();
        }
    }

    private void refreshNearbyViewers() {
        World world = this.player.getWorld();
        if (world.isClient) {
            return;
        }
        for (PlayerEntity other : world.getPlayers()) {
            if (this.player.squaredDistanceTo(other) > MYRIAD_RADIUS_SQUARED) {
                continue;
            }
            KEY.get(other).refreshScrambled();
        }
    }

    private void refreshScrambled() {
        boolean wasActive = this.scrambledTicks > 0;
        this.scrambledTicks = Math.max(this.scrambledTicks, SCRAMBLE_REFRESH_TICKS);
        if (!wasActive) {
            this.sync();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("myriadTicks", this.myriadTicks);
        tag.putInt("scrambledTicks", this.scrambledTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.myriadTicks = tag.contains("myriadTicks") ? tag.getInt("myriadTicks") : 0;
        this.scrambledTicks = tag.contains("scrambledTicks") ? tag.getInt("scrambledTicks") : 0;
    }
}

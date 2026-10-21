package org.agmas.noellesroles.mengpo;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 喝下孟婆汤后的心跳期：60 秒后由 {@link MengPoHelper#resolveSoup} 决定是否转职。
 * 只同步给本人；心跳声与视野跳动只在本人客户端出现，期间死亡则作废。
 * 与 Wathe 中毒组件完全独立（不会致死，毒师/毒理学家也看不到）。
 */
public class MengPoSoupPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<MengPoSoupPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "mengpo_soup"), MengPoSoupPlayerComponent.class);

    public static final int SOUP_TICKS = GameConstants.getInTicks(1, 0);
    // 心跳间隔从 1.5 秒逐渐加快到 0.4 秒
    private static final int SLOWEST_BEAT_INTERVAL = 30;
    private static final int FASTEST_BEAT_INTERVAL = 8;

    private final PlayerEntity player;
    private int soupTicks = 0;
    private int beatCooldown = 0;
    /** 客户端最近一次心跳的时间，供视野跳动 mixin 计算脉动曲线。 */
    public long clientLastBeatMs = 0L;

    public MengPoSoupPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.soupTicks = 0;
        this.beatCooldown = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void start() {
        this.soupTicks = SOUP_TICKS;
        this.sync();
    }

    public boolean isActive() {
        return this.soupTicks > 0;
    }

    public static boolean isActive(PlayerEntity player) {
        return KEY.get(player).isActive();
    }

    @Override
    public void serverTick() {
        if (this.soupTicks <= 0) {
            return;
        }
        if (!(this.player instanceof ServerPlayerEntity serverPlayer) || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            this.reset();
            return;
        }
        this.soupTicks--;
        if (this.soupTicks <= 0) {
            this.sync();
            MengPoHelper.resolveSoup(serverPlayer);
            return;
        }
        if (this.soupTicks % 20 == 0) {
            this.sync();
        }
    }

    @Override
    public void clientTick() {
        if (this.soupTicks <= 0) {
            this.beatCooldown = 0;
            return;
        }
        this.soupTicks--;
        if (this.beatCooldown > 0) {
            this.beatCooldown--;
            return;
        }
        float progress = 1.0F - (float) this.soupTicks / SOUP_TICKS;
        this.beatCooldown = Math.round(SLOWEST_BEAT_INTERVAL - (SLOWEST_BEAT_INTERVAL - FASTEST_BEAT_INTERVAL) * progress);
        this.player.playSoundToPlayer(SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 0.5F + 0.5F * progress, 1.0F);
        this.clientLastBeatMs = System.currentTimeMillis();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.soupTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.soupTicks = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("soupTicks", this.soupTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.soupTicks = tag.contains("soupTicks") ? tag.getInt("soupTicks") : 0;
    }
}

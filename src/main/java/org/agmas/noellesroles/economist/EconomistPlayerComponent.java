package org.agmas.noellesroles.economist;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.EconomistReportS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 经济学家《经济财报》的观测状态：
 * 购买后 20 秒内实时观测（除自己外）金币最多的玩家，期间商店库存为 0，观测结束时补货。
 */
public class EconomistPlayerComponent implements Component, ServerTickingComponent {
    public static final ComponentKey<EconomistPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "economist"), EconomistPlayerComponent.class);

    /** 观测时长，同时也是商店补货冷却。 */
    public static final int OBSERVATION_TICKS = 20 * 20;
    private static final int PUSH_INTERVAL_TICKS = 10;

    private final PlayerEntity player;
    @Nullable
    private UUID observeTargetUuid;
    private int observeTicksRemaining;
    private int lastKnownBalance;

    public EconomistPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /** 商店购买回调：选中目标并开始观测；没有其他可观测玩家时返回 false（购买失败、不扣钱）。 */
    public boolean beginObservation() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        UUID target = findRichestOtherPlayer(serverPlayer);
        if (target == null) {
            return false;
        }
        this.observeTargetUuid = target;
        this.observeTicksRemaining = OBSERVATION_TICKS;
        this.lastKnownBalance = 0;
        pushObservation(serverPlayer);
        return true;
    }

    @Override
    public void serverTick() {
        if (this.observeTicksRemaining <= 0 || !(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        this.observeTicksRemaining--;
        if (this.observeTicksRemaining % PUSH_INTERVAL_TICKS == 0 || this.observeTicksRemaining <= 0) {
            pushObservation(serverPlayer);
        }
        if (this.observeTicksRemaining <= 0) {
            finishObservation(serverPlayer);
        }
    }

    private void pushObservation(ServerPlayerEntity observer) {
        if (this.observeTargetUuid != null) {
            PlayerEntity target = observer.getServerWorld().getPlayerByUuid(this.observeTargetUuid);
            if (target != null) {
                this.lastKnownBalance = PlayerShopComponent.KEY.get(target).getBalance();
            }
        }
        ServerPlayNetworking.send(observer,
                new EconomistReportS2CPacket(this.lastKnownBalance, Math.max(0, this.observeTicksRemaining)));
    }

    /** 观测结束：通知客户端隐藏该行，并给商店补货。 */
    private void finishObservation(ServerPlayerEntity observer) {
        this.observeTargetUuid = null;
        this.observeTicksRemaining = 0;
        ServerPlayNetworking.send(observer, new EconomistReportS2CPacket(0, 0));
        PlayerShopComponent.KEY.get(observer).initializeShop(ShopUtils.getShopEntriesForPlayer(observer));
    }

    @Nullable
    private UUID findRichestOtherPlayer(ServerPlayerEntity self) {
        ServerWorld world = self.getServerWorld();
        UUID richest = null;
        int highestBalance = Integer.MIN_VALUE;
        for (ServerPlayerEntity candidate : world.getPlayers()) {
            if (candidate == self || !GameFunctions.isPlayerPlayingAndAlive(candidate)) {
                continue;
            }
            int balance = PlayerShopComponent.KEY.get(candidate).getBalance();
            if (balance > highestBalance) {
                highestBalance = balance;
                richest = candidate.getUuid();
            }
        }
        return richest;
    }

    public void reset() {
        this.observeTargetUuid = null;
        this.observeTicksRemaining = 0;
        this.lastKnownBalance = 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("observeTicksRemaining", this.observeTicksRemaining);
        tag.putInt("lastKnownBalance", this.lastKnownBalance);
        if (this.observeTargetUuid != null) {
            tag.putUuid("observeTargetUuid", this.observeTargetUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.observeTicksRemaining = tag.contains("observeTicksRemaining") ? tag.getInt("observeTicksRemaining") : 0;
        this.lastKnownBalance = tag.contains("lastKnownBalance") ? tag.getInt("lastKnownBalance") : 0;
        this.observeTargetUuid = tag.contains("observeTargetUuid") ? tag.getUuid("observeTargetUuid") : null;
    }
}

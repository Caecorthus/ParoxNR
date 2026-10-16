package org.agmas.noellesroles.bodyguard;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 保镖玩家组件
 * 空手按技能键进入潜行格挡姿态:强制潜行、锁视角、锁热栏,格挡正面刀刺与球棒击打。
 */
public class BodyguardPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BodyguardPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "bodyguard"), BodyguardPlayerComponent.class);

    /** 格挡成功后的技能冷却:90 秒 */
    public static final int BLOCK_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    /** 主动取消姿态的冷却:3 秒 */
    public static final int CANCEL_COOLDOWN_TICKS = GameConstants.getInTicks(0, 3);
    /** 格挡后攻击者匕首的物品冷却:2 秒 */
    public static final int KNIFE_BLOCK_OVERRIDE_TICKS = GameConstants.getInTicks(0, 2);
    /** 格挡后攻击者球棒的物品冷却:6 秒 */
    public static final int BAT_BLOCK_COOLDOWN_TICKS = GameConstants.getInTicks(0, 6);

    private final PlayerEntity player;

    private boolean stanceActive = false;
    private long stanceStartedTick = -1;

    public BodyguardPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.stanceActive = false;
        this.stanceStartedTick = -1;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.stanceActive);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.stanceActive = buf.readBoolean();
    }

    /**
     * 进入姿态。条件由调用方(技能键处理)校验,此处做兜底守卫。
     */
    public void startStance() {
        if (this.stanceActive) {
            return;
        }
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        if (!GameWorldComponent.KEY.get(serverWorld).isRole(player, Noellesroles.BODYGUARD)) {
            return;
        }
        if (SwallowedPlayerComponent.isPlayerSwallowed(player) || player.hasVehicle()) {
            return;
        }
        if (!player.getMainHandStack().isEmpty()) {
            return;
        }
        this.stanceActive = true;
        this.stanceStartedTick = serverWorld.getTime();
        this.sync();
    }

    /**
     * 主动取消:姿态结束并进入 3 秒冷却。
     */
    public void cancelStance() {
        if (!this.stanceActive) {
            return;
        }
        this.stanceActive = false;
        this.stanceStartedTick = -1;
        AbilityPlayerComponent.KEY.get(this.player).setCooldown(CANCEL_COOLDOWN_TICKS);
        this.sync();
    }

    /**
     * 格挡成功:姿态结束并进入 90 秒冷却。
     */
    public void onBlockedAttack() {
        this.stanceActive = false;
        this.stanceStartedTick = -1;
        AbilityPlayerComponent.KEY.get(this.player).setCooldown(BLOCK_COOLDOWN_TICKS);
        this.sync();
    }

    /**
     * 无惩罚结束姿态(死亡/换局/被吞噬/主手被填充等)。
     */
    public void endStance() {
        if (!this.stanceActive) {
            return;
        }
        this.stanceActive = false;
        this.stanceStartedTick = -1;
        this.sync();
    }

    public boolean isStanceActive() {
        return this.stanceActive;
    }

    public long getStanceStartedTick() {
        return this.stanceStartedTick;
    }

    /**
     * 格挡判定:与防御者水平朝向点积 > 0,即攻击者位于水平前方半球(同原版盾牌)。
     */
    public boolean blocksAttacker(PlayerEntity attacker) {
        if (!this.stanceActive || attacker == null) {
            return false;
        }
        Vec3d look = this.player.getRotationVec(1.0F);
        Vec3d toAttacker = attacker.getPos().subtract(this.player.getPos());
        Vec3d horizontalLook = new Vec3d(look.x, 0.0, look.z);
        Vec3d horizontalTo = new Vec3d(toAttacker.x, 0.0, toAttacker.z);
        if (horizontalLook.lengthSquared() < 1.0E-6 || horizontalTo.lengthSquared() < 1.0E-6) {
            return false;
        }
        return horizontalLook.normalize().dotProduct(horizontalTo.normalize()) > 0.0;
    }

    @Override
    public void serverTick() {
        if (!this.stanceActive) {
            return;
        }
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            this.endStance();
            return;
        }
        boolean valid = GameFunctions.isPlayerPlayingAndAlive(player)
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && GameWorldComponent.KEY.get(serverWorld).isRole(player, Noellesroles.BODYGUARD)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player)
                && !player.hasVehicle()
                && player.getMainHandStack().isEmpty();
        if (!valid) {
            // 死亡/换局/旁观/创造/被吞噬/骑乘/主手被填充:免惩罚自动结束
            this.endStance();
            return;
        }
        // 服务端锚定蹲姿,使元数据向所有客户端同步潜行姿态
        ((ServerPlayerEntity) this.player).setSneaking(true);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("stanceActive", this.stanceActive);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.stanceActive = tag.getBoolean("stanceActive");
    }
}

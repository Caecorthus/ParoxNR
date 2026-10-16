package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;

/**
 * 误杀冷却惩罚：开枪误杀的无辜者若死于误杀惩罚（SHOT_INNOCENT，默认惩罚模式下击杀开枪者），
 * 他遗落的枪被标记为误杀枪，拾取时给拾取者 15 秒左轮冷却。
 * <p>
 * 地图惩罚为「只掉枪不杀」时开枪者没有死亡，不触发本惩罚，符合"玩家死于误杀"的前提。
 */
public final class MisKillGunCooldownHelper {
    private MisKillGunCooldownHelper() {}

    /** 误杀枪拾取冷却 15 秒。 */
    public static final int PICKUP_COOLDOWN_TICKS = GameConstants.getInTicks(0, 15);

    public static void handleMisKillShooterDeath(PlayerEntity shooter, Identifier deathReason) {
        if (!GameConstants.DeathReasons.SHOT_INNOCENT.equals(deathReason)) {
            return;
        }
        if (!(shooter.getWorld() instanceof ServerWorld world)) {
            return;
        }
        // 误杀惩罚掉枪由 Wathe 设置 thrower 为开枪者，据此精确取回这把枪
        for (ItemEntity item : world.getEntitiesByType(TypeFilter.equals(ItemEntity.class),
                entity -> entity.getOwner() != null
                        && shooter.getUuid().equals(entity.getOwner().getUuid())
                        && entity.getStack().isIn(WatheItemTags.GUNS))) {
            ((MisKillGunMark) item).noellesroles$markMisKillGun();
        }
    }
}

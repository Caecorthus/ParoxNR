package org.agmas.noellesroles.bandit;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;
import org.agmas.noellesroles.item.ThrowingAxeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 强盗技能：G=一次性掷出全部飞斧（开局/使用后冷却90秒）；回收飞斧改为商店购买（见 BanditShopHandler）。
 */
public final class BanditSkillHelper {
    public static final int VOLLEY_COOLDOWN_TICKS = 90 * 20;
    private static final float AXE_SPACING = 0.625F; // 与飞斧实体碰撞箱宽度一致，排开后边缘相切无空隙
    private static final double VOLLEY_FORWARD_OFFSET = 0.5;
    private static final float MAX_CHARGE_VELOCITY = 3.0F; // (0.4 + 满蓄力 1.0 * 2.0) * 1.25，满蓄力 60 m/s

    private BanditSkillHelper() {
    }

    public static boolean tryVolley(ServerPlayerEntity player) {
        List<Integer> axeSlots = new ArrayList<>();
        int inventorySize = player.getInventory().size();
        for (int i = 0; i < inventorySize; i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.THROWING_AXE)) {
                axeSlots.add(i);
            }
        }
        if (axeSlots.isEmpty()) {
            player.sendMessage(Text.translatable("tip.noellesroles.bandit.no_axes"), true);
            return false;
        }

        float velocity = MAX_CHARGE_VELOCITY;

        Vec3d look = player.getRotationVec(1.0F);
        Vec3d forwardHorizontal = new Vec3d(look.x, 0.0, look.z);
        forwardHorizontal = forwardHorizontal.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 0.0, 1.0)
                : forwardHorizontal.normalize();
        Vec3d right = new Vec3d(-forwardHorizontal.z, 0.0, forwardHorizontal.x);

        Vec3d center = new Vec3d(player.getX(), player.getEyeY() - 0.1, player.getZ())
                .add(forwardHorizontal.multiply(VOLLEY_FORWARD_OFFSET));

        // 整次施放共享一条击杀序号（多杀奖励递减 100/75/50 按本次施放整体计算）
        ThrowingAxeEntity.AxeKillSequence killSequence = new ThrowingAxeEntity.AxeKillSequence();

        int count = axeSlots.size();
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0) * AXE_SPACING;
            Vec3d spawnPos = center.add(right.multiply(offset));
            ItemStack stack = player.getInventory().getStack(axeSlots.get(i));
            ThrowingAxeItem.spawnAxe(player.getWorld(), player, stack, spawnPos.x, spawnPos.y, spawnPos.z, velocity, 0.0F, killSequence);
            player.getInventory().removeStack(axeSlots.get(i), 1);
        }
        player.getInventory().markDirty();

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "volley");
        extra.putInt("count", count);
        GameRecordManager.recordSkillUse(player, Noellesroles.BANDIT_ID, null, extra);
        return true;
    }

    /**
     * 当前维度内、投掷者是该玩家的、尚存在于世界中的飞斧（飞行中或插在方块上）。
     * owner 无法解析（离线/他维）的实体跳过，避免误删。
     */
    public static List<ThrowingAxeEntity> findRecoverableAxes(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        return new ArrayList<>(world.getEntitiesByType(NoellesRolesEntities.THROWING_AXE_ENTITY, axe -> {
            Entity owner = axe.getOwner();
            return owner != null && owner.getUuid().equals(player.getUuid());
        }));
    }
}

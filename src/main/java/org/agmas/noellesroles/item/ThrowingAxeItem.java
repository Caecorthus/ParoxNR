package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThrowingAxeItem extends Item {
    private static final int LOOSE_END_COOLDOWN_TICKS = 20 * 30;

    public ThrowingAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) return;
        // 蓄力期间飞斧被「掷出全部」等技能消耗时，松手不应再凭空掷出一把
        if (stack.isEmpty()) return;

        int useDuration = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float power = getPowerForTime(useDuration);

        if (power < 0.25F) return;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (!world.isClient) {
            // 曲线整体 ×1.25：满蓄力 3 格/tick（60 m/s），蓄力仍影响速度
            float velocity = (0.4F + (power * 2.0F)) * 1.25F;
            spawnAxe(world, player, stack, player.getX(), player.getEyeY() - 0.1, player.getZ(), velocity, 1.0F, null);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
            }
        }

        stack.decrementUnlessCreative(1, player);
        if (!world.isClient && GameWorldComponent.KEY.get(world).isRole(player, WatheRoles.LOOSE_END)) {
            player.getItemCooldownManager().set(this, LOOSE_END_COOLDOWN_TICKS);
        }
        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    /**
     * 在指定位置生成一把沿投掷者视线方向飞行的飞斧，供普通投掷与强盗「掷出全部」共用。
     * killSequence 为 null 时该飞斧自建独立序列（普通投掷）；齐掷传入共享序列以便整次施放共用击杀序号。
     */
    public static void spawnAxe(World world, PlayerEntity thrower, ItemStack stack,
                                double x, double y, double z, float velocity, float divergence,
                                @Nullable ThrowingAxeEntity.AxeKillSequence killSequence) {
        ThrowingAxeEntity axeEntity = new ThrowingAxeEntity(NoellesRolesEntities.THROWING_AXE_ENTITY, world);
        axeEntity.setOwner(thrower);
        axeEntity.initFromStack(stack);
        axeEntity.setPosition(x, y, z);
        axeEntity.setVelocity(thrower, thrower.getPitch(), thrower.getYaw(), 0.0F, velocity, divergence);
        axeEntity.setKillSequence(killSequence);
        world.spawnEntity(axeEntity);
    }

    public static float getPowerForTime(int time) {
        float f = (float) time / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.throwing_axe.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}

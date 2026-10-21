package org.agmas.noellesroles.mixin.wathe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.util.OpeningKnockbackSuppressionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 屏蔽 LivingEntity#damage 内置的 0.4 击退（wathe 只取消 applyDamage，伤害流程其余副作用照旧，
 * 其中 takeKnockback(0.4, ...) 不经过 PlayerEntity#attack，是开局压制最容易漏的一条击退路径）。
 * <p>
 * 做法：damage() 在击退之前恰好调用一次 DamageSource#getAttacker，借它标记"本次伤害的攻击者
 * 正处于开局压制窗口"，再到唯一的 takeKnockback 调用点决定是否跳过。
 * 受害者仍保留受伤动画、hurtTime 等副作用，只是不被击飞；客户端窗口集合为空自然放行。
 */
@Mixin(LivingEntity.class)
public class DamageKnockbackSuppressMixin {
    @Unique
    private boolean noellesroles$suppressNextDamageKnockback;

    @WrapOperation(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;"))
    private Entity noellesroles$captureSuppressedAttacker(DamageSource source, Operation<Entity> original) {
        Entity attacker = original.call(source);
        noellesroles$suppressNextDamageKnockback = attacker instanceof PlayerEntity player
                && OpeningKnockbackSuppressionHelper.isSuppressed(player);
        return attacker;
    }

    @WrapOperation(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V"))
    private void noellesroles$skipSuppressedDamageKnockback(LivingEntity victim, double strength, double x, double z, Operation<Void> original) {
        if (noellesroles$suppressNextDamageKnockback) {
            noellesroles$suppressNextDamageKnockback = false;
            return;
        }
        original.call(victim, strength, x, z);
    }
}

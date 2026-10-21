package org.agmas.noellesroles.mixin.wathe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.agmas.noellesroles.util.OpeningKnockbackSuppressionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 开局压制窗口内屏蔽 PlayerEntity#attack 的近战命中结算：
 * 对玩家目标直接跳过 damage()（无受伤动画/红闪/受伤音效，damage()==false 也让
 * 击退、横扫、暴击音效分支整体不执行）；另有两处 takeKnockback 包装作双保险，
 * 兜住未来可能绕过 damage 返回值判定重新触发的击退调用。
 * 攻击者侧挥刀动画由独立的摆臂包驱动，不受影响。
 */
@Mixin(PlayerEntity.class)
public class KnifeKnockbackSuppressMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean noellesroles$skipSuppressedDamage(Entity victim, DamageSource source, float amount, Operation<Boolean> original) {
        if (victim instanceof PlayerEntity
                && OpeningKnockbackSuppressionHelper.isSuppressed((PlayerEntity) (Object) this)) {
            return false;
        }
        return original.call(victim, source, amount);
    }

    /**
     * 压制命中时 damage() 返回 false，原版会广播"打到但无伤害"的 NODAMAGE 闷响；
     * 冲刺全力出手还会广播挥砍 KNOCKBACK 声。两者对压制中的攻击者一律不播
     * （按 SoundEvent 参数过滤，横扫/暴击/轻重击等其余音效原样放行）。
     */
    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
    private void noellesroles$muffleSuppressedAttackSounds(World world, PlayerEntity excluded, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, Operation<Void> original) {
        if (OpeningKnockbackSuppressionHelper.isSuppressed((PlayerEntity) (Object) this)
                && (sound == SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE
                        || sound == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK)) {
            return;
        }
        original.call(world, excluded, x, y, z, sound, category, volume, pitch);
    }

    @WrapOperation(method = "attack", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V", ordinal = 0),
            @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V", ordinal = 1)
    })
    private void noellesroles$suppressOpeningKnockback(LivingEntity victim, double strength, double x, double z, Operation<Void> original) {
        if (OpeningKnockbackSuppressionHelper.isSuppressed((PlayerEntity) (Object) this)) {
            return;
        }
        original.call(victim, strength, x, z);
    }
}

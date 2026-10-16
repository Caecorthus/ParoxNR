package org.agmas.noellesroles.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 射程无限制效果 - 拥有此效果的玩家左轮射程不受 30/65 格限制（白兰地）
 */
public class UnlimitedRangeEffect extends StatusEffect {
    public UnlimitedRangeEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xC77B3E); // 琥珀
    }
}

package org.agmas.noellesroles.util;

/**
 * 由 {@code org.agmas.noellesroles.mixin.wathe.MisKillGunItemEntityMixin} 织入到原版 {@code ItemEntity} 上，
 * 标记误杀惩罚掉落的枪：被拾取时给拾取者附加 15 秒左轮冷却。
 */
public interface MisKillGunMark {

    /** 标记为误杀惩罚遗留的枪。 */
    void noellesroles$markMisKillGun();
}

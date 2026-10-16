package org.agmas.noellesroles.mixin.dreameater;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 wathe {@code PlayerMoodComponent$Task} 的私有静态 {@code $VALUES} 数组，
 * 供 {@link TaskEnumExtensionMixin} 追加本模组的任务槽位。
 * <p>
 * {@code @Mutable} 是必须的：该字段在字节码里带 ACC_FINAL，不去掉的话 Mixin 不允许 putstatic。
 */
@Mixin(PlayerMoodComponent.Task.class)
public interface PlayerMoodComponentTaskAccessor {

    @Accessor("$VALUES")
    @Mutable
    static void noellesroles$setValues(PlayerMoodComponent.Task[] values) {
        throw new AssertionError("mixin accessor");
    }
}

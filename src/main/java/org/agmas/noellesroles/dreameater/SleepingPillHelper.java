package org.agmas.noellesroles.dreameater;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 安眠药食物标记：记录下毒者 UUID。
 * 食用时视为一次噬梦者技能命中；因为不写 wathe 的 POISONER 组件，不会触发原版中毒。
 */
public final class SleepingPillHelper {
    public static final ComponentType<String> SLEEPING_PILL_POISONER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "sleeping_pill_poisoner"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    private SleepingPillHelper() {}

    public static void init() {
    }

    public static void markFood(ItemStack stack, String poisonerUuid) {
        stack.set(SLEEPING_PILL_POISONER, poisonerUuid);
    }

    public static String getPoisoner(ItemStack stack) {
        return stack.get(SLEEPING_PILL_POISONER);
    }
}

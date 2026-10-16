package org.agmas.noellesroles;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    // 安眠药餐盘粒子：wathe 红毒粒子（骷髅头贴图）的蓝色重染色版本
    public static final SimpleParticleType SLEEPING_PILL_POISON = Registry.register(
            Registries.PARTICLE_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "sleeping_pill_poison"),
            FabricParticleTypes.simple(true)
    );

    public static void init() {
    }
}

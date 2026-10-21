package org.agmas.noellesroles;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.entity.RoleMineEntity;
import org.agmas.noellesroles.entity.PoisonGasBombEntity;
import org.agmas.noellesroles.entity.PoisonGasCloudEntity;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;
import org.agmas.noellesroles.entity.HunterTrapEntity;
import org.agmas.noellesroles.entity.PuppetEntity;

public class NoellesRolesEntities {
    public static final EntityType<RoleMineEntity> ROLE_MINE_ENTITY_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "cube"),
            EntityType.Builder.create(RoleMineEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build("cube")
    );

    public static final EntityType<PoisonGasBombEntity> POISON_GAS_BOMB_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "poison_gas_bomb"),
            EntityType.Builder.create(PoisonGasBombEntity::new, SpawnGroup.MISC).dimensions(0.2f, 0.2f).build("poison_gas_bomb")
    );

    public static final EntityType<PoisonGasCloudEntity> POISON_GAS_CLOUD_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "poison_gas_cloud"),
            EntityType.Builder.create(PoisonGasCloudEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("poison_gas_cloud")
    );

    // 飞斧实体：碰撞箱边长为原来的 1.25 倍（0.5 * 1.25 = 0.625），放大后更易命中
    public static final EntityType<ThrowingAxeEntity> THROWING_AXE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "throwing_axe"),
            EntityType.Builder.<ThrowingAxeEntity>create(ThrowingAxeEntity::new, SpawnGroup.MISC).dimensions(0.625f, 0.625f).build("throwing_axe")
    );

    public static final EntityType<HunterTrapEntity> HUNTER_TRAP_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "hunter_trap"),
            EntityType.Builder.<HunterTrapEntity>create(HunterTrapEntity::new, SpawnGroup.MISC).dimensions(0.6f, 0.1f).build("hunter_trap")
    );

    // 傀儡师：闲置傀儡 / 本体替身（站立人形，追踪距离与玩家一致，供本能透视远距离显示）
    public static final EntityType<PuppetEntity> PUPPET_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "puppet"),
            EntityType.Builder.<PuppetEntity>create(PuppetEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .maxTrackingRange(32)
                    .trackingTickInterval(2)
                    .build("puppet")
    );

    public static void init() {
        FabricDefaultAttributeRegistry.register(PUPPET_ENTITY, PuppetEntity.createPuppetAttributes());
    }
}

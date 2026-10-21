package org.agmas.noellesroles;

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
import org.agmas.noellesroles.breacher.BreacherMarkerEntity;
import org.agmas.noellesroles.recaller.RecallerAnchorEntity;

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

    public static final EntityType<BreacherMarkerEntity> BREACHER_MARKER_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "breacher_marker"),
            EntityType.Builder.<BreacherMarkerEntity>create(BreacherMarkerEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("breacher_marker")
    );

    // maxTrackingRange(128) 照抄 Wathe 实体先例：锚点需全图被所有客户端追踪，否则远距离描边不出现
    public static final EntityType<RecallerAnchorEntity> RECALLER_ANCHOR_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "recaller_anchor"),
            EntityType.Builder.<RecallerAnchorEntity>create(RecallerAnchorEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).maxTrackingRange(128).build("recaller_anchor")
    );

    public static void init() {}
}

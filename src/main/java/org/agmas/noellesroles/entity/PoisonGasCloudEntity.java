package org.agmas.noellesroles.entity;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.agmas.noellesroles.hallucination.HallucinationDummyInteractionHelper;
import org.agmas.noellesroles.hallucination.HallucinationDummyState;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;
import org.agmas.noellesroles.util.AreaDamageImmunityHelper;
import org.joml.Vector3f;

import java.util.*;

/**
 * 毒气云实体
 * - BFS扩散系统，最多500个方块
 * - 在毒气中停留5秒(100 ticks)将中毒
 * - 30秒(600 ticks)后消散
 */
public class PoisonGasCloudEntity extends Entity {
    private static final int MAX_GAS_BLOCKS = 500;
    private static final int MAX_LIFETIME = 600; // 30秒
    private static final int SPREAD_INTERVAL = 8; // 每8 ticks扩散一次，约10秒填满5x5x20车厢
    private static final int EXPOSURE_THRESHOLD = 100; // 5秒暴露阈值
    private static final double MAX_SPREAD_RADIUS_SQ = 20.0 * 20.0; // 最大扩散半径20格（平方）
    private static final DustParticleEffect GAS_PARTICLE = new DustParticleEffect(new Vector3f(0.3f, 0.8f, 0.2f), 1.5f);

    private final Set<BlockPos> gasBlocks = new HashSet<>();
    private final List<BlockPos> gasBlockList = new ArrayList<>();
    private Set<BlockPos> frontier = new HashSet<>();
    private final Map<UUID, Integer> exposureTicks = new HashMap<>();
    private final Map<UUID, Integer> dummyExposureTicks = new HashMap<>();
    private final Map<UUID, Integer> puppetExposureTicks = new HashMap<>();
    private final Set<UUID> playersInGas = new HashSet<>();
    private UUID ownerUuid;
    private int age = 0;

    public PoisonGasCloudEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (age > MAX_LIFETIME) {
            clearAllGasExhaustion();
            this.discard();
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        // 初始化起始位置
        if (age == 1) {
            BlockPos startPos = this.getBlockPos();
            if (gasBlocks.add(startPos)) {
                gasBlockList.add(startPos);
            }
            frontier.add(startPos);
        }

        // BFS扩散（被阻挡的边缘方块保留在frontier中，支持门打开后继续扩散）
        if (age % SPREAD_INTERVAL == 0 && !frontier.isEmpty() && gasBlocks.size() < MAX_GAS_BLOCKS) {
            Set<BlockPos> newFrontier = new HashSet<>();
            for (BlockPos pos : frontier) {
                if (gasBlocks.size() >= MAX_GAS_BLOCKS) break;
                boolean stillEdge = false;
                for (Direction direction : Direction.values()) {
                    if (gasBlocks.size() >= MAX_GAS_BLOCKS) break;
                    BlockPos neighbor = pos.offset(direction);
                    if (gasBlocks.contains(neighbor)) continue;
                    // 超出最大扩散半径则跳过
                    if (neighbor.getSquaredDistance(this.getBlockPos()) > MAX_SPREAD_RADIUS_SQ) continue;
                    VoxelShape fromShape = serverWorld.getBlockState(pos).getCollisionShape(serverWorld, pos);
                    VoxelShape toShape = serverWorld.getBlockState(neighbor).getCollisionShape(serverWorld, neighbor);
                    // 源方块出口检测（方向性）+ 目标方块入口检测（体积+面）
                    if (doesShapeBlockExit(fromShape, direction)
                        || isBlockTooSolid(toShape)
                        || doesShapeBlockEntry(toShape, direction)) {
                        stillEdge = true; // 有被阻挡的邻居，保留在frontier中等待重新检测
                        continue;
                    }
                    if (gasBlocks.add(neighbor)) {
                        gasBlockList.add(neighbor);
                    }
                    newFrontier.add(neighbor);
                }
                if (stillEdge) {
                    newFrontier.add(pos); // 保留边缘方块，门打开后可继续扩散
                }
            }
            frontier = newFrontier;
        }

        // 玩家中毒检测（毒师免疫）
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) continue;
            if (gameWorld.isRole(player, Noellesroles.POISONER)) continue;
            if (AreaDamageImmunityHelper.isImmuneToAreaDamage(player)) {
                exposureTicks.put(player.getUuid(), 0);
                if (playersInGas.remove(player.getUuid())) {
                    PlayerStaminaComponent staminaComp = PlayerStaminaComponent.KEY.get(player);
                    if (!staminaComp.isInfiniteStamina()) {
                        staminaComp.setExhausted(false);
                        staminaComp.setSprintingTicks(76.0f);
                    }
                }
                continue;
            }

            Box box = player.getBoundingBox();
            boolean inGas = false;
            for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX) && !inGas; x++) {
                for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY) && !inGas; y++) {
                    for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ) && !inGas; z++) {
                        if (gasBlocks.contains(new BlockPos(x, y, z))) {
                            inGas = true;
                        }
                    }
                }
            }
            if (inGas) {
                int ticks = exposureTicks.getOrDefault(player.getUuid(), 0) + 1;
                exposureTicks.put(player.getUuid(), ticks);

                // 体力压制：在毒气中扣光体力，无法疾跑
                PlayerStaminaComponent staminaComp = PlayerStaminaComponent.KEY.get(player);
                if (!staminaComp.isInfiniteStamina()) {
                    staminaComp.setSprintingTicks(0);
                    staminaComp.setExhausted(true);
                    playersInGas.add(player.getUuid());
                }

                if (ticks >= EXPOSURE_THRESHOLD) {
                    PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(player);
                    // 固定中毒时间 20秒 (400 ticks)，已中毒则加速
                    int baseTicks = 20 * 20;
                    int poisonTime = poisonComp.poisonTicks > 0
                            ? Math.max(1, poisonComp.poisonTicks - baseTicks)
                            : baseTicks;
                    poisonComp.setPoisonTicks(poisonTime, ownerUuid, Noellesroles.POISON_SOURCE_GAS_BOMB);
                    exposureTicks.put(player.getUuid(), 0);
                }
            } else {
                exposureTicks.put(player.getUuid(), 0);

                // 离开毒气时恢复体力，解除疲惫
                if (playersInGas.remove(player.getUuid())) {
                    PlayerStaminaComponent staminaComp = PlayerStaminaComponent.KEY.get(player);
                    if (!staminaComp.isInfiniteStamina()) {
                        staminaComp.setExhausted(false);
                        staminaComp.setSprintingTicks(76.0f);
                    }
                }
            }
        }

        // 傀儡师：闲置傀儡吸满毒气即倒地；本体替身吸满毒气则傀儡师被迫回到本体，毒落在本体身上
        Set<UUID> seenPuppets = new HashSet<>();
        for (PuppetEntity puppet : serverWorld.getEntitiesByType(NoellesRolesEntities.PUPPET_ENTITY, p -> isBoxInGas(p.getBoundingBox()))) {
            seenPuppets.add(puppet.getUuid());
            int ticks = puppetExposureTicks.getOrDefault(puppet.getUuid(), 0) + 1;
            puppetExposureTicks.put(puppet.getUuid(), ticks);
            if (ticks < EXPOSURE_THRESHOLD) {
                continue;
            }
            puppetExposureTicks.put(puppet.getUuid(), 0);
            ServerPlayerEntity gasOwner = ownerUuid != null ? serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid) : null;
            if (!puppet.isShell()) {
                PuppeteerControl.onEntityHit(puppet, gasOwner, GameConstants.DeathReasons.POISON);
                continue;
            }
            ServerPlayerEntity shellOwner = PuppeteerControl.returnShellOwnerToBody(puppet);
            if (shellOwner != null && !gameWorld.isRole(shellOwner, Noellesroles.POISONER)
                    && !AreaDamageImmunityHelper.isImmuneToAreaDamage(shellOwner)) {
                PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(shellOwner);
                int baseTicks = 20 * 20;
                int poisonTime = poisonComp.poisonTicks > 0
                        ? Math.max(1, poisonComp.poisonTicks - baseTicks)
                        : baseTicks;
                poisonComp.setPoisonTicks(poisonTime, ownerUuid, Noellesroles.POISON_SOURCE_GAS_BOMB);
            }
        }
        puppetExposureTicks.keySet().removeIf(uuid -> !seenPuppets.contains(uuid));

        // 粒子效果
        if (ownerUuid != null) {
            ServerPlayerEntity owner = serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid);
            if (owner != null) {
                HallucinationPlayerComponent hallucinationComponent = HallucinationPlayerComponent.KEY.get(owner);
                Set<UUID> seenDummyIds = new HashSet<>();
                for (HallucinationDummyState dummy : hallucinationComponent.getDummyStates()) {
                    seenDummyIds.add(dummy.id());
                    if (isDummyInGas(dummy.position())) {
                        int ticks = dummyExposureTicks.getOrDefault(dummy.id(), 0) + 1;
                        dummyExposureTicks.put(dummy.id(), ticks);
                        if (ticks >= EXPOSURE_THRESHOLD) {
                            hallucinationComponent.applyPoisonToDummy(dummy.id(), 20 * 20, ownerUuid, Noellesroles.POISON_SOURCE_GAS_BOMB);
                            dummyExposureTicks.put(dummy.id(), 0);
                            hallucinationComponent.sync();
                        }
                    } else {
                        dummyExposureTicks.put(dummy.id(), 0);
                    }
                }
                dummyExposureTicks.keySet().removeIf(dummyId -> !seenDummyIds.contains(dummyId));
            } else {
                dummyExposureTicks.clear();
            }
        }

        if (!gasBlockList.isEmpty()) {
            int particleCount = 4 + serverWorld.random.nextInt(3); // 4-6个粒子
            for (int i = 0; i < particleCount; i++) {
                BlockPos pos = gasBlockList.get(serverWorld.random.nextInt(gasBlockList.size()));
                serverWorld.spawnParticles(
                        GAS_PARTICLE,
                        pos.getX() + 0.5 + serverWorld.random.nextGaussian() * 0.3,
                        pos.getY() + 0.5 + serverWorld.random.nextGaussian() * 0.3,
                        pos.getZ() + 0.5 + serverWorld.random.nextGaussian() * 0.3,
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    /**
     * 毒气消散时清除所有受影响玩家的体力压制
     */
    private void clearAllGasExhaustion() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        for (UUID uuid : playersInGas) {
            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                PlayerStaminaComponent staminaComp = PlayerStaminaComponent.KEY.get(player);
                if (!staminaComp.isInfiniteStamina()) {
                    staminaComp.setExhausted(false);
                    staminaComp.setSprintingTicks(76.0f);
                }
            }
        }
        playersInGas.clear();
        dummyExposureTicks.clear();
    }

    private boolean isBoxInGas(Box box) {
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); x++) {
            for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); y++) {
                for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); z++) {
                    if (gasBlocks.contains(new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isDummyInGas(Vec3d dummyPosition) {
        Box box = HallucinationDummyInteractionHelper.getDummyBox(dummyPosition);
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); x++) {
            for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); y++) {
                for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); z++) {
                    if (gasBlocks.contains(new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double getCrossSection(Box box, Direction.Axis moveAxis) {
        Direction.Axis perp1, perp2;
        switch (moveAxis) {
            case X -> { perp1 = Direction.Axis.Y; perp2 = Direction.Axis.Z; }
            case Y -> { perp1 = Direction.Axis.X; perp2 = Direction.Axis.Z; }
            default -> { perp1 = Direction.Axis.X; perp2 = Direction.Axis.Y; }
        }
        return getAxisSpan(box, perp1) * getAxisSpan(box, perp2);
    }

    private double getAxisSpan(Box box, Direction.Axis axis) {
        return box.getMax(axis) - box.getMin(axis);
    }

    /**
     * 出口检测：气体能否从源方块的某个面离开
     * (a) 碰撞箱覆盖出口面 → 阻挡（屏障面板等贴面方块）
     * (b) 碰撞箱形成中间墙壁（深度>0.1）→ 阻挡（门等中部方块）
     * 均要求垂直截面覆盖率 > 50%
     */
    private boolean doesShapeBlockExit(VoxelShape shape, Direction direction) {
        if (shape.isEmpty()) return false;

        Direction.Axis moveAxis = direction.getAxis();
        for (Box box : shape.getBoundingBoxes()) {
            if (getCrossSection(box, moveAxis) <= 0.5) {
                continue;
            }

            // (a) 碰撞箱覆盖出口面
            boolean reachesFace = direction.getDirection() == Direction.AxisDirection.POSITIVE
                    ? box.getMax(moveAxis) > 0.99
                    : box.getMin(moveAxis) < 0.01;
            if (reachesFace) {
                return true;
            }

            // (b) 中间墙壁（深度>0.1，如门 4/16=0.25）
            double depth = getAxisSpan(box, moveAxis);
            if (depth > 0.1 && box.getMin(moveAxis) > 0.01 && box.getMax(moveAxis) < 0.99) {
                return true;
            }
        }
        return false;
    }

    /**
     * 入口体积检测：仅完整实心方块不允许气体进入
     * 不再把楼梯、半砖等由多个碰撞盒组成的不完整方块误判为整块阻挡
     */
    private boolean isBlockTooSolid(VoxelShape shape) {
        if (shape.isEmpty()) return false;
        List<Box> boxes = shape.getBoundingBoxes();
        if (boxes.size() != 1) {
            return false;
        }

        Box box = boxes.get(0);
        return getAxisSpan(box, Direction.Axis.X) > 0.99
                && getAxisSpan(box, Direction.Axis.Y) > 0.99
                && getAxisSpan(box, Direction.Axis.Z) > 0.99
                && box.minX < 0.01 && box.minY < 0.01 && box.minZ < 0.01
                && box.maxX > 0.99 && box.maxY > 0.99 && box.maxZ > 0.99;
    }

    /**
     * 入口面检测：碰撞箱是否覆盖了气体进入的那个面
     * 处理屏障面板等只挡一面的薄方块
     *
     * 气体沿moveDirection移动进入目标方块，入口面在方块的反方向侧：
     * - 气体向SOUTH(+Z)移动 → 入口面在z=0(min端)
     * - 气体向NORTH(-Z)移动 → 入口面在z=1(max端)
     */
    private boolean doesShapeBlockEntry(VoxelShape shape, Direction moveDirection) {
        if (shape.isEmpty()) return false;

        Direction.Axis moveAxis = moveDirection.getAxis();
        for (Box box : shape.getBoundingBoxes()) {
            if (getCrossSection(box, moveAxis) <= 0.5) {
                continue;
            }

            if (moveDirection.getDirection() == Direction.AxisDirection.POSITIVE) {
                if (box.getMin(moveAxis) < 0.01) {
                    return true;
                }
            } else if (box.getMax(moveAxis) > 0.99) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (ownerUuid != null) {
            nbt.putUuid("OwnerUuid", ownerUuid);
        }
        nbt.putInt("Age", age);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("OwnerUuid")) {
            ownerUuid = nbt.getUuid("OwnerUuid");
        }
        age = nbt.getInt("Age");
    }
}

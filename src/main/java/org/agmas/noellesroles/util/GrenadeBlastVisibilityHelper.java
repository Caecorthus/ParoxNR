package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * 手雷爆炸的遮挡判定（手写多点采样版）。
 * <p>
 * Wathe 原版 {@code GrenadeEntity.onCollision} 只按爆炸点周围 3 格 AABB 无条件击杀，
 * 没有做任何视线判定，于是隔着整面墙也会被炸死。这里改成：
 * 从爆炸体积内的若干爆点高度，向受害者碰撞箱上的若干采样点各打一条方块射线，
 * <b>只要任意一条未被方块挡住就判定致死</b>（即「爆点与玩家之间没有被完全隔断」），
 * 全部被挡才免疫。
 * <p>
 * <b>为什么要多个爆点高度</b>：手雷必须触地才爆，落点贴地时爆点只有 y≈0.22。
 * 从这个高度望去矮墙的遮挡夹角极其夸张——玩家贴在 1 格矮墙后时，连头顶的射线都越不过墙顶，
 * 于是出现「上半身明明露在墙上面却炸不死」。真实手雷的杀伤来自整个爆炸球体而非地面一个点，
 * 所以这里在爆炸体积内取多个高度分别判定，任一层能通就算未被隔断。
 * <p>
 * 为什么不用原版 {@code Explosion#getExposure}：
 * <ul>
 *     <li>它的采样点正好取在受害者碰撞箱的边界上（{@code lerp(0/1)} 角点），
 *     站在门框 / 窄走廊 / 贴墙时这些点会落进旁边的方块里，导致所有射线全灭、出现「明明看得见却炸不死」；</li>
 *     <li>它只有一个爆点，且高度固定为传入坐标，无法处理上面说的贴地矮墙夹角问题。</li>
 * </ul>
 * <p>
 * 注意：{@link RaycastContext} 的 {@code Entity} 重载<b>不能传 null</b>
 * （内部会走 {@code ShapeContext.of(entity)} → {@code new BlockShapeContext(entity)}，
 * 而后者构造器第一句就解引用实体，传 null 直接 NPE 崩服）。
 * 上下文实体只影响方块形状的解析上下文（流体、栅栏贴靠连通等），不参与实体碰撞。
 * <p>
 * 遮挡口径（{@link RaycastContext.ShapeType#COLLIDER} + {@link RaycastContext.FluidHandling#NONE}，
 * 与 1.21.1 原版爆炸一致）：
 * <ul>
 *     <li>玻璃、玻璃板、铁栏杆、栅栏、门、台阶：有碰撞箱 → 会挡住；</li>
 *     <li>树叶（默认 {@code requiresTool=false} 无碰撞箱）、水与岩浆、火把、草、藤蔓、竖牌：不挡，照常炸死。</li>
 * </ul>
 */
public final class GrenadeBlastVisibilityHelper {

    /**
     * 诊断开关：启动参数加 {@code -Dnoellesroles.debugGrenadeLos=true} 后，
     * 每次手雷爆炸都会把爆点与每条采样射线的命中情况打到日志（仅用于排查遮挡误判）。
     */
    private static final boolean DEBUG = Boolean.getBoolean("noellesroles.debugGrenadeLos");

    private static final Logger LOGGER = LoggerFactory.getLogger("noellesroles-grenade-los");

    /** 爆炸判定基准高度偏移，与 Wathe 手雷粒子/音效使用的偏移一致（手雷碰撞箱高 0.25）。 */
    private static final double BLAST_ORIGIN_Y_OFFSET = 0.1D;

    /**
     * 爆点相对基准高度向上取的层级（米）。
     * 0 是贴地的真实爆点；更高的层级代表爆炸球体上部的破片方向，
     * 用于让「上半身露在矮墙之上」的玩家不会被贴地爆点看低的夹角误判成完全遮挡。
     * <p>
     * 这些层级会被 {@link #getRoofClearance} 夹紧：抬高后的爆点必须仍在
     * 「基准爆点正上方的无遮挡空气柱」内。否则手雷贴天花板炸时，
     * 抬高后的检测点会穿进天花板甚至进到二层，射线从楼板上方朝二层玩家发射，
     * 中间没有任何方块可挡，造成「一楼炸二楼」。
     */
    private static final double[] ORIGIN_LEVEL_OFFSETS = {0.0D, 1.0D, 1.8D};

    /** 抬高层级的最大偏移，用于给「向上量天花板」的射线定长度。 */
    private static final double ORIGIN_MAX_LEVEL = 1.8D;

    /** 层级顶部与头顶第一个实心方块之间保留的安全间距（米），避免检测点贴在方块底面上。 */
    private static final double ROOF_CLEARANCE_MARGIN = 0.1D;

    /** 采样箱相对受害者碰撞箱的收缩量（米）：避开紧贴身体的方块与地面，避免采样点落在方块内部。 */
    private static final double SAMPLE_BOX_SHRINK = 0.2D;

    /** 收缩后的采样箱最小边长（米）：够矮/够瘦的目标收缩后仍保留一个可用体积。 */
    private static final double SAMPLE_BOX_MIN_SIZE = 0.1D;

    /** 采样高度（0 = 脚底，1 = 头顶）。脚 / 腰 / 胸 / 头各一层，用于覆盖「矮墙露头」「蹲在掩体后」等部分遮挡。 */
    private static final double[] SAMPLE_HEIGHTS = {0.1D, 0.4D, 0.7D, 0.95D};

    private GrenadeBlastVisibilityHelper() {}

    /** 爆炸中心（粒子与音效使用的原点）。 */
    public static Vec3d blastOrigin(@Nullable GrenadeEntity grenade) {
        if (grenade == null) {
            return Vec3d.ZERO;
        }
        return grenade.getPos().add(0.0D, BLAST_ORIGIN_Y_OFFSET, 0.0D);
    }

    /**
     * 爆炸点与目标之间是否存在未被方块完全挡住的通路。
     *
     * @return {@code true} 表示应判定致死（未完全隔断）；{@code false} 表示完全被挡住，应免疫。
     */
    public static boolean hasBlastLineOfSight(@Nullable GrenadeEntity grenade, @Nullable Entity target) {
        if (grenade == null || target == null) {
            return true;
        }
        World world = grenade.getWorld();
        Vec3d baseOrigin = blastOrigin(grenade);
        Box sampleBox = buildSampleBox(target.getBoundingBox());
        boolean debug = DEBUG;
        // 基准爆点正上方到第一个实心方块的距离：所有抬高层级都必须落在这段空气柱内，
        // 否则检测点会穿出天花板跑到别的楼层去（一楼炸二楼）。
        double roofClearance = getRoofClearance(world, baseOrigin, grenade);

        for (double levelOffset : ORIGIN_LEVEL_OFFSETS) {
            if (levelOffset > roofClearance) {
                if (debug) {
                    LOGGER.info("[GrenadeLOS] level={} skipped=roof-clearance({})", levelOffset, fmt(roofClearance));
                }
                continue;
            }
            Vec3d origin = baseOrigin.add(0.0D, levelOffset, 0.0D);
            // 该层爆点若被方块埋住（贴地、嵌进台阶）就直接跳过：起点埋在方块里会让所有射线「起点即命中」。
            if (isInsideCollision(world, origin, grenade)) {
                if (debug) {
                    LOGGER.info("[GrenadeLOS] level={} origin=({},{},{}) skipped=inside-block",
                            levelOffset, fmt(origin.x), fmt(origin.y), fmt(origin.z));
                }
                continue;
            }

            String prefix = "L" + levelOffset + ":";

            // 中心：判定「能否直接看到身体主体」。只有所有层级的中心与四角全被挡才算完全隔断。
            if (isPathClear(grenade, origin, sampleBox.getCenter(), debug, prefix + "center")) {
                logResult(grenade, target, baseOrigin, origin, true);
                return true;
            }

            for (double height : SAMPLE_HEIGHTS) {
                double y = sampleBox.minY + (sampleBox.maxY - sampleBox.minY) * height;
                if (isPathClear(grenade, origin, new Vec3d(sampleBox.minX, y, sampleBox.minZ), debug, prefix + "h" + height + "-minmin")
                        || isPathClear(grenade, origin, new Vec3d(sampleBox.minX, y, sampleBox.maxZ), debug, prefix + "h" + height + "-minmax")
                        || isPathClear(grenade, origin, new Vec3d(sampleBox.maxX, y, sampleBox.minZ), debug, prefix + "h" + height + "-maxmin")
                        || isPathClear(grenade, origin, new Vec3d(sampleBox.maxX, y, sampleBox.maxZ), debug, prefix + "h" + height + "-maxmax")) {
                    logResult(grenade, target, baseOrigin, origin, true);
                    return true;
                }
            }
        }
        logResult(grenade, target, baseOrigin, null, false);
        return false;
    }

    /** 把碰撞箱向内收缩一圈，避免采样点贴在方块表面上造成「起点即命中」。 */
    private static Box buildSampleBox(Box raw) {
        Box shrunk = raw.contract(SAMPLE_BOX_SHRINK);
        if (shrunk.maxX - shrunk.minX < SAMPLE_BOX_MIN_SIZE
                || shrunk.maxY - shrunk.minY < SAMPLE_BOX_MIN_SIZE
                || shrunk.maxZ - shrunk.minZ < SAMPLE_BOX_MIN_SIZE) {
            return raw.contract(Math.min(SAMPLE_BOX_SHRINK, 0.02D));
        }
        return shrunk;
    }

    /**
     * 量出基准爆点正上方到第一个实心方块的距离，用于把抬高的检测点夹在同一个空间内。
     * 手雷贴天花板爆炸时这个值接近 0，于是所有抬高层级都会被跳过，
     * 检测点不会穿到楼板上方去炸别的楼层。
     */
    private static double getRoofClearance(World world, Vec3d baseOrigin, Entity context) {
        if (world == null) {
            return 0.0D;
        }
        Vec3d end = baseOrigin.add(0.0D, ORIGIN_MAX_LEVEL + 1.0D, 0.0D);
        HitResult hit = world.raycast(new RaycastContext(
                baseOrigin,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                context
        ));
        double distance = hit.getType() == HitResult.Type.MISS
                ? ORIGIN_MAX_LEVEL
                : hit.getPos().y - baseOrigin.y - ROOF_CLEARANCE_MARGIN;
        return Math.max(0.0D, distance);
    }

    /** 该点是否落在有碰撞箱的方块内部。 */
    private static boolean isInsideCollision(World world, Vec3d pos, Entity context) {
        BlockPos blockPos = BlockPos.ofFloored(pos);
        BlockState state = world.getBlockState(blockPos);
        VoxelShape shape = state.getCollisionShape(world, blockPos, ShapeContext.of(context));
        if (shape.isEmpty()) {
            return false;
        }
        Vec3d local = pos.subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        return shape.getBoundingBox().contains(local);
    }

    /** 从爆点到采样点打一条方块射线；未被方块挡住（含未走完全程的擦边情况）即视为通路。 */
    private static boolean isPathClear(Entity context, Vec3d origin, Vec3d sample, boolean debug, String label) {
        if (origin.squaredDistanceTo(sample) < 1.0E-6D) {
            // 爆点与采样点几乎重合：按「贴脸爆炸」处理，直接判定致死。
            return true;
        }
        HitResult hit = context.getWorld().raycast(new RaycastContext(
                origin,
                sample,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                context
        ));
        boolean clear = hit.getType() == HitResult.Type.MISS
                || hit.getPos().squaredDistanceTo(origin) + 1.0E-4D >= origin.squaredDistanceTo(sample);
        if (debug) {
            LOGGER.info(
                    "[GrenadeLOS] {} sample=({},{},{}) clear={} hit={} hitPos=({},{},{})",
                    label,
                    fmt(sample.x), fmt(sample.y), fmt(sample.z),
                    clear,
                    hit.getType() == HitResult.Type.MISS ? "MISS" : describeBlock(context.getWorld(), hit.getPos()),
                    fmt(hit.getPos().x), fmt(hit.getPos().y), fmt(hit.getPos().z));
        }
        return clear;
    }

    private static void logResult(GrenadeEntity grenade, Entity target, Vec3d baseOrigin, @Nullable Vec3d lethalOrigin, boolean lethal) {
        if (!DEBUG) {
            return;
        }
        LOGGER.info(
                "[GrenadeLOS] RESULT grenade=({},{},{}) baseOrigin=({},{},{}) lethalOrigin={} target={} lethal={}",
                fmt(grenade.getX()), fmt(grenade.getY()), fmt(grenade.getZ()),
                fmt(baseOrigin.x), fmt(baseOrigin.y), fmt(baseOrigin.z),
                lethalOrigin == null ? "none" : "(" + fmt(lethalOrigin.x) + "," + fmt(lethalOrigin.y) + "," + fmt(lethalOrigin.z) + ")",
                target.getName().getString(), lethal);
    }

    private static String describeBlock(World world, Vec3d pos) {
        BlockPos blockPos = BlockPos.ofFloored(pos);
        return Registries.BLOCK.getId(world.getBlockState(blockPos).getBlock()) + "@" + blockPos.toShortString();
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}

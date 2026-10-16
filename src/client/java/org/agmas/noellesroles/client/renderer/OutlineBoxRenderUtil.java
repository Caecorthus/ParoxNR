package org.agmas.noellesroles.client.renderer;

import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * 线框描边共享工具：门框描边与通用 12 边盒子描边（颜色参数化）。
 * 从 EngineerDoorHighlightRenderer 抽取，供工程师警报与任务点高亮复用。
 */
public final class OutlineBoxRenderUtil {

    private OutlineBoxRenderUtil() {
    }

    /**
     * 渲染门框描边 — 复用 SmallDoorBlockEntityRenderer 的变换链
     */
    public static void renderDoorFrameOutline(MatrixStack matrices, BufferBuilder buffer,
                                              Vec3d cameraPos, BlockPos lowerPos,
                                              DoorBlockEntity doorEntity,
                                              float r, float g, float b, float a) {
        matrices.push();

        // 1. 平移到方块位置（相机相对坐标）+ 渲染器的 translate(0.5, 1.5, 0.5)
        double dx = lowerPos.getX() + 0.5 - cameraPos.x;
        double dy = lowerPos.getY() + 1.5 - cameraPos.y;
        double dz = lowerPos.getZ() + 0.5 - cameraPos.z;
        matrices.translate(dx, dy, dz);

        // 2. 翻转 Y（与渲染器一致）
        matrices.scale(1, -1, 1);

        // 3. ModelPart pivot 偏移 (0, 24/16, 0) = (0, 1.5, 0)
        matrices.translate(0, 1.5, 0);

        // 4. 门的朝向旋转
        float yaw = doorEntity.getYaw();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));

        // 5. 开门滑动偏移
        if (doorEntity.isOpen()) {
            matrices.translate(DOOR_OPEN_SLIDE_OFFSET, 0, 0);
        }

        // 6. 绘制完整的 12 条边框线
        drawBox(matrices, buffer,
                DOOR_MIN_X, DOOR_MIN_Y, DOOR_MIN_Z,
                DOOR_MAX_X, DOOR_MAX_Y, DOOR_MAX_Z,
                r, g, b, a);

        matrices.pop();
    }

    /**
     * 绘制 box 的全部 12 条边（DEBUG_LINES + POSITION_COLOR 格式，无需法线）
     */
    public static void drawBox(MatrixStack matrices, BufferBuilder buffer,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float r, float g, float b, float a) {
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // 底面 4 条边
        line(buffer, mat, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buffer, mat, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buffer, mat, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buffer, mat, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // 顶面 4 条边
        line(buffer, mat, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buffer, mat, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buffer, mat, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buffer, mat, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // 竖直 4 条边
        line(buffer, mat, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buffer, mat, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buffer, mat, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buffer, mat, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        buffer.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(mat, x2, y2, z2).color(r, g, b, a);
    }

    /**
     * 定位门的下半部分位置
     */
    public static BlockPos getLowerDoorPos(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        if (state.getBlock() instanceof SmallDoorBlock) {
            if (state.contains(SmallDoorBlock.HALF)) {
                return state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            }
        }
        if (client.world.getBlockEntity(pos) instanceof SmallDoorBlockEntity) {
            return pos;
        }
        if (client.world.getBlockEntity(pos.down()) instanceof SmallDoorBlockEntity) {
            return pos.down();
        }
        return null;
    }

    // 门框在模型局部坐标下的包围盒（单位：block）
    // 模型 cuboid(-8, -32, -1) to (8, 0, 1) → ÷16 → (-0.5, -2, -0.0625) to (0.5, 0, 0.0625)
    public static final float DOOR_MIN_X = -0.5f;
    public static final float DOOR_MAX_X = 0.5f;
    public static final float DOOR_MIN_Y = -2.0f;
    public static final float DOOR_MAX_Y = 0.0f;
    public static final float DOOR_MIN_Z = -0.0625f;
    public static final float DOOR_MAX_Z = 0.0625f;

    // 开门动画的滑动距离（14 model units = 14/16 block）
    public static final float DOOR_OPEN_SLIDE_OFFSET = 14.0f / 16.0f;
}

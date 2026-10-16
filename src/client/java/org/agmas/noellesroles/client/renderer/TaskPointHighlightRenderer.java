package org.agmas.noellesroles.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.block.DrinkTrayBlock;
import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.client.taskpoint.TaskPointHighlightManager;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务点高亮描边渲染器：透视线框。
 * - 常驻门高亮：任何时刻主手持房间钥匙，就描边匹配的门框（白色，无需开启开关）
 * - 开关开启后：同时描边当前所有 EAT/DRINK 任务的目标点（餐盘橙色、饮品托盘蓝色）
 * 两个通道同帧可能同时存在；渲染前先预解析目标，目标为空时不触碰 GL 状态、不构建空 BufferBuilder。
 */
public class TaskPointHighlightRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("noellesroles-taskpoint");
    private static final float LINE_ALPHA = 0.9f;
    private static final float BLOCK_LINE_WIDTH = 3.0f;
    private static final float DOOR_LINE_WIDTH = 5.0f;
    // 吃任务（餐盘）橙色 / 喝任务（饮品托盘）蓝色
    private static final float FOOD_R = 1.0f;
    private static final float FOOD_G = 0.6f;
    private static final float FOOD_B = 0.0f;
    private static final float DRINK_R = 0.25f;
    private static final float DRINK_G = 0.55f;
    private static final float DRINK_B = 1.0f;

    // 餐盘模型轮廓：占满整格，高 2px（16px = 1 格）
    private static final float PLATTER_MIN_X = 0.0f;
    private static final float PLATTER_MAX_X = 1.0f;
    private static final float PLATTER_MIN_Z = 0.0f;
    private static final float PLATTER_MAX_Z = 1.0f;
    private static final float PLATTER_MAX_Y = 2.0f / 16.0f;
    // 饮品托盘模型轮廓：中间留边，高 3px
    private static final float TRAY_MIN_X = 2.0f / 16.0f;
    private static final float TRAY_MAX_X = 14.0f / 16.0f;
    private static final float TRAY_MIN_Z = 2.0f / 16.0f;
    private static final float TRAY_MAX_Z = 14.0f / 16.0f;
    private static final float TRAY_MAX_Y = 3.0f / 16.0f;

    private TaskPointHighlightRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(TaskPointHighlightRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        try {
            renderInternal(context);
        } catch (Exception e) {
            // 渲染钩子内的异常绝不能导致游戏崩溃
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.lineWidth(1.0f);
            LOGGER.error("Failed to render task point highlight", e);
        }
    }

    private static void renderInternal(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null || client.world == null) return;

        // 预解析：门通道（常驻）与方块通道（开关开启后的所有吃/喝任务点）
        List<DoorDraw> doors = new ArrayList<>();
        for (BlockPos pos : TaskPointHighlightManager.resolveDoorTargets(player)) {
            BlockPos lowerPos = OutlineBoxRenderUtil.getLowerDoorPos(client, pos);
            if (lowerPos == null) continue;
            BlockEntity be = client.world.getBlockEntity(lowerPos);
            if (be instanceof DoorBlockEntity doorEntity) {
                doors.add(new DoorDraw(lowerPos, doorEntity));
            }
        }
        List<BlockDraw> blocks = new ArrayList<>();
        if (TaskPointHighlightManager.isEnabled()) {
            for (BlockPos pos : TaskPointHighlightManager.resolveRenderTargets(player)) {
                Block block = client.world.getBlockState(pos).getBlock();
                if (block instanceof FoodPlatterBlock) {
                    blocks.add(new BlockDraw(pos, block instanceof DrinkTrayBlock));
                }
            }
        }
        if (doors.isEmpty() && blocks.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        // GL 状态：禁用深度测试（透视）、position_color shader、DEBUG_LINES
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        if (!doors.isEmpty()) {
            drawDoorFrames(matrices, cameraPos, doors);
        }
        if (!blocks.isEmpty()) {
            drawBlockBoxes(matrices, cameraPos, blocks);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private static void drawDoorFrames(MatrixStack matrices, Vec3d cameraPos, List<DoorDraw> doors) {
        RenderSystem.lineWidth(DOOR_LINE_WIDTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (DoorDraw door : doors) {
            OutlineBoxRenderUtil.renderDoorFrameOutline(matrices, buffer, cameraPos, door.lowerPos, door.doorEntity,
                    1.0f, 1.0f, 1.0f, LINE_ALPHA);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawBlockBoxes(MatrixStack matrices, Vec3d cameraPos, List<BlockDraw> blocks) {
        RenderSystem.lineWidth(BLOCK_LINE_WIDTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockDraw block : blocks) {
            float minX = block.tray ? TRAY_MIN_X : PLATTER_MIN_X;
            float maxX = block.tray ? TRAY_MAX_X : PLATTER_MAX_X;
            float minZ = block.tray ? TRAY_MIN_Z : PLATTER_MIN_Z;
            float maxZ = block.tray ? TRAY_MAX_Z : PLATTER_MAX_Z;
            float maxY = block.tray ? TRAY_MAX_Y : PLATTER_MAX_Y;
            float r = block.tray ? DRINK_R : FOOD_R;
            float g = block.tray ? DRINK_G : FOOD_G;
            float b = block.tray ? DRINK_B : FOOD_B;
            matrices.push();
            matrices.translate(block.pos.getX() - cameraPos.x, block.pos.getY() - cameraPos.y, block.pos.getZ() - cameraPos.z);
            OutlineBoxRenderUtil.drawBox(matrices, buffer,
                    minX, 0.0f, minZ, maxX, maxY, maxZ,
                    r, g, b, LINE_ALPHA);
            matrices.pop();
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private record DoorDraw(BlockPos lowerPos, DoorBlockEntity doorEntity) {
    }

    private record BlockDraw(BlockPos pos, boolean tray) {
    }
}

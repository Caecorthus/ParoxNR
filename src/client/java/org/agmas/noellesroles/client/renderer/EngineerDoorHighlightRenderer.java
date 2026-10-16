package org.agmas.noellesroles.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 工程师门高亮描边渲染器
 * 当门被撬/被堵时，在客户端渲染5秒红色透视描边。
 *
 * 使用 DEBUG_LINES + POSITION_COLOR 格式，不依赖 rendertype_lines shader 的法线展开，
 * 确保所有 12 条边都可靠渲染。具体描边绘制复用 OutlineBoxRenderUtil。
 */
public class EngineerDoorHighlightRenderer {

    private static final int HIGHLIGHT_DURATION_TICKS = 100; // 5 seconds
    private static final float RED = 1.0f;
    private static final float GREEN = 0.19f;
    private static final float BLUE = 0.19f;
    private static final float ALPHA = 1.0f;

    private static final List<HighlightEntry> highlightEntries = new ArrayList<>();

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EngineerDoorHighlightRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> clientTick());
    }

    public static void onPacketReceived(BlockPos pos) {
        for (HighlightEntry entry : highlightEntries) {
            if (entry.pos.equals(pos)) {
                entry.remainingTicks = HIGHLIGHT_DURATION_TICKS;
                return;
            }
        }
        highlightEntries.add(new HighlightEntry(pos, HIGHLIGHT_DURATION_TICKS));
    }

    private static void clientTick() {
        Iterator<HighlightEntry> iter = highlightEntries.iterator();
        while (iter.hasNext()) {
            HighlightEntry entry = iter.next();
            entry.remainingTicks--;
            if (entry.remainingTicks <= 0) {
                iter.remove();
            }
        }
    }

    public static void clear() {
        highlightEntries.clear();
    }

    private static void render(WorldRenderContext context) {
        if (highlightEntries.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        // GL 状态：禁用深度测试（透视）、position_color shader、DEBUG_LINES
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(5.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        boolean hasVertices = false;

        for (HighlightEntry entry : highlightEntries) {
            BlockPos pos = entry.pos;

            BlockPos lowerPos = OutlineBoxRenderUtil.getLowerDoorPos(client, pos);
            if (lowerPos == null) continue;

            BlockEntity be = client.world.getBlockEntity(lowerPos);
            if (!(be instanceof DoorBlockEntity doorEntity)) continue;

            OutlineBoxRenderUtil.renderDoorFrameOutline(matrices, buffer, cameraPos, lowerPos, doorEntity,
                    RED, GREEN, BLUE, ALPHA);
            hasVertices = true;
        }

        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } else {
            buffer.end().close();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private static class HighlightEntry {
        final BlockPos pos;
        int remainingTicks;

        HighlightEntry(BlockPos pos, int remainingTicks) {
            this.pos = pos;
            this.remainingTicks = remainingTicks;
        }
    }
}

package org.agmas.noellesroles.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.util.RecallerClientHelper;
import org.agmas.noellesroles.recaller.RecallerAnchorEntity;

/**
 * 回溯者【锚点】渲染器：旋转全亮锚点物品模型 + 名牌（模板：BreacherMarkerEntityRenderer）。
 * 按观看者门控：仅碎片持有者渲染；非持有者完全看不见（无模型、无名牌，描边由
 * GetInstinctHighlight 监听另行门控）。远距离可见由实体的 shouldRender(double) 旁路保证。
 */
@Environment(EnvType.CLIENT)
public class RecallerAnchorEntityRenderer extends EntityRenderer<RecallerAnchorEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack anchor = new ItemStack(ModItems.RECALLER_ANCHOR);

    public RecallerAnchorEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RecallerAnchorEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0D, 0.25D, 0.0D);
        float rotation = (entity.age + tickDelta) * 3.0F;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.scale(1.4F, 1.4F, 1.4F);

        BakedModel model = this.itemRenderer.getModel(this.anchor, entity.getWorld(), null, entity.getId());
        // 全亮度，保证锚点在黑暗处仍清晰可见
        this.itemRenderer.renderItem(
                this.anchor, ModelTransformationMode.GROUND, false,
                matrices, vertexConsumers, 15728880,
                OverlayTexture.DEFAULT_UV, model
        );
        matrices.pop();

        // super.render 负责绘制名牌（锚点）——随本渲染器门控，只有持有者看得到
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public boolean shouldRender(RecallerAnchorEntity entity, Frustum frustum, double x, double y, double z) {
        ClientPlayerEntity local = MinecraftClient.getInstance().player;
        if (local == null || !RecallerClientHelper.canSeeAnchor(local)) return false;
        return frustum.isVisible(entity.getBoundingBox());
    }

    @Override
    public Identifier getTexture(RecallerAnchorEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}

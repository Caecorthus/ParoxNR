package org.agmas.noellesroles.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.breacher.BreacherMarkerEntity;

@Environment(EnvType.CLIENT)
public class BreacherMarkerEntityRenderer extends EntityRenderer<BreacherMarkerEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);

    public BreacherMarkerEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BreacherMarkerEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0D, 0.25D, 0.0D);
        float rotation = (entity.age + tickDelta) * 3.0F;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.scale(1.4F, 1.4F, 1.4F);

        BakedModel model = this.itemRenderer.getModel(this.pickaxe, entity.getWorld(), null, entity.getId());
        // 全亮度，保证破坏点在黑暗/损坏电灯下仍清晰可见
        this.itemRenderer.renderItem(
                this.pickaxe, ModelTransformationMode.GROUND, false,
                matrices, vertexConsumers, 15728880,
                OverlayTexture.DEFAULT_UV, model
        );
        matrices.pop();

        // super.render 负责绘制发光名牌（破坏点）
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(BreacherMarkerEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}

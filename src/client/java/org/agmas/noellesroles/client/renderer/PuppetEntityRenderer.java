package org.agmas.noellesroles.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.agmas.noellesroles.entity.PuppetEntity;

/**
 * 站立的傀儡 / 本体替身：用玩家模型渲染皮肤 UUID 对应玩家的皮肤，按皮肤类型切换宽/窄手臂模型。
 */
public class PuppetEntityRenderer extends LivingEntityRenderer<PuppetEntity, PlayerEntityModel<PuppetEntity>> {
    private final PlayerEntityModel<PuppetEntity> wideModel;
    private final PlayerEntityModel<PuppetEntity> slimModel;

    public PuppetEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(PuppetEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        SkinTextures skin = PuppeteerClient.getSkinTextures(entity.getSkinUuid());
        this.model = skin.model() == SkinTextures.Model.SLIM ? this.slimModel : this.wideModel;
        this.model.setVisible(true);
        this.model.sneaking = false;
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(PuppetEntity entity) {
        return PuppeteerClient.getSkinTextures(entity.getSkinUuid()).texture();
    }

    // 与玩家渲染器相同的整体缩放
    @Override
    protected void scale(PuppetEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected boolean hasLabel(PuppetEntity entity) {
        return false;
    }
}

package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import org.agmas.noellesroles.client.morphling.ClientForgedBodyVisualHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 赝造药水：伪装皮肤为纤细(Slim)模型时，把尸体也切换到瘦模型渲染器，避免手臂贴图错位。
 * priority 低于默认值，保证在 Wathe 的 EntityRendererDispatchMixin（同样注入 getRenderer）
 * 之后应用，这样 RETURN 注入才能覆盖它 cancel 时产生的 return。
 */
@SuppressWarnings("unchecked")
@Mixin(value = EntityRenderDispatcher.class, priority = 900)
public class ForgedBodyModelDispatchMixin {
    @Unique
    private Map<SkinTextures.Model, EntityRenderer<? extends PlayerBodyEntity>> noellesroles$forgedBodyRenderers = Map.of();

    @Inject(method = "reload", at = @At("TAIL"))
    private void noellesroles$reloadForgedBodyRenderers(ResourceManager manager, CallbackInfo ci,
            @Local EntityRendererFactory.Context context) {
        Map<SkinTextures.Model, EntityRenderer<? extends PlayerBodyEntity>> renderers = new EnumMap<>(SkinTextures.Model.class);
        renderers.put(SkinTextures.Model.WIDE, new PlayerBodyEntityRenderer<>(context, false));
        renderers.put(SkinTextures.Model.SLIM, new PlayerBodyEntityRenderer<>(context, true));
        this.noellesroles$forgedBodyRenderers = renderers;
    }

    @Inject(method = "getRenderer", at = @At("RETURN"), cancellable = true)
    private <T extends Entity> void noellesroles$swapForgedBodyModel(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (!(entity instanceof PlayerBodyEntity body)) {
            return;
        }
        SkinTextures forgedTextures = ClientForgedBodyVisualHelper.getForgedSkinTextures(body);
        if (forgedTextures == null) {
            return;
        }
        EntityRenderer<? extends PlayerBodyEntity> renderer = this.noellesroles$forgedBodyRenderers.get(forgedTextures.model());
        if (renderer != null) {
            cir.setReturnValue((EntityRenderer<? super T>) renderer);
        }
    }
}

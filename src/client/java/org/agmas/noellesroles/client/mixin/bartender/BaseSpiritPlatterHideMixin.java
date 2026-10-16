package org.agmas.noellesroles.client.mixin.bartender;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.render.block_entity.PlateBlockEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * 餐盘 / 饮品托盘上的基酒（含调制酒）对其他玩家不可见，酒保本人仍可看到自己放上去的酒。
 */
@Mixin(PlateBlockEntityRenderer.class)
public class BaseSpiritPlatterHideMixin {

    @ModifyExpressionValue(method = {"renderFood", "renderDrinks"},
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/block_entity/BeveragePlateBlockEntity;getStoredItems()Ljava/util/List;"))
    private List<ItemStack> noellesroles$hideBaseSpiritsFromOthers(List<ItemStack> original) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null && localPlayer.getWorld() != null
                && GameWorldComponent.KEY.get(localPlayer.getWorld()).isRole(localPlayer, Noellesroles.BARTENDER)) {
            return original;
        }
        return original.stream().filter(stack -> !stack.isOf(ModItems.BASE_SPIRIT)).toList();
    }
}

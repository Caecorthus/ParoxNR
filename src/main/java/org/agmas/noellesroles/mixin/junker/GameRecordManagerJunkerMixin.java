package org.agmas.noellesroles.mixin.junker;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.junker.JunkerLootDestroyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wathe 只在成功使用的路径上调用 recordItemUse，
 * 借此判定拾荒者搜刮所得物品的一次性销毁。
 */
@Mixin(GameRecordManager.class)
public abstract class GameRecordManagerJunkerMixin {

    @Inject(method = "recordItemUse", at = @At("HEAD"))
    private static void noellesroles$destroySearchedLootOnUse(ServerPlayerEntity player, Identifier itemId,
                                                             ServerPlayerEntity target, NbtCompound extra,
                                                             CallbackInfo ci) {
        if (player == null || itemId == null) {
            return;
        }
        Item item = Registries.ITEM.get(itemId);
        JunkerLootDestroyHandler.onItemUsed(player, item);
    }
}

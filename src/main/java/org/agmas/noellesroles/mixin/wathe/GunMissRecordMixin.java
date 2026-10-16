package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 枪械开火未命中的回放事件（左轮、德林加、双管猎枪）。
 * Wathe 只在开火成功路径上调用 recordItemUse（冷却中、空仓等被拦截的点击不会到达），
 * 左轮/德林加在该调用点 target 为空即未命中；双管猎枪的装填路径 target 同样为空，仅取 action=fire。
 */
@Mixin(GameRecordManager.class)
public abstract class GunMissRecordMixin {

    @Inject(method = "recordItemUse", at = @At("HEAD"))
    private static void noellesroles$recordGunMiss(ServerPlayerEntity player, Identifier itemId,
                                                   ServerPlayerEntity target, NbtCompound extra,
                                                   CallbackInfo ci) {
        if (player == null || itemId == null || target != null) {
            return;
        }

        boolean missedShot;
        if (itemId.equals(Registries.ITEM.getId(WatheItems.REVOLVER))
                || itemId.equals(Registries.ITEM.getId(WatheItems.DERRINGER))) {
            missedShot = true;
        } else if (itemId.equals(Registries.ITEM.getId(ModItems.DOUBLE_BARREL_SHOTGUN))) {
            missedShot = extra != null && "fire".equals(extra.getString("action"));
        } else {
            return;
        }

        GameRecordManager.event("gun_miss").actor(player).put("item", itemId.toString()).record();
    }
}

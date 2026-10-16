package org.agmas.noellesroles.mixin.taotie;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 被吞玩家不是真正的旁观者（保持原游戏模式 + 摄像机跟随饕餮），
 * 所以其实体仍真实存在于世界中，且被每 tick 传送并钉在与饕餮完全重叠的位置。
 * 只要它还带着正常大小的包围盒，饕餮周围的攻击/交互/碰撞判定就可能命中它：
 * 例如 vanilla 的跨准星实体射线检测，对"射线起点位于实体包围盒内"的目标会以
 * 距离 0 强制选中它（饕餮的摄像机眼点始终在被吞者盒内），
 * 于是这次攻击/方块交互被整体吞掉 —— 表现为饕餮偶发被打不掉血、点不了门和方块。
 *
 * 这里把被吞实体的包围盒平移到远离对局的高空（Y + 4096），使其永远不可能被任何
 * 盒查询、射线或碰撞判定命中；实体的 X/Y/Z 坐标、摄像机跟随和游戏模式不受影响，
 * 释放/重置后 isSwallowed=false 自动恢复正常包围盒。
 */
@Mixin(Entity.class)
public class SwallowedPlayerBoundingBoxMixin {

    private static final double GHOST_BOX_OFFSET_Y = 4096.0;

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    private void noellesroles$relocateSwallowedBoundingBox(CallbackInfoReturnable<Box> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof PlayerEntity player && SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            double x = entity.getX();
            double z = entity.getZ();
            double ghostY = entity.getY() + GHOST_BOX_OFFSET_Y;
            cir.setReturnValue(new Box(x - 0.3, ghostY, z - 0.3, x + 0.3, ghostY + 1.8, z + 0.3));
        }
    }
}

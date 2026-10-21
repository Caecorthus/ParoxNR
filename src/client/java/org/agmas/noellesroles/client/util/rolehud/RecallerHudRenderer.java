package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.client.util.RecallerClientHelper;
import org.agmas.noellesroles.recaller.RecallerAnchorWorldComponent;

/**
 * 回溯者 HUD：右下角单行提示。提示行构建抽为共享静态方法 {@link #buildHintLine}，
 * 绘制端（RecallerHudMixin）与堆叠测量端（本类 getTopY）都调用它，杜绝两文件漂移。
 * HUD 只对回溯者本人显示；捡到碎片的其他玩家靠物品 tooltip 获知规则（设计如此）。
 */
public final class RecallerHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        return HudRenderHelper.stackLine(bottom, renderer, buildHintLine(player), 0);
    }

    /**
     * 提示行优先级（读客户端同步的锚点世界组件 + 本地背包）：
     * 1. 持有锚点物品 → 放置提示；
     * 2. 否则有碎片 → 有锚点 ? 传送提示 : 无锚点提示；
     * 3. 否则锚点在场 → 回收提示（回收权属于回溯者职业，本 HUD 本就只对回溯者显示）；
     * 4. 否则 → 购买提示。
     */
    public static Text buildHintLine(PlayerEntity player) {
        RecallerAnchorWorldComponent comp = RecallerAnchorWorldComponent.KEY.get(player.getWorld());
        if (RecallerClientHelper.holdsAnchorItem(player)) {
            return Text.translatable("tip.recaller.hud.place");
        }
        if (RecallerClientHelper.holdsRewindShard(player)) {
            return Text.translatable(comp.isPlaced()
                    ? "tip.recaller.hud.shard_ready" : "tip.recaller.hud.shard_no_anchor");
        }
        if (comp.isPlaced()) {
            return Text.translatable("tip.recaller.hud.placed");
        }
        return Text.translatable("tip.recaller.hud.buy");
    }
}

package org.agmas.noellesroles.client.phonism;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.client.screen.RoleScreenHelper.InteractionBlocker;
import org.agmas.noellesroles.client.widget.MenuPlayerTargetWidget;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

// 幻听目标格子：生死玩家一视同仁，不做任何区分
public class PhonismTargetWidget extends MenuPlayerTargetWidget {
    private static final Text UNKNOWN_PLAYER_TEXT = Text.translatable("screen.role_target.unknown_player");

    public final UUID targetUuid;

    public PhonismTargetWidget(int x, int y, @NotNull UUID targetUuid, Consumer<UUID> onTargetSelected,
                               int clipLeft, int clipTop, int clipRight, int clipBottom,
                               InteractionBlocker... blockers) {
        super(x, y, RoleScreenHelper.getPlayerName(targetUuid, UNKNOWN_PLAYER_TEXT),
                button -> onTargetSelected.accept(targetUuid),
                clipLeft, clipTop, clipRight, clipBottom, blockers);
        this.targetUuid = targetUuid;
    }

    @Override
    protected SkinTextures getSkinTextures() {
        return RoleScreenHelper.getPlayerSkinTextures(this.targetUuid);
    }

    @Override
    protected ShopEntry.Type getBackgroundType() {
        return ShopEntry.Type.TOOL;
    }

    @Override
    protected Text getOverlayText() {
        return RoleScreenHelper.getPlayerName(this.targetUuid, UNKNOWN_PLAYER_TEXT);
    }
}

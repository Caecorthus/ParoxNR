package org.agmas.noellesroles.client.util.rolehud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.junker.JunkerLootHelper;
import org.agmas.noellesroles.junker.JunkerPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

public final class JunkerHudRenderer implements RoleHudRenderer {
    @Override
    public int getTopY(TextRenderer renderer, ClientPlayerEntity player, int bottom) {
        if (SwallowedPlayerComponent.isPlayerSwallowed(player)) return bottom;
        return HudRenderHelper.stackLine(bottom, renderer, getLine(player), 0);
    }

    public static Text getLine(ClientPlayerEntity player) {
        JunkerPlayerComponent junker = JunkerPlayerComponent.KEY.get(player);
        if (junker.isSearching()) {
            return Text.translatable("tip.junker.searching", Math.round(junker.getSearchProgress() * 100.0F));
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.getCooldown() > 0) {
            return Text.translatable("tip.noellesroles.cooldown", ability.getCooldown() / 20);
        }

        ItemStack blockingLoot = JunkerLootHelper.findFirstBlockingLoot(player);
        if (blockingLoot != null) {
            return Text.translatable("tip.junker.blocked",
                    blockingLoot.getName().copy().formatted(Formatting.YELLOW));
        }

        if (!JunkerLootHelper.hasEmptyHotbarSlot(player)) {
            return Text.translatable("tip.junker.hotbar_full");
        }

        return Text.translatable("tip.junker.ready", HudRenderHelper.getAbilityKeyName());
    }
}

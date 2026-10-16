package org.agmas.noellesroles.junker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 拾荒者「搜刮尸体」的服务端校验与结算。
 */
public final class JunkerSearchHelper {
    public static final double SEARCH_RANGE = 3.0D;

    private JunkerSearchHelper() {}

    public static void handleSearchRequest(ServerPlayerEntity player, UUID bodyEntityUuid) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.JUNKER)
                || !GameFunctions.isPlayerPlayingAndAlive(player)
                || SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return;
        }
        if (AbilityPlayerComponent.KEY.get(player).getCooldown() > 0) {
            return;
        }
        JunkerPlayerComponent junkerComponent = JunkerPlayerComponent.KEY.get(player);
        if (junkerComponent.isSearching()) {
            return;
        }

        ItemStack blockingLoot = JunkerLootHelper.findFirstBlockingLoot(player);
        if (blockingLoot != null) {
            sendActionBar(player, Text.translatable("tip.junker.blocked",
                    blockingLoot.getName().copy().formatted(Formatting.YELLOW)));
            return;
        }
        if (!JunkerLootHelper.hasEmptyHotbarSlot(player)) {
            sendActionBar(player, Text.translatable("tip.junker.hotbar_full"));
            return;
        }

        PlayerBodyEntity body = findBody(player, bodyEntityUuid);
        if (body == null) {
            return;
        }
        UUID deadPlayerUuid = body.getPlayerUuid();
        if (deadPlayerUuid == null) {
            return;
        }

        JunkerLootWorldComponent lootComponent = JunkerLootWorldComponent.KEY.get(player.getServerWorld());
        if (lootComponent.wasSearched(deadPlayerUuid)) {
            sendActionBar(player, Text.translatable("tip.junker.searched"));
            return;
        }
        if (!lootComponent.hasLoot(deadPlayerUuid)) {
            lootComponent.markSearched(deadPlayerUuid);
            sendActionBar(player, Text.translatable("tip.junker.empty"));
            return;
        }

        junkerComponent.startSearch(bodyEntityUuid);
    }

    /** 注视满5秒后的结算；结算前复查尸体是否已被他人搜过。 */
    public static void completeSearch(ServerPlayerEntity player, UUID bodyEntityUuid) {
        ServerWorld world = player.getServerWorld();
        Entity entity = world.getEntity(bodyEntityUuid);
        UUID deadPlayerUuid = entity instanceof PlayerBodyEntity body ? body.getPlayerUuid() : null;
        if (deadPlayerUuid == null) {
            return;
        }

        JunkerLootWorldComponent lootComponent = JunkerLootWorldComponent.KEY.get(world);
        if (lootComponent.wasSearched(deadPlayerUuid)) {
            sendActionBar(player, Text.translatable("tip.junker.searched"));
            return;
        }

        ItemStack reward = lootComponent.randomLoot(deadPlayerUuid, player.getRandom());
        if (reward == null) {
            lootComponent.markSearched(deadPlayerUuid);
            sendActionBar(player, Text.translatable("tip.junker.empty"));
            return;
        }

        JunkerLootHelper.mark(reward);
        // giveItemStack 入包时会清空传入的 ItemStack，先取好物品与名字
        Item rewardItem = reward.getItem();
        Text rewardName = reward.getName();
        int hotbarSlot = JunkerLootHelper.findEmptyHotbarSlot(player);
        if (hotbarSlot >= 0) {
            player.getInventory().setStack(hotbarSlot, reward);
        } else {
            player.giveItemStack(reward);
        }

        lootComponent.markSearched(deadPlayerUuid);
        AbilityPlayerComponent.KEY.get(player).setCooldown(GameConstants.getInTicks(1, 0));
        // 搜刮成功不播音效：拾取音会暴露有人正在搜尸
        sendActionBar(player, Text.translatable("tip.junker.found", rewardName));

        // 回放记录：拾荒者从谁的尸体上搜到了什么
        NbtCompound extra = new NbtCompound();
        extra.putString("action", "search");
        extra.putString("item", Registries.ITEM.getId(rewardItem).toString());
        extra.putString("item_name", Text.Serialization.toJsonString(rewardName, player.getRegistryManager()));
        extra.putUuid("victim", deadPlayerUuid);
        ServerPlayerEntity recordTarget = world.getPlayerByUuid(deadPlayerUuid) instanceof ServerPlayerEntity serverVictim
                ? serverVictim : null;
        GameRecordManager.recordSkillUse(player, Noellesroles.JUNKER_ID, recordTarget, extra);
    }

    @Nullable
    private static PlayerBodyEntity findBody(ServerPlayerEntity player, UUID bodyEntityUuid) {
        var hitResult = BodyTargetHelper.raycastBody(player, SEARCH_RANGE,
                body -> body.getUuid().equals(bodyEntityUuid));
        return hitResult != null && hitResult.getEntity() instanceof PlayerBodyEntity body ? body : null;
    }

    private static void sendActionBar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, true);
    }
}

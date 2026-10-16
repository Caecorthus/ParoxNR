package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.morphling.BodyForgeryWorldComponent;
import org.agmas.noellesroles.packet.OpenForgedPotionSelectS2CPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** 赝造药水：右键打开选人界面指定目标；对尸体使用把尸体皮肤伪装成指定玩家。一次性消耗。 */
public class ForgedPotionItem extends Item {
    // 与 wathe 商店物品描述一致的灰色（WatheItemTooltips.REGULAR_TOOLTIP_COLOR）
    private static final int TOOLTIP_COLOR = 0x808080;
    private static final String TARGET_KEY = "forged_target";
    private static final String TARGET_NAME_KEY = "forged_target_name";
    private static final double APPLY_RANGE_SQ = BodyTargetHelper.SKILL_RANGE * BodyTargetHelper.SKILL_RANGE;

    public ForgedPotionItem(Settings settings) {
        super(settings);
    }

    public static @Nullable UUID getTarget(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        NbtCompound nbt = customData.copyNbt();
        return nbt.containsUuid(TARGET_KEY) ? nbt.getUuid(TARGET_KEY) : null;
    }

    public static String getTargetName(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return "";
        }
        return customData.copyNbt().getString(TARGET_NAME_KEY);
    }

    public static void setTarget(ItemStack stack, UUID targetUuid, String targetName) {
        NbtCompound nbt = new NbtCompound();
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            nbt = customData.copyNbt();
        }
        nbt.putUuid(TARGET_KEY, targetUuid);
        nbt.putString(TARGET_NAME_KEY, targetName);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity serverUser)) {
            return TypedActionResult.pass(stack);
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(user, Noellesroles.MORPHLING)
                || !GameFunctions.isPlayerPlayingAndAlive(user)
                || SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            return TypedActionResult.pass(stack);
        }
        ServerPlayNetworking.send(serverUser, new OpenForgedPotionSelectS2CPacket());
        return TypedActionResult.success(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerBodyEntity body)) {
            return ActionResult.PASS;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(user.getWorld());
        if (!gameWorld.isRole(user, Noellesroles.MORPHLING)) {
            return ActionResult.PASS;
        }
        // 客户端先认领这次点击：服务端返回 PASS 时原版 doItemUse 会落到 use() 误开选人界面
        if (user.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity serverUser)
                || !GameFunctions.isPlayerPlayingAndAlive(user)
                || SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            return ActionResult.PASS;
        }

        UUID targetUuid = getTarget(stack);
        if (targetUuid == null) {
            serverUser.sendMessage(Text.translatable("tip.noellesroles.forged_potion.no_target"), true);
            return ActionResult.SUCCESS;
        }
        if (!BodyTargetHelper.canPlayerSeeBody(user, body)
                || user.squaredDistanceTo(body) > APPLY_RANGE_SQ) {
            return ActionResult.PASS;
        }

        UUID victimUuid = body.getPlayerUuid();
        // 先读出名字：消耗后药水堆变空，NBT 会一起丢失
        String targetName = getTargetName(stack);
        BodyForgeryWorldComponent.KEY.get(user.getWorld()).setForgedSkin(body.getUuid(), targetUuid);
        user.getWorld().playSound(null, body.getX(), body.getY() + 0.1, body.getZ(),
                SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 0.8F, 1.0F);
        stack.decrementUnlessCreative(1, user);
        user.getInventory().markDirty();
        serverUser.playerScreenHandler.sendContentUpdates();
        serverUser.sendMessage(Text.translatable("tip.noellesroles.forged_potion.applied", targetName), true);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "forge_body");
        extra.putUuid("victim", victimUuid);
        extra.putUuid("forged_as", targetUuid);
        GameRecordManager.recordItemUse(serverUser, Registries.ITEM.getId(ModItems.FORGED_POTION), null, extra);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : Text.translatable("item.noellesroles.forged_potion.tooltip").getString().split("\\R")) {
            // 与 wathe 商店物品描述一致的灰色（WatheItemTooltips.REGULAR_TOOLTIP_COLOR）
            tooltip.add(Text.literal(line).withColor(TOOLTIP_COLOR));
        }
        if (getTarget(stack) == null) {
            tooltip.add(Text.translatable("item.noellesroles.forged_potion.no_target").withColor(TOOLTIP_COLOR));
        } else {
            tooltip.add(Text.translatable("item.noellesroles.forged_potion.target", getTargetName(stack))
                    .withColor(TOOLTIP_COLOR));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

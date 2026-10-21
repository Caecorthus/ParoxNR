package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.mengpo.MengPoHelper;
import org.agmas.noellesroles.mengpo.MengPoSoupPlayerComponent;
import org.agmas.noellesroles.saint.SaintHelper;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;

import java.util.List;
import java.util.UUID;

/**
 * 孟婆汤。按来源分三种，限制写在物品数据里，掉落/被捡走后依然有效：
 * <ul>
 *     <li>渡己：只有原主人能自己喝；</li>
 *     <li>渡人：只能喂给别人，且不能喂回给发汤的孟婆；</li>
 *     <li>购买：既能自己喝，也能喂给别人。</li>
 * </ul>
 * 喂给别人时对方必须处于潜行（视为同意），蓄力 1.5 秒，3 格内有效。
 */
public class MengPoSoupItem extends Item {
    private static final int USE_TIME = 30;
    private static final double MAX_DISTANCE_SQ = 9.0;

    private static final String KIND_KEY = "mengpoSoupKind";
    private static final String OWNER_KEY = "mengpoSoupOwner";
    private static final String TARGET_KEY = "mengpoSoupTarget";
    private static final String FEEDING_KEY = "mengpoSoupFeeding";

    public enum Kind { SELF, DUREN, BOUGHT }

    public MengPoSoupItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(Kind kind, UUID owner) {
        ItemStack stack = new ItemStack(ModItems.MENGPO_SOUP);
        NbtCompound nbt = new NbtCompound();
        nbt.putString(KIND_KEY, kind.name());
        if (owner != null) {
            nbt.putUuid(OWNER_KEY, owner);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return stack;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(user) || !GameFunctions.isPlayerAliveAndSurvival(target)
                || SwallowedInteractionHelper.blocksPlayerTarget(user) || SwallowedInteractionHelper.blocksPlayerTarget(target)) {
            return ActionResult.PASS;
        }

        if (isKarmaLocked(user)) {
            return ActionResult.CONSUME;
        }

        // 拒绝时返回 CONSUME 而非 FAIL：FAIL 不算已处理，客户端会接着走 use()，把喂汤变成自己喝
        Kind kind = getKind(stack);
        if (kind == Kind.SELF) {
            tip(user, "tip.mengpo.soup.self_only");
            return ActionResult.CONSUME;
        }
        if (kind == Kind.DUREN && target.getUuid().equals(getOwner(stack))) {
            tip(user, "tip.mengpo.soup.not_mengpo");
            return ActionResult.CONSUME;
        }
        if (!target.isSneaking()) {
            tip(user, "tip.mengpo.soup.need_sneak");
            return ActionResult.CONSUME;
        }

        setTarget(stack, target.getUuid(), true);
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isUsingItem() || !GameFunctions.isPlayerAliveAndSurvival(user) || SwallowedInteractionHelper.blocksPlayerTarget(user)) {
            return TypedActionResult.pass(stack);
        }
        if (isKarmaLocked(user)) {
            return TypedActionResult.fail(stack);
        }

        Kind kind = getKind(stack);
        if (kind == Kind.DUREN) {
            tip(user, "tip.mengpo.soup.others_only");
            return TypedActionResult.fail(stack);
        }
        if (kind == Kind.SELF && !user.getUuid().equals(getOwner(stack))) {
            tip(user, "tip.mengpo.soup.not_owner");
            return TypedActionResult.fail(stack);
        }
        if (MengPoSoupPlayerComponent.isActive(user)) {
            tip(user, "tip.mengpo.soup.self_already");
            return TypedActionResult.fail(stack);
        }

        setTarget(stack, user.getUuid(), false);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !isFeeding(stack)) {
            return;
        }
        UUID targetUuid = getTarget(stack);
        PlayerEntity target = targetUuid == null ? null : world.getPlayerByUuid(targetUuid);
        if (target == null || user.squaredDistanceTo(target) > MAX_DISTANCE_SQ
                || !GameFunctions.isPlayerAliveAndSurvival(target) || !target.isSneaking()) {
            user.stopUsingItem();
            clearTarget(stack);
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            return stack;
        }
        UUID targetUuid = getTarget(stack);
        boolean feeding = isFeeding(stack);
        clearTarget(stack);
        if (targetUuid == null) {
            return stack;
        }

        if (!(world.getPlayerByUuid(targetUuid) instanceof ServerPlayerEntity target)
                || !GameFunctions.isPlayerPlayingAndAlive(target)) {
            return stack;
        }
        if (feeding && (player.squaredDistanceTo(target) > MAX_DISTANCE_SQ || !target.isSneaking())) {
            return stack;
        }
        if (MengPoSoupPlayerComponent.isActive(target)) {
            tip(player, feeding ? "tip.mengpo.soup.target_already" : "tip.mengpo.soup.self_already");
            return stack;
        }

        MengPoHelper.applySoup(player, target);
        stack.decrement(1);
        return stack;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        clearTarget(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TIME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return isFeeding(stack) ? UseAction.SPEAR : UseAction.DRINK;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Kind kind = getKind(stack);
        tooltip.add(Text.translatable("item.noellesroles.mengpo_soup.tooltip." + kind.name().toLowerCase()));
        tooltip.add(Text.translatable("item.noellesroles.mengpo_soup.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    /** 圣徒业力锁：SaintItemUseMixin 拦在 Item 基类上，本物品重写了 use/useOnEntity，需要自行检查。 */
    private static boolean isKarmaLocked(PlayerEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayer && SaintPlayerComponent.KEY.get(serverPlayer).isKarmaLocked()) {
            SaintHelper.sendKarmaLockedMessage(serverPlayer);
            return true;
        }
        return false;
    }

    private static void tip(PlayerEntity player, String key) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable(key), true);
        }
    }

    private static NbtCompound readNbt(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null ? customData.copyNbt() : new NbtCompound();
    }

    public static Kind getKind(ItemStack stack) {
        NbtCompound nbt = readNbt(stack);
        if (nbt.contains(KIND_KEY)) {
            try {
                return Kind.valueOf(nbt.getString(KIND_KEY));
            } catch (IllegalArgumentException ignored) {
            }
        }
        // 创造栏/指令直接拿到的汤没有来源数据，按购买的汤处理
        return Kind.BOUGHT;
    }

    private static UUID getOwner(ItemStack stack) {
        NbtCompound nbt = readNbt(stack);
        return nbt.containsUuid(OWNER_KEY) ? nbt.getUuid(OWNER_KEY) : null;
    }

    private static UUID getTarget(ItemStack stack) {
        NbtCompound nbt = readNbt(stack);
        return nbt.containsUuid(TARGET_KEY) ? nbt.getUuid(TARGET_KEY) : null;
    }

    private static boolean isFeeding(ItemStack stack) {
        return readNbt(stack).getBoolean(FEEDING_KEY);
    }

    private static void setTarget(ItemStack stack, UUID target, boolean feeding) {
        NbtCompound nbt = readNbt(stack);
        nbt.putUuid(TARGET_KEY, target);
        nbt.putBoolean(FEEDING_KEY, feeding);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    // 只清本次使用的临时目标，保留来源与绑定数据
    private static void clearTarget(ItemStack stack) {
        NbtCompound nbt = readNbt(stack);
        if (!nbt.contains(TARGET_KEY) && !nbt.contains(FEEDING_KEY)) {
            return;
        }
        nbt.remove(TARGET_KEY);
        nbt.remove(FEEDING_KEY);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}

package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.hallucination.HallucinationDummyInteractionHelper;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;
import org.agmas.noellesroles.toxicologist.AntidoteShieldPlayerComponent;

import java.util.Optional;
import java.util.UUID;

public class AntidoteItem extends Item {
    // Charge time: 1.5 seconds = 30 ticks
    private static final int USE_TIME = 30;
    // Cooldown after use: 2 minutes = 2 * 60 * 20 ticks
    public static final int COOLDOWN_TICKS = 2 * 60 * 20;
    // Initial cooldown at game start: 2 minutes = 2 * 60 * 20 ticks
    public static final int INITIAL_COOLDOWN_TICKS = COOLDOWN_TICKS;
    // Max distance to target: 3 blocks
    private static final double MAX_DISTANCE_SQ = 9.0;
    // Tag marking a preemptive use (target is not poisoned yet)
    private static final String SHIELD_TAG = "shield";

    public AntidoteItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.FAIL;
        }

        // Target must be a player
        HallucinationPlayerComponent hallucinationComponent = HallucinationPlayerComponent.KEY.get(user);
        Optional<UUID> dummyId = HallucinationDummyInteractionHelper.getDummyId(entity, user);
        if (dummyId.isPresent()) {
            if (hallucinationComponent.getDummyPoisonState(dummyId.get()).isEmpty()) {
                return ActionResult.PASS;
            }
            NbtCompound nbt = new NbtCompound();
            nbt.putUuid("hallucinationDummy", dummyId.get());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            user.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }

        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        // Target must be alive
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            return ActionResult.PASS;
        }

        PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
        boolean shielding = poisonComp.poisonTicks <= 0;
        // A shielded player cannot be shielded again
        if (shielding && AntidoteShieldPlayerComponent.KEY.get(target).hasShield()) {
            return ActionResult.PASS;
        }

        // Store target UUID in item stack for later use
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("target", target.getUuid());
        if (shielding) {
            nbt.putBoolean(SHIELD_TAG, true);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // Start the use action (arm-raising animation)
        user.setCurrentHand(hand);

        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!GameFunctions.isPlayerAliveAndSurvival(user)) {
            return TypedActionResult.pass(stack);
        }

        PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(user);
        boolean shielding = poisonComp.poisonTicks <= 0;
        if (shielding && AntidoteShieldPlayerComponent.KEY.get(user).hasShield()) {
            return TypedActionResult.pass(stack);
        }

        // Store self UUID as target
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("target", user.getUuid());
        if (shielding) {
            nbt.putBoolean(SHIELD_TAG, true);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        user.setCurrentHand(hand);

        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) return;

        // Get stored target
        UUID targetUuid = getTargetUuid(stack);
        UUID dummyId = getDummyUuid(stack);
        if (targetUuid == null && dummyId == null) {
            user.stopUsingItem();
            return;
        }

        if (dummyId != null && user instanceof PlayerEntity player) {
            HallucinationPlayerComponent hallucinationComponent = HallucinationPlayerComponent.KEY.get(player);
            if (hallucinationComponent.getDummy(dummyId).isEmpty() || hallucinationComponent.getDummyPoisonState(dummyId).isEmpty()) {
                user.stopUsingItem();
                clearTargetUuid(stack);
                return;
            }
        } else {
            PlayerEntity target = world.getPlayerByUuid(targetUuid);
            if (target == null || user.squaredDistanceTo(target) > MAX_DISTANCE_SQ
                    || !GameFunctions.isPlayerAliveAndSurvival(target)) {
                user.stopUsingItem();
                clearTargetUuid(stack);
                return;
            }

            PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
            if (isShieldMode(stack)) {
                // Preemptive shield: cancel if the target gets poisoned mid-charge
                if (poisonComp.poisonTicks > 0) {
                    user.stopUsingItem();
                    clearTargetUuid(stack);
                    return;
                }
            } else if (poisonComp.poisonTicks <= 0) {
                user.stopUsingItem();
                clearTargetUuid(stack);
                return;
            }
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world.isClient) {
            return stack;
        }

        if (!(user instanceof PlayerEntity player)) {
            return stack;
        }

        // Get stored target
        UUID targetUuid = getTargetUuid(stack);
        UUID dummyId = getDummyUuid(stack);
        if (targetUuid == null && dummyId == null) {
            return stack;
        }

        if (dummyId != null) {
            HallucinationPlayerComponent hallucinationComponent = HallucinationPlayerComponent.KEY.get(player);
            if (hallucinationComponent.cureDummyPoison(dummyId)) {
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.2F);
                player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                hallucinationComponent.sync();
            }
            clearTargetUuid(stack);
            return stack;
        }

        PlayerEntity target = world.getPlayerByUuid(targetUuid);
        if (target == null || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            clearTargetUuid(stack);
            return stack;
        }

        // Final range check
        if (user.squaredDistanceTo(target) > MAX_DISTANCE_SQ) {
            clearTargetUuid(stack);
            return stack;
        }

        PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);

        if (isShieldMode(stack)) {
            // Preemptive shield: only valid if the target is still unpoisoned
            if (poisonComp.poisonTicks <= 0) {
                AntidoteShieldPlayerComponent shieldComp = AntidoteShieldPlayerComponent.KEY.get(target);
                if (!shieldComp.hasShield()) {
                    shieldComp.applyShield();
                    // Silently grants protection - no sound for non-poisoned targets
                    player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                }
            }
            clearTargetUuid(stack);
            return stack;
        }

        // Cure the poison
        if (poisonComp.poisonTicks > 0) {
            poisonComp.reset();

            // Play success sound
            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.2F);

            // Set cooldown
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayerEntity recordTarget = target instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "cure");
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), recordTarget, extra);
            }
        }

        // Clear target data
        clearTargetUuid(stack);

        return stack;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        // Clear target data when use is interrupted
        clearTargetUuid(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TIME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    private boolean isShieldMode(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            return nbt.getBoolean(SHIELD_TAG);
        }
        return false;
    }

    private UUID getTargetUuid(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.containsUuid("target")) {
                return nbt.getUuid("target");
            }
        }
        return null;
    }

    private UUID getDummyUuid(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.containsUuid("hallucinationDummy")) {
                return nbt.getUuid("hallucinationDummy");
            }
        }
        return null;
    }

    private void clearTargetUuid(ItemStack stack) {
        stack.remove(DataComponentTypes.CUSTOM_DATA);
    }
}

package org.agmas.noellesroles.client.hallucination;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hallucination.ClientHallucinationVisualHelper;
import org.agmas.noellesroles.hallucination.HallucinationEffectId;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;
import org.agmas.noellesroles.util.HiddenEquipmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ClientHallucinationHeldItemHelper {
    private static final ThreadLocal<AbstractClientPlayerEntity> CURRENT_RENDERED_PLAYER = new ThreadLocal<>();

    private ClientHallucinationHeldItemHelper() {
    }

    public static void beginHeldItemRender(@Nullable AbstractClientPlayerEntity player) {
        if (player == null) {
            CURRENT_RENDERED_PLAYER.remove();
            return;
        }
        CURRENT_RENDERED_PLAYER.set(player);
    }

    public static void endHeldItemRender() {
        CURRENT_RENDERED_PLAYER.remove();
    }

    public static ItemStack getScrambledHeldItem(ItemStack original, ModelTransformationMode renderMode) {
        if (!isThirdPersonHandRender(renderMode)) {
            return original;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        AbstractClientPlayerEntity owner = CURRENT_RENDERED_PLAYER.get();
        if (client.player == null || client.world == null || owner == null || owner == client.player) {
            return original;
        }

        HallucinationPlayerComponent component = HallucinationPlayerComponent.KEY.get(client.player);
        UUID apparentUuid = getApparentPlayerUuid(owner);
        if (!shouldScrambleHeldItems(client, component, apparentUuid)) {
            return original;
        }

        UUID shuffledUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(apparentUuid);
        if (shuffledUuid == null && apparentUuid.equals(owner.getUuid()) && ClientHallucinationVisualHelper.shouldUseShuffledAppearance(owner)) {
            var shuffledEntry = ClientHallucinationVisualHelper.getShuffledEntry(owner);
            if (shuffledEntry != null && shuffledEntry.getProfile() != null) {
                shuffledUuid = shuffledEntry.getProfile().getId();
            }
        }
        if (shuffledUuid != null) {
            PlayerEntity target = client.world.getPlayerByUuid(shuffledUuid);
            if (target != null) {
                ItemStack replacement = getDisplayedHandStack(target, renderMode);
                if (!replacement.isEmpty() && !HiddenEquipmentHelper.shouldHideItem(replacement, target, client.player)) {
                    return replacement.copy();
                }
            }
        }

        PlayerEntity fallbackTarget = getFallbackHeldItemSource(client, owner, apparentUuid, renderMode);
        if (fallbackTarget == null) {
            return original;
        }

        ItemStack replacement = getDisplayedHandStack(fallbackTarget, renderMode);
        if (replacement.isEmpty() || HiddenEquipmentHelper.shouldHideItem(replacement, fallbackTarget, client.player)) {
            return original;
        }
        return replacement.copy();
    }

    private static ItemStack getDisplayedHandStack(PlayerEntity player, ModelTransformationMode renderMode) {
        Arm mainArm = player.getMainArm();
        ItemStack rightHandStack = mainArm == Arm.RIGHT ? player.getMainHandStack() : player.getOffHandStack();
        ItemStack leftHandStack = mainArm == Arm.RIGHT ? player.getOffHandStack() : player.getMainHandStack();
        if (renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND) {
            return rightHandStack;
        }
        if (renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND) {
            return leftHandStack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isThirdPersonHandRender(ModelTransformationMode renderMode) {
        return renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
    }

    private static UUID getApparentPlayerUuid(AbstractClientPlayerEntity owner) {
        var profile = owner.getGameProfile();
        if (profile != null && profile.getId() != null) {
            return profile.getId();
        }
        return owner.getUuid();
    }

    private static boolean shouldScrambleHeldItems(MinecraftClient client,
                                                   HallucinationPlayerComponent component,
                                                   UUID apparentUuid) {
        if (client.world == null || WatheClient.moodComponent == null || apparentUuid == null) {
            return false;
        }
        if (!NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(apparentUuid)) {
            return false;
        }
        ConfigWorldComponent config = ConfigWorldComponent.KEY.get(client.world);
        return config.insaneSeesMorphs && WatheClient.moodComponent.isLowerThanDepressed();
    }

    private static @Nullable PlayerEntity getFallbackHeldItemSource(MinecraftClient client,
                                                                     AbstractClientPlayerEntity owner,
                                                                     UUID apparentUuid,
                                                                     ModelTransformationMode renderMode) {
        if (client.world == null) {
            return null;
        }
        var players = client.world.getPlayers().stream()
                .filter(candidate -> candidate != owner)
                .filter(candidate -> candidate != client.player)
                .toList();
        if (players.isEmpty()) {
            return null;
        }

        int startIndex = Math.floorMod(apparentUuid.hashCode() ^ client.player.getUuid().hashCode(), players.size());
        for (int offset = 0; offset < players.size(); offset++) {
            PlayerEntity candidate = players.get((startIndex + offset) % players.size());
            ItemStack displayed = getDisplayedHandStack(candidate, renderMode);
            if (!displayed.isEmpty() && !HiddenEquipmentHelper.shouldHideItem(displayed, candidate, client.player)) {
                return candidate;
            }
        }
        return null;
    }
}

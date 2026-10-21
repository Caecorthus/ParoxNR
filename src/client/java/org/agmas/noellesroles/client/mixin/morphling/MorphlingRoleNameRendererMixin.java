package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hallucination.ClientHallucinationVisualHelper;
import org.agmas.noellesroles.client.morphling.ClientMorphlingScrambleHelper;
import org.agmas.noellesroles.client.puppeteer.PuppeteerClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public abstract class MorphlingRoleNameRendererMixin {

    @WrapOperation(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text noellesroles$swapRoleHudName(PlayerEntity instance, Operation<Text> original) {
        if (instance instanceof AbstractClientPlayerEntity clientPlayer
                && ClientHallucinationVisualHelper.shouldUseShuffledAppearance(clientPlayer)) {
            return Text.literal("??!?!").formatted(Formatting.OBFUSCATED);
        }
        if (ClientMorphlingScrambleHelper.isMyriadScrambled()) {
            UUID shuffled = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(instance.getUuid());
            if (shuffled != null) {
                Text shuffledName = noellesroles$nameOfUuid(instance.getWorld(), shuffled);
                if (shuffledName != null) {
                    return shuffledName;
                }
            }
        }
        if (instance.isInvisible()) {
            return Text.literal("");
        }
        // 傀儡形态：名牌显示为死者
        UUID puppetDisguise = PuppeteerClient.getPuppetDisguise(instance);
        if (puppetDisguise != null) {
            return PuppeteerClient.getName(puppetDisguise);
        }
        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(instance);
        if (morphComp.corpseMode) {
            return Text.literal("");
        }
        if (morphComp.getMorphTicks() > 0) {
            Text disguiseName = noellesroles$nameOfUuid(instance.getWorld(), morphComp.disguise);
            if (disguiseName != null) {
                return disguiseName;
            }
            if (MinecraftClient.getInstance().player != null
                    && morphComp.disguise.equals(MinecraftClient.getInstance().player.getUuid())) {
                return MinecraftClient.getInstance().player.getDisplayName();
            }
        }
        return original.call(instance);
    }

    @Unique
    private static @Nullable Text noellesroles$nameOfUuid(World world, UUID uuid) {
        PlayerEntity targetPlayer = world.getPlayerByUuid(uuid);
        if (targetPlayer != null) {
            return targetPlayer.getDisplayName();
        }
        PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (cachedEntry != null) {
            return cachedEntry.getDisplayName() != null
                    ? cachedEntry.getDisplayName()
                    : Text.literal(cachedEntry.getProfile().getName());
        }
        return null;
    }
}

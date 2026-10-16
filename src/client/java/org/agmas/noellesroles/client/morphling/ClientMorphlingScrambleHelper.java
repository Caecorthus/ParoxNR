package org.agmas.noellesroles.client.morphling;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hallucination.ClientHallucinationVisualHelper;
import org.agmas.noellesroles.morphling.MorphlingScramblePlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 变形者「万千变化」光环的乱序皮肤解析：先沿用幻觉/低理智乱序，再叠加光环。 */
public final class ClientMorphlingScrambleHelper {
    private ClientMorphlingScrambleHelper() {
    }

    public static boolean isMyriadScrambled() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        return MorphlingScramblePlayerComponent.KEY.get(client.player).isScrambled();
    }

    public static @Nullable PlayerListEntry getScrambledEntry(@Nullable AbstractClientPlayerEntity player) {
        PlayerListEntry hallucinationEntry = ClientHallucinationVisualHelper.getShuffledEntry(player);
        if (hallucinationEntry != null) {
            return hallucinationEntry;
        }
        if (player == null || !isMyriadScrambled()) {
            return null;
        }
        UUID shuffledUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(player.getUuid());
        return shuffledUuid == null ? null : WatheClient.PLAYER_ENTRIES_CACHE.get(shuffledUuid);
    }

    public static @Nullable SkinTextures getScrambledSkinTextures(@Nullable AbstractClientPlayerEntity player) {
        PlayerListEntry entry = getScrambledEntry(player);
        return entry != null ? entry.getSkinTextures() : null;
    }
}

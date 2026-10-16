package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SpectatorInfoSyncS2CPacket(long requestId,
                                         long matchStartTick,
                                         long latestReplayTick,
                                         List<Entry> entries,
                                         List<ReplayToast> replayToasts) implements CustomPayload {
    public static final Id<SpectatorInfoSyncS2CPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "spectator_info_sync"));

    public static final PacketCodec<RegistryByteBuf, SpectatorInfoSyncS2CPacket> CODEC =
            PacketCodec.of(SpectatorInfoSyncS2CPacket::write, SpectatorInfoSyncS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarLong(requestId);
        buf.writeVarLong(matchStartTick);
        buf.writeVarLong(latestReplayTick);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUuid(entry.uuid());
            buf.writeString(entry.roleTranslationKey(), 128);
            buf.writeInt(entry.roleColor());
            buf.writeString(entry.deathReasonRaw(), 256);
            buf.writeVarLong(entry.deathTick());
            buf.writeVarInt(entry.deathAgeSeconds());
            buf.writeVarLong(entry.latestRelevantReplayTick());
            buf.writeString(entry.replaySummary(), 4096);
            buf.writeInt(entry.balance());
        }
        buf.writeVarInt(replayToasts.size());
        for (ReplayToast replayToast : replayToasts) {
            buf.writeVarLong(replayToast.worldTick());
            buf.writeString(replayToast.actorRoleKey(), 128);
            buf.writeString(replayToast.targetRoleKey(), 128);
            buf.writeString(replayToast.deathReasonRaw(), 256);
        }
    }

    private static SpectatorInfoSyncS2CPacket read(PacketByteBuf buf) {
        long requestId = buf.readVarLong();
        long matchStartTick = buf.readVarLong();
        long latestReplayTick = buf.readVarLong();
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            String roleTranslationKey = buf.readString(128);
            int roleColor = buf.readInt();
            String deathReasonRaw = buf.readString(256);
            long deathTick = buf.readVarLong();
            int deathAgeSeconds = buf.readVarInt();
            long latestRelevantReplayTick = buf.readVarLong();
            String replaySummary = buf.readString(4096);
            int balance = buf.readInt();
            entries.add(new Entry(uuid, roleTranslationKey, roleColor, deathReasonRaw, deathTick, deathAgeSeconds, latestRelevantReplayTick, replaySummary, balance));
        }
        int toastCount = buf.readVarInt();
        List<ReplayToast> replayToasts = new ArrayList<>(toastCount);
        for (int i = 0; i < toastCount; i++) {
            long worldTick = buf.readVarLong();
            String actorRoleKey = buf.readString(128);
            String targetRoleKey = buf.readString(128);
            String deathReasonRaw = buf.readString(256);
            replayToasts.add(new ReplayToast(worldTick, actorRoleKey, targetRoleKey, deathReasonRaw));
        }
        return new SpectatorInfoSyncS2CPacket(requestId, matchStartTick, latestReplayTick, entries, replayToasts);
    }

    /** balance 为 -1 表示该玩家没有金币系统（或已离线），客户端不显示金币。 */
    public record Entry(UUID uuid,
                        String roleTranslationKey,
                        int roleColor,
                        String deathReasonRaw,
                        long deathTick,
                        int deathAgeSeconds,
                        long latestRelevantReplayTick,
                        String replaySummary,
                        int balance) {
    }

    public record ReplayToast(long worldTick, String actorRoleKey, String targetRoleKey, String deathReasonRaw) {
    }
}

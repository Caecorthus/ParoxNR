package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 服务端通知客户端打开（或恢复）某破坏点某等级的舒尔特方格小游戏。 */
public record SchulteStartS2CPacket(int pointId, int level, int size,
                                    int timeLimitTicks, int ticksRemaining,
                                    int nextIndex, int[] sequence) implements CustomPayload {
    public static final CustomPayload.Id<SchulteStartS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "schulte_start"));

    public static final PacketCodec<RegistryByteBuf, SchulteStartS2CPacket> CODEC =
            PacketCodec.of(SchulteStartS2CPacket::write, SchulteStartS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarInt(pointId);
        buf.writeVarInt(level);
        buf.writeVarInt(size);
        buf.writeVarInt(timeLimitTicks);
        buf.writeVarInt(ticksRemaining);
        buf.writeVarInt(nextIndex);
        buf.writeVarInt(sequence.length);
        for (int value : sequence) {
            buf.writeVarInt(value);
        }
    }

    private static SchulteStartS2CPacket read(PacketByteBuf buf) {
        int pointId = buf.readVarInt();
        int level = buf.readVarInt();
        int size = buf.readVarInt();
        int timeLimitTicks = buf.readVarInt();
        int ticksRemaining = buf.readVarInt();
        int nextIndex = buf.readVarInt();
        int length = buf.readVarInt();
        int[] sequence = new int[length];
        for (int i = 0; i < length; i++) {
            sequence[i] = buf.readVarInt();
        }
        return new SchulteStartS2CPacket(pointId, level, size, timeLimitTicks, ticksRemaining, nextIndex, sequence);
    }
}

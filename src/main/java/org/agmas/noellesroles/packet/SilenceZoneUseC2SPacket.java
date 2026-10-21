package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/** 寂静装置选人面板点击头像后，请求在目标玩家周围创造无声区域。 */
public record SilenceZoneUseC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "silence_zone_use");
    public static final Id<SilenceZoneUseC2SPacket> ID = new Id<>(PACKET_ID);

    public static final PacketCodec<RegistryByteBuf, SilenceZoneUseC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), SilenceZoneUseC2SPacket::targetPlayer,
            SilenceZoneUseC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

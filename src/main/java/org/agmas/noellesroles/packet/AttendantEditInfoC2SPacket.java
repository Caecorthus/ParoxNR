package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/** 乘务员在登记面板中修改某位乘客的登记信息。field：0=见面 1=存活 2=职业。 */
public record AttendantEditInfoC2SPacket(UUID target, int field, String value) implements CustomPayload {
    public static final CustomPayload.Id<AttendantEditInfoC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "attendant_edit_info"));
    public static final PacketCodec<RegistryByteBuf, AttendantEditInfoC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, AttendantEditInfoC2SPacket::target,
            PacketCodecs.VAR_INT, AttendantEditInfoC2SPacket::field,
            PacketCodecs.STRING, AttendantEditInfoC2SPacket::value,
            AttendantEditInfoC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

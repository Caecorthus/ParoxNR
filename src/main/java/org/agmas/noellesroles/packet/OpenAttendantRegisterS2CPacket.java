package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 服务端通知客户端打开乘务登记表面板（右键登记表时发送）。 */
public record OpenAttendantRegisterS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenAttendantRegisterS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "open_attendant_register"));

    public static final PacketCodec<RegistryByteBuf, OpenAttendantRegisterS2CPacket> CODEC =
            PacketCodec.unit(new OpenAttendantRegisterS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

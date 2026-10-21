package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 服务端通知客户端打开寂静装置选人面板（右键寂静装置时发送）。 */
public record OpenSilenceZonePanelS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenSilenceZonePanelS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "open_silence_zone_panel"));

    public static final PacketCodec<RegistryByteBuf, OpenSilenceZonePanelS2CPacket> CODEC =
            PacketCodec.unit(new OpenSilenceZonePanelS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 服务端通知客户端打开巫毒绑定菜单（右键巫毒娃娃时发送）。 */
public record OpenVoodooPanelS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenVoodooPanelS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "open_voodoo_panel"));

    public static final PacketCodec<RegistryByteBuf, OpenVoodooPanelS2CPacket> CODEC =
            PacketCodec.unit(new OpenVoodooPanelS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

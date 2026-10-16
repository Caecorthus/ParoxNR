package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 服务端通知客户端打开刺客猜测面板（右键刺客之书时发送）。 */
public record OpenAssassinBookPanelS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenAssassinBookPanelS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "open_assassin_book_panel"));

    public static final PacketCodec<RegistryByteBuf, OpenAssassinBookPanelS2CPacket> CODEC =
            PacketCodec.unit(new OpenAssassinBookPanelS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

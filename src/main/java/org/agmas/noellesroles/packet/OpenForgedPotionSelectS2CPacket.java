package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 服务端通知客户端打开赝造药水选人界面（手持药水右键时发送）。 */
public record OpenForgedPotionSelectS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenForgedPotionSelectS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "open_forged_potion_select"));

    public static final PacketCodec<RegistryByteBuf, OpenForgedPotionSelectS2CPacket> CODEC =
            PacketCodec.unit(new OpenForgedPotionSelectS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

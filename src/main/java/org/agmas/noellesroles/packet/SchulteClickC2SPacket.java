package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 玩家在舒尔特方格小游戏中点击一个数字（服务端权威校验）。 */
public record SchulteClickC2SPacket(int pointId, int level, int number) implements CustomPayload {
    public static final CustomPayload.Id<SchulteClickC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "schulte_click"));

    public static final PacketCodec<RegistryByteBuf, SchulteClickC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SchulteClickC2SPacket::pointId,
            PacketCodecs.VAR_INT, SchulteClickC2SPacket::level,
            PacketCodecs.VAR_INT, SchulteClickC2SPacket::number,
            SchulteClickC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

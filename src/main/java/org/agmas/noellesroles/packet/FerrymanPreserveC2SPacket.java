package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 摆渡人技能二“不腐处理”：客户端请求对当前瞄准的尸体执行防腐（服务端自行射线校验目标）。
 */
public record FerrymanPreserveC2SPacket() implements CustomPayload {
    public static final Id<FerrymanPreserveC2SPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "ferryman_preserve"));

    public static final PacketCodec<RegistryByteBuf, FerrymanPreserveC2SPacket> CODEC;

    static {
        CODEC = PacketCodec.of(FerrymanPreserveC2SPacket::write, buf -> new FerrymanPreserveC2SPacket());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }
}

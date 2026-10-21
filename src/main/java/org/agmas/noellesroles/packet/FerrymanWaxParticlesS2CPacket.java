package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 不腐处理的涂蜡粒子包：仅发送给验尸官、杀手与中立玩家，客户端在目标尸体周围生成原版 WAX_ON 粒子。
 */
public record FerrymanWaxParticlesS2CPacket(int entityId) implements CustomPayload {
    public static final Id<FerrymanWaxParticlesS2CPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "ferryman_wax_particles"));

    public static final PacketCodec<RegistryByteBuf, FerrymanWaxParticlesS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, FerrymanWaxParticlesS2CPacket::entityId,
            FerrymanWaxParticlesS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

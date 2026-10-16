package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 经济学家的《经济财报》观测数据：
 * 服务端在观测期间定期推送目标玩家当前金币与剩余观测时间，remainingTicks 为 0 时客户端隐藏该行。
 */
public record EconomistReportS2CPacket(int balance, int remainingTicks) implements CustomPayload {
    public static final Id<EconomistReportS2CPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "economist_report"));

    public static final PacketCodec<RegistryByteBuf, EconomistReportS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, EconomistReportS2CPacket::balance,
            PacketCodecs.INTEGER, EconomistReportS2CPacket::remainingTicks,
            EconomistReportS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 小游戏结果：0=本等级修复完成，1=点错数字失败，2=超时失败。客户端据此关闭界面。 */
public record SchulteResultS2CPacket(int pointId, int level, int outcome) implements CustomPayload {
    public static final int OUTCOME_LEVEL_DONE = 0;
    public static final int OUTCOME_FAIL_WRONG = 1;
    public static final int OUTCOME_FAIL_TIMEOUT = 2;

    public static final CustomPayload.Id<SchulteResultS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "schulte_result"));

    public static final PacketCodec<RegistryByteBuf, SchulteResultS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SchulteResultS2CPacket::pointId,
            PacketCodecs.VAR_INT, SchulteResultS2CPacket::level,
            PacketCodecs.VAR_INT, SchulteResultS2CPacket::outcome,
            SchulteResultS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

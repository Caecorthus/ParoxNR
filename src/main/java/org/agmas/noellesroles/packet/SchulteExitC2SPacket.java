package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 玩家点退出按钮或关闭小游戏界面（服务端按修复失败处理）。 */
public record SchulteExitC2SPacket(int pointId, int level) implements CustomPayload {
    public static final CustomPayload.Id<SchulteExitC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "schulte_exit"));

    public static final PacketCodec<RegistryByteBuf, SchulteExitC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SchulteExitC2SPacket::pointId,
            PacketCodecs.VAR_INT, SchulteExitC2SPacket::level,
            SchulteExitC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

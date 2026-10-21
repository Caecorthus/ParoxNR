package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 把"自己是否处于开局击退压制窗口"同步给玩家本人客户端，
 * 供攻击本地预测（挥空/冲刺音效等）判定使用；服务端权威判定不变。
 */
public record OpeningKnockbackSuppressS2CPacket(boolean suppressed) implements CustomPayload {
    public static final Id<OpeningKnockbackSuppressS2CPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "opening_knockback_suppress"));

    public static final PacketCodec<RegistryByteBuf, OpeningKnockbackSuppressS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BYTE.xmap(b -> b != 0, (Boolean value) -> (byte) (value ? 1 : 0)),
            OpeningKnockbackSuppressS2CPacket::suppressed,
            OpeningKnockbackSuppressS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

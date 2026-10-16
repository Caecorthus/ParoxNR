package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 双角色选择 UI 数据包。
 * canChoose = true  → 显示二选按钮，等待玩家选择；
 * canChoose = false → 仅显示"你的角色是 XXX"宣言屏，5 秒后自动关闭（固定槽位玩家/无备选项玩家）。
 */
public record DualRoleOptionsS2CPacket(
        Identifier firstRoleId,
        String firstRoleDisplayKey,
        Identifier secondRoleId,
        String secondRoleDisplayKey,
        int totalTicks,
        boolean canChoose
) implements CustomPayload {
    public static final CustomPayload.Id<DualRoleOptionsS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "dual_role_options"));

    public static final PacketCodec<RegistryByteBuf, DualRoleOptionsS2CPacket> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, DualRoleOptionsS2CPacket::firstRoleId,
                    PacketCodecs.STRING, DualRoleOptionsS2CPacket::firstRoleDisplayKey,
                    Identifier.PACKET_CODEC, DualRoleOptionsS2CPacket::secondRoleId,
                    PacketCodecs.STRING, DualRoleOptionsS2CPacket::secondRoleDisplayKey,
                    PacketCodecs.INTEGER, DualRoleOptionsS2CPacket::totalTicks,
                    PacketCodecs.BOOL, DualRoleOptionsS2CPacket::canChoose,
                    DualRoleOptionsS2CPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

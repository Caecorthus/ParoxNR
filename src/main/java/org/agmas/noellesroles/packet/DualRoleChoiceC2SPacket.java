package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record DualRoleChoiceC2SPacket(boolean choseSecond) implements CustomPayload {
    public static final CustomPayload.Id<DualRoleChoiceC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "dual_role_choice"));

    public static final PacketCodec<RegistryByteBuf, DualRoleChoiceC2SPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL, DualRoleChoiceC2SPacket::choseSecond,
                    DualRoleChoiceC2SPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

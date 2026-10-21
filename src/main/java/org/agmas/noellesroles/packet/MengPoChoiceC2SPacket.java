package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** 孟婆开局选择：duji=true 为渡己，false 为渡人。 */
public record MengPoChoiceC2SPacket(boolean duji) implements CustomPayload {
    public static final Id<MengPoChoiceC2SPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "mengpo_choice"));

    public static final PacketCodec<RegistryByteBuf, MengPoChoiceC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOL, MengPoChoiceC2SPacket::duji, MengPoChoiceC2SPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

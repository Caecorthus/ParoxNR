package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * Client to Server packet for Criminal Reasoner invisibility (ability key 2).
 */
public record CriminalReasonerInvisibilityC2SPacket() implements CustomPayload {
    public static final Identifier INVISIBILITY_PAYLOAD_ID =
            Identifier.of(Noellesroles.MOD_ID, "criminal_reasoner_invisibility");
    public static final Id<CriminalReasonerInvisibilityC2SPacket> ID = new Id<>(INVISIBILITY_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, CriminalReasonerInvisibilityC2SPacket> CODEC;

    public CriminalReasonerInvisibilityC2SPacket() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static CriminalReasonerInvisibilityC2SPacket read(PacketByteBuf buf) {
        return new CriminalReasonerInvisibilityC2SPacket();
    }

    static {
        CODEC = PacketCodec.of(CriminalReasonerInvisibilityC2SPacket::write, CriminalReasonerInvisibilityC2SPacket::read);
    }
}

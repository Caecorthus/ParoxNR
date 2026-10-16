package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record JunkerSearchC2SPacket(UUID playerBody) implements CustomPayload {
    public static final Identifier JUNKER_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "junker_search");
    public static final Id<JunkerSearchC2SPacket> ID = new Id<>(JUNKER_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, JunkerSearchC2SPacket> CODEC;

    public JunkerSearchC2SPacket(UUID playerBody) {
        this.playerBody = playerBody;
    }

    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.playerBody);
    }

    public static JunkerSearchC2SPacket read(PacketByteBuf buf) {
        return new JunkerSearchC2SPacket(buf.readUuid());
    }

    public UUID playerBody() {
        return this.playerBody;
    }

    static {
        CODEC = PacketCodec.of(JunkerSearchC2SPacket::write, JunkerSearchC2SPacket::read);
    }
}

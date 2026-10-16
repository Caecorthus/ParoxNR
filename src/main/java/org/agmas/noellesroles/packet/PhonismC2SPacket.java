package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record PhonismC2SPacket(UUID targetPlayer, String sound) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "phonism");
    public static final Id<PhonismC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, PhonismC2SPacket> CODEC;

    public PhonismC2SPacket(UUID targetPlayer, String sound) {
        this.targetPlayer = targetPlayer;
        this.sound = sound;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetPlayer);
        buf.writeString(this.sound);
    }

    public static PhonismC2SPacket read(PacketByteBuf buf) {
        return new PhonismC2SPacket(buf.readUuid(), buf.readString());
    }

    public UUID targetPlayer() {
        return this.targetPlayer;
    }

    public String sound() {
        return this.sound;
    }

    static {
        CODEC = PacketCodec.of(PhonismC2SPacket::write, PhonismC2SPacket::read);
    }
}

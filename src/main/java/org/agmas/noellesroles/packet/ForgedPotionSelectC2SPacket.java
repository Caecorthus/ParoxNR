package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/** 赝造药水选人界面：客户端选择要伪装成的玩家。 */
public record ForgedPotionSelectC2SPacket(UUID target) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "forged_potion_select");
    public static final Id<ForgedPotionSelectC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ForgedPotionSelectC2SPacket> CODEC;

    public ForgedPotionSelectC2SPacket(UUID target) {
        this.target = target;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.target);
    }

    public static ForgedPotionSelectC2SPacket read(PacketByteBuf buf) {
        return new ForgedPotionSelectC2SPacket(buf.readUuid());
    }

    static {
        CODEC = PacketCodec.of(ForgedPotionSelectC2SPacket::write, ForgedPotionSelectC2SPacket::read);
    }
}

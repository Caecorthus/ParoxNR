package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 傀儡师的技能操作：傀儡术（target 为尸体实体 UUID）、操纵（target 为傀儡的生前玩家 UUID，可空）、
 * 切回本体、回收傀儡。
 */
public record PuppeteerActionC2SPacket(Action action, @Nullable UUID target) implements CustomPayload {
    public static final Identifier PUPPETEER_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "puppeteer_action");
    public static final Id<PuppeteerActionC2SPacket> ID = new Id<>(PUPPETEER_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, PuppeteerActionC2SPacket> CODEC;

    public enum Action {
        CONVERT,
        CONTROL,
        RETURN,
        RECALL
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(this.action);
        buf.writeBoolean(this.target != null);
        if (this.target != null) {
            buf.writeUuid(this.target);
        }
    }

    public static PuppeteerActionC2SPacket read(PacketByteBuf buf) {
        Action action = buf.readEnumConstant(Action.class);
        UUID target = buf.readBoolean() ? buf.readUuid() : null;
        return new PuppeteerActionC2SPacket(action, target);
    }

    static {
        CODEC = PacketCodec.of(PuppeteerActionC2SPacket::write, PuppeteerActionC2SPacket::read);
    }
}

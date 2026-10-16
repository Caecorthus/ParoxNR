package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/** OP 在 O 键配置界面修改服务端 gameplay 选项后同步到服务端。 */
public record ServerConfigUpdateC2SPacket(
        boolean insanePlayersSeeMorphs,
        boolean voodooNonKillerDeaths,
        boolean showFogRadiusHud,
        boolean showHallucinationHud,
        boolean skipModeVoting,
        boolean skipMapVoting,
        int generalCooldownTicks,
        int matchLengthSeconds,
        int whiteKnightChancePercent,
        boolean misKillGunCooldown
) implements CustomPayload {
    public static final CustomPayload.Id<ServerConfigUpdateC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "server_config_update"));

    public static final PacketCodec<RegistryByteBuf, ServerConfigUpdateC2SPacket> CODEC =
            PacketCodec.of(ServerConfigUpdateC2SPacket::write, ServerConfigUpdateC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeBoolean(insanePlayersSeeMorphs);
        buf.writeBoolean(voodooNonKillerDeaths);
        buf.writeBoolean(showFogRadiusHud);
        buf.writeBoolean(showHallucinationHud);
        buf.writeBoolean(skipModeVoting);
        buf.writeBoolean(skipMapVoting);
        buf.writeVarInt(generalCooldownTicks);
        buf.writeVarInt(matchLengthSeconds);
        buf.writeVarInt(whiteKnightChancePercent);
        buf.writeBoolean(misKillGunCooldown);
    }

    private static ServerConfigUpdateC2SPacket read(PacketByteBuf buf) {
        return new ServerConfigUpdateC2SPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }
}

package org.agmas.noellesroles.client.configscreen;

import dev.doctor4t.wathe.WatheConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.packet.ServerConfigUpdateC2SPacket;

public class ConfigScreenState {
    private final NoellesRolesConfig noellesRolesConfig;
    private WatheConfig.InstinctModeConfig instinctMode;
    private int chatHistoryLimit;

    public static ConfigScreenState defaults() {
        return new ConfigScreenState(
                new NoellesRolesConfig(),
                WatheConfig.InstinctModeConfig.HOLD,
                500
        );
    }

    private ConfigScreenState(
            NoellesRolesConfig noellesRolesConfig,
            WatheConfig.InstinctModeConfig instinctMode,
            int chatHistoryLimit
    ) {
        this.noellesRolesConfig = noellesRolesConfig;
        this.instinctMode = instinctMode;
        this.chatHistoryLimit = chatHistoryLimit;
    }

    public static ConfigScreenState capture() {
        NoellesRolesConfig captured = NoellesRolesConfig.HANDLER.instance().copy();
        overwriteServerOwnedValues(captured);
        return new ConfigScreenState(
                captured,
                WatheConfig.instinctMode,
                WatheConfig.chatHistoryLimit
        );
    }

    private static boolean isRemoteServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.world != null && client.player != null && !client.isInSingleplayer();
    }

    /** 远程服务器上，gameplay 服务端选项显示服务端同步下来的值。 */
    private static void overwriteServerOwnedValues(NoellesRolesConfig config) {
        if (!isRemoteServer()) {
            return;
        }
        ConfigWorldComponent server = ConfigWorldComponent.KEY.get(MinecraftClient.getInstance().world);
        config.insanePlayersSeeMorphs = server.insaneSeesMorphs;
        config.voodooNonKillerDeaths = server.naturalVoodoosAllowed;
        config.misKillGunCooldown = server.misKillGunCooldown;
        config.showFogRadiusHud = server.showFogRadiusHud;
        config.showHallucinationHud = server.showHallucinationHud;
        config.skipModeVoting = server.skipModeVoting;
        config.skipMapVoting = server.skipMapVoting;
        config.generalCooldownTicks = server.generalCooldownTicks;
        config.matchLengthSeconds = server.matchLengthSeconds;
        config.whiteKnightChancePercent = server.whiteKnightChancePercent;
    }

    /** 用本地保存值覆盖 config 里的服务端字段，防止服务端值被写进客户端本地文件。 */
    private static void restoreServerOwnedValues(NoellesRolesConfig config) {
        NoellesRolesConfig saved = NoellesRolesConfig.HANDLER.instance();
        config.insanePlayersSeeMorphs = saved.insanePlayersSeeMorphs;
        config.voodooNonKillerDeaths = saved.voodooNonKillerDeaths;
        config.misKillGunCooldown = saved.misKillGunCooldown;
        config.showFogRadiusHud = saved.showFogRadiusHud;
        config.showHallucinationHud = saved.showHallucinationHud;
        config.skipModeVoting = saved.skipModeVoting;
        config.skipMapVoting = saved.skipMapVoting;
        config.generalCooldownTicks = saved.generalCooldownTicks;
        config.matchLengthSeconds = saved.matchLengthSeconds;
        config.whiteKnightChancePercent = saved.whiteKnightChancePercent;
    }

    public ConfigScreenState copy() {
        return new ConfigScreenState(
                noellesRolesConfig.copy(),
                instinctMode,
                chatHistoryLimit
        );
    }

    public void apply(boolean includeRestrictedSettings) {
        if (isRemoteServer() && includeRestrictedSettings) {
            ClientPlayNetworking.send(new ServerConfigUpdateC2SPacket(
                    noellesRolesConfig.insanePlayersSeeMorphs,
                    noellesRolesConfig.voodooNonKillerDeaths,
                    noellesRolesConfig.showFogRadiusHud,
                    noellesRolesConfig.showHallucinationHud,
                    noellesRolesConfig.skipModeVoting,
                    noellesRolesConfig.skipMapVoting,
                    noellesRolesConfig.generalCooldownTicks,
                    noellesRolesConfig.matchLengthSeconds,
                    noellesRolesConfig.whiteKnightChancePercent,
                    noellesRolesConfig.misKillGunCooldown
            ));
            restoreServerOwnedValues(noellesRolesConfig);
        }

        if (!includeRestrictedSettings) {
            NoellesRolesConfig savedConfig = NoellesRolesConfig.HANDLER.instance();
            noellesRolesConfig.insanePlayersSeeMorphs = savedConfig.insanePlayersSeeMorphs;
            noellesRolesConfig.generalCooldownTicks = savedConfig.generalCooldownTicks;
            noellesRolesConfig.matchLengthSeconds = savedConfig.matchLengthSeconds;
            noellesRolesConfig.whiteKnightChancePercent = savedConfig.whiteKnightChancePercent;
            noellesRolesConfig.voodooNonKillerDeaths = savedConfig.voodooNonKillerDeaths;
            noellesRolesConfig.misKillGunCooldown = savedConfig.misKillGunCooldown;
            noellesRolesConfig.showFogRadiusHud = savedConfig.showFogRadiusHud;
            noellesRolesConfig.showHallucinationHud = savedConfig.showHallucinationHud;
            noellesRolesConfig.skipModeVoting = savedConfig.skipModeVoting;
            noellesRolesConfig.skipMapVoting = savedConfig.skipMapVoting;
            noellesRolesConfig.lockSoundPhysicsRemasteredConfig = savedConfig.lockSoundPhysicsRemasteredConfig;
            noellesRolesConfig.soundPhysicsRemasteredLockedValues = savedConfig.soundPhysicsRemasteredLockedValues;
            noellesRolesConfig.lockTalkBubblesConfig = savedConfig.lockTalkBubblesConfig;
            noellesRolesConfig.talkBubblesLockedValues = savedConfig.talkBubblesLockedValues;
        }

        NoellesRolesConfig.HANDLER.instance().copyFrom(noellesRolesConfig);
        NoellesRolesConfig.HANDLER.save();

        WatheConfig.instinctMode = instinctMode;
        WatheConfig.chatHistoryLimit = chatHistoryLimit;
        writeWatheConfig();
    }

    private static void writeWatheConfig() {
        try {
            Class<?> midnightConfigClass = Class.forName("eu.midnightdust.lib.config.MidnightConfig");
            midnightConfigClass.getMethod("write", String.class).invoke(null, "wathe");
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to write Wathe config", exception);
        }
    }

    public NoellesRolesConfig noellesRolesConfig() {
        return noellesRolesConfig;
    }

    public WatheConfig.InstinctModeConfig instinctMode() {
        return instinctMode;
    }

    public void instinctMode(WatheConfig.InstinctModeConfig instinctMode) {
        this.instinctMode = instinctMode;
    }

    public int chatHistoryLimit() {
        return chatHistoryLimit;
    }

    public void chatHistoryLimit(int chatHistoryLimit) {
        this.chatHistoryLimit = Math.max(WatheConfig.MIN_CHAT_HISTORY_LIMIT, Math.min(WatheConfig.MAX_CHAT_HISTORY_LIMIT, chatHistoryLimit));
    }
}

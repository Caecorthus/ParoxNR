package org.agmas.noellesroles.config;

import dev.doctor4t.wathe.game.GameConstants;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NoellesRolesConfig {
    public static ConfigClassHandler<NoellesRolesConfig> HANDLER = ConfigClassHandler.createBuilder(NoellesRolesConfig.class)
            .id(Identifier.of(Noellesroles.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve( Noellesroles.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry(comment = "Whether insane players will randomly see people as morphed.")
    public boolean insanePlayersSeeMorphs = true;

    @SerialEntry(comment = "Starting cooldown (in ticks)")
    public int generalCooldownTicks = GameConstants.getInTicks(0,30);

    @SerialEntry(comment = "Match length in seconds, applied when a new match starts.")
    public int matchLengthSeconds = 600;

    @SerialEntry(comment = "Chance (percent) that the White Knight replaces one vigilante slot in a match.")
    public int whiteKnightChancePercent = 50;

    @SerialEntry(comment = "Allow Natural deaths to trigger voodoo (deaths without an assigned killer)")
    public boolean voodooNonKillerDeaths = false;

    @SerialEntry(comment = "Whether the gun dropped by a player who died from the innocent-shooting punishment picks up with a 15s cooldown")
    public boolean misKillGunCooldown = true;

    @SerialEntry(comment = "Show the fog radius HUD for the Fog of War event")
    public boolean showFogRadiusHud = false;

    @SerialEntry(comment = "Show the hallucination HUD for the Fog of War event")
    public boolean showHallucinationHud = false;

    @SerialEntry(comment = "Collapse the top-right info hints to a single config screen hint line")
    public boolean collapseTopRightHints = false;

    @SerialEntry(comment = "Draw the bottom-right skill and task point hint text with an outline")
    public boolean bottomRightHintOutline = false;

    @SerialEntry(comment = "Use the legacy flat GUI style for the role info and dual role screens, and hide the inventory role card")
    public boolean legacyGuiStyle = false;

    @SerialEntry(comment = "Custom label shown in the handmade config screen example")
    public String preferredServerLabel = "NoellesRoles";

    @SerialEntry(comment = "Visual theme preset used by the handmade config screen example")
    public UiThemePreset uiThemePreset = UiThemePreset.MODERN;

    @SerialEntry(comment = "Map registry id used by Death Arena for loading spawn rooms and map enhancements.")
    public String deathArenaMapId = "wathe:yangguan_siwang";

    @SerialEntry(comment = "World dimension id used by Death Arena. If blank, the dimension from deathArenaMapId will be used.")
    public String deathArenaDimensionId = "wathe:yangguan_siwang";

    @SerialEntry(comment = "Skip mode voting after a match and keep the previous game mode")
    public boolean skipModeVoting = false;

    @SerialEntry(comment = "Skip map voting and the map roulette after a match and keep the previous map")
    public boolean skipMapVoting = false;

    @SerialEntry(comment = "Lock Sound Physics Remastered config to the server-defined values while connected")
    public boolean lockSoundPhysicsRemasteredConfig = true;

    @SerialEntry(comment = "Server-enforced Sound Physics Remastered values. Keys must match Sound Physics config entry field names.")
    public Map<String, String> soundPhysicsRemasteredLockedValues = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("enabled", "true"),
            Map.entry("attenuationFactor", "1.0"),
            Map.entry("reverbAttenuationDistance", "0.2"),
            Map.entry("reverbGain", "1.0"),
            Map.entry("reverbBrightness", "1.0"),
            Map.entry("reverbDistance", "1.0"),
            Map.entry("blockAbsorption", "4.0"),
            Map.entry("occlusionVariation", "0.15"),
            Map.entry("defaultBlockReflectivity", "0.5"),
            Map.entry("defaultBlockOcclusionFactor", "1.0"),
            Map.entry("soundDistanceAllowance", "4.0"),
            Map.entry("airAbsorption", "1.0"),
            Map.entry("underwaterFilter", "0.8"),
            Map.entry("evaluateAmbientSounds", "true"),
            Map.entry("environmentEvaluationRayCount", "224"),
            Map.entry("environmentEvaluationRayBounces", "8"),
            Map.entry("nonFullBlockOcclusionFactor", "0.25"),
            Map.entry("maxOcclusionRays", "16"),
            Map.entry("maxOcclusion", "10.0"),
            Map.entry("strictOcclusion", "false"),
            Map.entry("soundDirectionEvaluation", "true"),
            Map.entry("redirectNonOccludedSounds", "true"),
            Map.entry("updateMovingSounds", "true"),
            Map.entry("soundUpdateInterval", "5"),
            Map.entry("maxSoundProcessingDistance", "256.0"),
            Map.entry("unsafeLevelAccess", "false"),
            Map.entry("levelCloneRange", "4"),
            Map.entry("levelCloneMaxRetainTicks", "20"),
            Map.entry("levelCloneMaxRetainBlockDistance", "16"),
            Map.entry("debugLogging", "false"),
            Map.entry("occlusionLogging", "false"),
            Map.entry("environmentLogging", "false"),
            Map.entry("performanceLogging", "false"),
            Map.entry("renderSoundBounces", "false"),
            Map.entry("renderOcclusion", "false"),
            Map.entry("simpleVoiceChatIntegration", "true"),
            Map.entry("hearSelf", "false")
    ));

    @SerialEntry(comment = "Lock TalkBubbles config to the server-defined values while connected")
    public boolean lockTalkBubblesConfig = true;

    @SerialEntry(comment = "Server-enforced TalkBubbles values. Keys must match TalkBubbles config field names.")
    public Map<String, String> talkBubblesLockedValues = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("chatRange", "30.0"),
            Map.entry("chatTime", "200"),
            Map.entry("maxChatWidth", "180"),
            Map.entry("chatColor", "1315860"),
            Map.entry("chatHeight", "-0.5"),
            Map.entry("chatScale", "1.0"),
            Map.entry("backgroundOpacity", "0.699999988079071"),
            Map.entry("backgroundRed", "1.0"),
            Map.entry("backgroundGreen", "1.0"),
            Map.entry("backgroundBlue", "1.0"),
            Map.entry("maxUUIDWordCheck", "0"),
            Map.entry("showOwnBubble", "true")
    ));

    public NoellesRolesConfig copy() {
        NoellesRolesConfig copy = new NoellesRolesConfig();
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(NoellesRolesConfig other) {
        this.insanePlayersSeeMorphs = other.insanePlayersSeeMorphs;
        this.generalCooldownTicks = other.generalCooldownTicks;
        this.matchLengthSeconds = other.matchLengthSeconds;
        this.whiteKnightChancePercent = other.whiteKnightChancePercent;
        this.voodooNonKillerDeaths = other.voodooNonKillerDeaths;
        this.misKillGunCooldown = other.misKillGunCooldown;
        this.showFogRadiusHud = other.showFogRadiusHud;
        this.showHallucinationHud = other.showHallucinationHud;
        this.collapseTopRightHints = other.collapseTopRightHints;
        this.bottomRightHintOutline = other.bottomRightHintOutline;
        this.legacyGuiStyle = other.legacyGuiStyle;
        this.preferredServerLabel = other.preferredServerLabel;
        this.uiThemePreset = other.uiThemePreset;
        this.deathArenaMapId = other.deathArenaMapId;
        this.deathArenaDimensionId = other.deathArenaDimensionId;
        this.skipModeVoting = other.skipModeVoting;
        this.skipMapVoting = other.skipMapVoting;
        this.lockSoundPhysicsRemasteredConfig = other.lockSoundPhysicsRemasteredConfig;
        this.soundPhysicsRemasteredLockedValues = new LinkedHashMap<>(other.soundPhysicsRemasteredLockedValues);
        this.lockTalkBubblesConfig = other.lockTalkBubblesConfig;
        this.talkBubblesLockedValues = new LinkedHashMap<>(other.talkBubblesLockedValues);
    }

    public enum UiThemePreset {
        MODERN,
        COMPACT,
        CINEMATIC
    }
}

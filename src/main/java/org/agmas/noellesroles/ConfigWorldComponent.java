package org.agmas.noellesroles;


import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<ConfigWorldComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "config"), ConfigWorldComponent.class);
    public boolean insaneSeesMorphs = true;
    public boolean naturalVoodoosAllowed = false;
    public boolean misKillGunCooldown = true;
    public boolean showFogRadiusHud = false;
    public boolean showHallucinationHud = false;
    public boolean lockSoundPhysicsRemasteredConfig = false;
    public final Map<String, String> soundPhysicsRemasteredLockedValues = new LinkedHashMap<>();
    public boolean lockTalkBubblesConfig = false;
    public final Map<String, String> talkBubblesLockedValues = new LinkedHashMap<>();
    public int generalCooldownTicks = 0;
    public int matchLengthSeconds = 600;
    public int whiteKnightChancePercent = 50;
    public boolean skipModeVoting = false;
    public boolean skipMapVoting = false;
    private final World world;

    public void reset() {
        this.refreshFromConfig();
        this.sync();
    }

    public ConfigWorldComponent(World world) {
        this.world = world;
        this.refreshFromConfig();
    }

    public void sync() {
        this.refreshFromConfig();
        KEY.sync(this.world);
    }

    private void refreshFromConfig() {
        insaneSeesMorphs = NoellesRolesConfig.HANDLER.instance().insanePlayersSeeMorphs;
        naturalVoodoosAllowed = NoellesRolesConfig.HANDLER.instance().voodooNonKillerDeaths;
        misKillGunCooldown = NoellesRolesConfig.HANDLER.instance().misKillGunCooldown;
        showFogRadiusHud = NoellesRolesConfig.HANDLER.instance().showFogRadiusHud;
        showHallucinationHud = NoellesRolesConfig.HANDLER.instance().showHallucinationHud;
        lockSoundPhysicsRemasteredConfig = NoellesRolesConfig.HANDLER.instance().lockSoundPhysicsRemasteredConfig;
        soundPhysicsRemasteredLockedValues.clear();
        soundPhysicsRemasteredLockedValues.putAll(NoellesRolesConfig.HANDLER.instance().soundPhysicsRemasteredLockedValues);
        lockTalkBubblesConfig = NoellesRolesConfig.HANDLER.instance().lockTalkBubblesConfig;
        talkBubblesLockedValues.clear();
        talkBubblesLockedValues.putAll(NoellesRolesConfig.HANDLER.instance().talkBubblesLockedValues);
        generalCooldownTicks = NoellesRolesConfig.HANDLER.instance().generalCooldownTicks;
        matchLengthSeconds = NoellesRolesConfig.HANDLER.instance().matchLengthSeconds;
        whiteKnightChancePercent = NoellesRolesConfig.HANDLER.instance().whiteKnightChancePercent;
        skipModeVoting = NoellesRolesConfig.HANDLER.instance().skipModeVoting;
        skipMapVoting = NoellesRolesConfig.HANDLER.instance().skipMapVoting;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        // 同步配置数据到客户端
        buf.writeBoolean(this.insaneSeesMorphs);
        buf.writeBoolean(this.naturalVoodoosAllowed);
        buf.writeBoolean(this.showFogRadiusHud);
        buf.writeBoolean(this.showHallucinationHud);
        buf.writeBoolean(this.lockSoundPhysicsRemasteredConfig);
        buf.writeVarInt(this.soundPhysicsRemasteredLockedValues.size());
        this.soundPhysicsRemasteredLockedValues.forEach((key, value) -> {
            buf.writeString(key);
            buf.writeString(value);
        });
        buf.writeBoolean(this.lockTalkBubblesConfig);
        buf.writeVarInt(this.talkBubblesLockedValues.size());
        this.talkBubblesLockedValues.forEach((key, value) -> {
            buf.writeString(key);
            buf.writeString(value);
        });
        buf.writeVarInt(this.generalCooldownTicks);
        buf.writeVarInt(this.matchLengthSeconds);
        buf.writeVarInt(this.whiteKnightChancePercent);
        buf.writeBoolean(this.skipModeVoting);
        buf.writeBoolean(this.skipMapVoting);
        buf.writeBoolean(this.misKillGunCooldown);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        // 从服务端接收配置数据
        this.insaneSeesMorphs = buf.readBoolean();
        this.naturalVoodoosAllowed = buf.readBoolean();
        this.showFogRadiusHud = buf.readBoolean();
        this.showHallucinationHud = buf.readBoolean();
        this.lockSoundPhysicsRemasteredConfig = buf.readBoolean();
        this.soundPhysicsRemasteredLockedValues.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            this.soundPhysicsRemasteredLockedValues.put(buf.readString(), buf.readString());
        }
        this.lockTalkBubblesConfig = buf.readBoolean();
        this.talkBubblesLockedValues.clear();
        int talkBubblesSize = buf.readVarInt();
        for (int i = 0; i < talkBubblesSize; i++) {
            this.talkBubblesLockedValues.put(buf.readString(), buf.readString());
        }
        this.generalCooldownTicks = buf.readVarInt();
        this.matchLengthSeconds = buf.readVarInt();
        this.whiteKnightChancePercent = buf.readVarInt();
        this.skipModeVoting = buf.readBoolean();
        this.skipMapVoting = buf.readBoolean();
        this.misKillGunCooldown = buf.readBoolean();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.refreshFromConfig();
        tag.putBoolean("insaneSeesMorphs", this.insaneSeesMorphs);
        tag.putBoolean("naturalVoodoosAllowed", this.naturalVoodoosAllowed);
        tag.putBoolean("showFogRadiusHud", this.showFogRadiusHud);
        tag.putBoolean("showHallucinationHud", this.showHallucinationHud);
        tag.putBoolean("lockSoundPhysicsRemasteredConfig", this.lockSoundPhysicsRemasteredConfig);
        NbtCompound soundPhysicsTag = new NbtCompound();
        this.soundPhysicsRemasteredLockedValues.forEach(soundPhysicsTag::putString);
        tag.put("soundPhysicsRemasteredLockedValues", soundPhysicsTag);
        tag.putBoolean("lockTalkBubblesConfig", this.lockTalkBubblesConfig);
        NbtCompound talkBubblesTag = new NbtCompound();
        this.talkBubblesLockedValues.forEach(talkBubblesTag::putString);
        tag.put("talkBubblesLockedValues", talkBubblesTag);
        tag.putInt("generalCooldownTicks", this.generalCooldownTicks);
        tag.putInt("matchLengthSeconds", this.matchLengthSeconds);
        tag.putInt("whiteKnightChancePercent", this.whiteKnightChancePercent);
        tag.putBoolean("skipModeVoting", this.skipModeVoting);
        tag.putBoolean("skipMapVoting", this.skipMapVoting);
        tag.putBoolean("misKillGunCooldown", this.misKillGunCooldown);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("insaneSeesMorphs"))   this.insaneSeesMorphs = tag.getBoolean("insaneSeesMorphs");
        if (tag.contains("naturalVoodoosAllowed"))   this.naturalVoodoosAllowed = tag.getBoolean("naturalVoodoosAllowed");
        if (tag.contains("showFogRadiusHud")) this.showFogRadiusHud = tag.getBoolean("showFogRadiusHud");
        if (tag.contains("showHallucinationHud")) this.showHallucinationHud = tag.getBoolean("showHallucinationHud");
        if (tag.contains("lockSoundPhysicsRemasteredConfig")) this.lockSoundPhysicsRemasteredConfig = tag.getBoolean("lockSoundPhysicsRemasteredConfig");
        if (tag.contains("soundPhysicsRemasteredLockedValues", NbtElement.COMPOUND_TYPE)) {
            this.soundPhysicsRemasteredLockedValues.clear();
            NbtCompound soundPhysicsTag = tag.getCompound("soundPhysicsRemasteredLockedValues");
            for (String key : soundPhysicsTag.getKeys()) {
                this.soundPhysicsRemasteredLockedValues.put(key, soundPhysicsTag.getString(key));
            }
        }
        if (tag.contains("lockTalkBubblesConfig")) this.lockTalkBubblesConfig = tag.getBoolean("lockTalkBubblesConfig");
        if (tag.contains("talkBubblesLockedValues", NbtElement.COMPOUND_TYPE)) {
            this.talkBubblesLockedValues.clear();
            NbtCompound talkBubblesTag = tag.getCompound("talkBubblesLockedValues");
            for (String key : talkBubblesTag.getKeys()) {
                this.talkBubblesLockedValues.put(key, talkBubblesTag.getString(key));
            }
        }
        if (tag.contains("generalCooldownTicks")) this.generalCooldownTicks = tag.getInt("generalCooldownTicks");
        if (tag.contains("matchLengthSeconds")) this.matchLengthSeconds = tag.getInt("matchLengthSeconds");
        if (tag.contains("whiteKnightChancePercent")) this.whiteKnightChancePercent = tag.getInt("whiteKnightChancePercent");
        if (tag.contains("skipModeVoting")) this.skipModeVoting = tag.getBoolean("skipModeVoting");
        if (tag.contains("skipMapVoting")) this.skipMapVoting = tag.getBoolean("skipMapVoting");
        if (tag.contains("misKillGunCooldown")) this.misKillGunCooldown = tag.getBoolean("misKillGunCooldown");
    }
}

package org.agmas.noellesroles;


import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.deatharena.DeathArenaPlayerComponent;
import org.agmas.noellesroles.deatharena.DeathArenaWorldComponent;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.bandit.BanditPlayerComponent;
import org.agmas.noellesroles.baosang.BaosangPlayerComponent;
import org.agmas.noellesroles.mengpo.MengPoPlayerComponent;
import org.agmas.noellesroles.mengpo.MengPoSoupPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.pathogen.PathogenPlayerComponent;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.morphling.BodyForgeryWorldComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingScramblePlayerComponent;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.silencer.SilencerPlayerComponent;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.criminalreasoner.CriminalReasonerPlayerComponent;
import org.agmas.noellesroles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;
import org.agmas.noellesroles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.orthopedist.OrthopedistPlayerComponent;
import org.agmas.noellesroles.commander.CommanderPlayerComponent;
import org.agmas.noellesroles.breacher.BreacherPlayerComponent;
import org.agmas.noellesroles.dreameater.DreamEaterPlayerComponent;
import org.agmas.noellesroles.dreameater.SleepingPillPlateComponent;
import org.agmas.noellesroles.economist.EconomistPlayerComponent;
import org.agmas.noellesroles.junker.JunkerLootWorldComponent;
import org.agmas.noellesroles.junker.JunkerPlayerComponent;
import org.agmas.noellesroles.puppeteer.PuppetWorldComponent;
import org.agmas.noellesroles.puppeteer.PuppeteerPlayerComponent;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.agmas.noellesroles.survivalmaster.SurvivalMasterPlayerComponent;
import org.agmas.noellesroles.toxicologist.AntidotePlateShieldComponent;
import org.agmas.noellesroles.toxicologist.AntidoteShieldPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndsRadarWorldComponent;
import org.agmas.noellesroles.murdermayhem.MurderMayhemWorldComponent;
import org.agmas.noellesroles.music.WorldMusicComponent;
import org.agmas.noellesroles.scavenger.HiddenBodiesWorldComponent;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.agmas.noellesroles.ferryman.FerrymanPlayerComponent;
import org.agmas.noellesroles.gangsterstar.GangsterStarPlayerComponent;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;
import org.agmas.noellesroles.killerhistory.KillerHistoryPlayerComponent;
import org.agmas.noellesroles.rolehistory.RoleHistoryPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class NoellesRolesComponents implements EntityComponentInitializer, WorldComponentInitializer {
    public NoellesRolesComponents() {
    }

    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, MorphlingPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MorphlingPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, MorphlingScramblePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MorphlingScramblePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BartenderPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BartenderPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BaosangPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BaosangPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, VoodooPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(VoodooPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AttendantPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AttendantPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AbilityPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AbilityPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RecallerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RecallerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, VulturePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(VulturePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, JesterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(JesterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, InfectedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(InfectedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, PathogenPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PathogenPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BomberPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BomberPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AssassinPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AssassinPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, CorruptCopPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CorruptCopPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ReporterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ReporterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SerialKillerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SerialKillerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, IronManPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(IronManPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, TaotiePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(TaotiePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SwallowedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SwallowedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SilencedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SilencedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SilencerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SilencerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BodyguardPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BodyguardPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, NoisemakerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(NoisemakerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SurvivalMasterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SurvivalMasterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, EngineerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(EngineerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, CriminalReasonerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CriminalReasonerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RiotPatrolPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RiotPatrolPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, HunterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(HunterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, OrthopedistPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(OrthopedistPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, FerrymanPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(FerrymanPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, GangsterStarPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(GangsterStarPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, CommanderPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CommanderPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BreacherPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BreacherPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, JunkerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(JunkerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, EconomistPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(EconomistPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SaintPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SaintPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, LooseEndPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(LooseEndPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, HallucinationPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(HallucinationPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DeathArenaPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.ALWAYS_COPY).end(DeathArenaPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, KillerHistoryPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.ALWAYS_COPY).end(KillerHistoryPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RoleHistoryPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.ALWAYS_COPY).end(RoleHistoryPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AntidoteShieldPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AntidoteShieldPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DreamEaterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DreamEaterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BanditPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BanditPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, PuppeteerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PuppeteerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, MengPoPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MengPoPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, MengPoSoupPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MengPoSoupPlayerComponent::new);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry worldComponentFactoryRegistry) {
        worldComponentFactoryRegistry.register(ConfigWorldComponent.KEY, ConfigWorldComponent::new);
        worldComponentFactoryRegistry.register(BodyForgeryWorldComponent.KEY, BodyForgeryWorldComponent::new);
        worldComponentFactoryRegistry.register(WorldMusicComponent.KEY, WorldMusicComponent::new);
        worldComponentFactoryRegistry.register(HiddenBodiesWorldComponent.KEY, HiddenBodiesWorldComponent::new);
        worldComponentFactoryRegistry.register(JunkerLootWorldComponent.KEY, JunkerLootWorldComponent::new);
        worldComponentFactoryRegistry.register(LooseEndsRadarWorldComponent.KEY, LooseEndsRadarWorldComponent::new);
        worldComponentFactoryRegistry.register(DeathArenaWorldComponent.KEY, DeathArenaWorldComponent::new);
        worldComponentFactoryRegistry.register(MurderMayhemWorldComponent.KEY, MurderMayhemWorldComponent::new);
        worldComponentFactoryRegistry.register(DualRoleSelectionWorldComponent.KEY, DualRoleSelectionWorldComponent::new);
        worldComponentFactoryRegistry.register(AntidotePlateShieldComponent.KEY, AntidotePlateShieldComponent::new);
        worldComponentFactoryRegistry.register(SleepingPillPlateComponent.KEY, SleepingPillPlateComponent::new);
        worldComponentFactoryRegistry.register(PuppetWorldComponent.KEY, PuppetWorldComponent::new);
    }
}

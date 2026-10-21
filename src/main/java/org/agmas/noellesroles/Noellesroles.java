package org.agmas.noellesroles;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleAppearanceCondition;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheMapEffects;
import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.bartender.CocktailRegistry;
import dev.doctor4t.wathe.api.event.*;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.config.datapack.MapRegistry;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import dev.doctor4t.wathe.record.replay.DefaultReplayFormatters;
import dev.doctor4t.wathe.record.replay.ReplayEventFormatter;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import dev.doctor4t.wathe.util.Scheduler;
import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderShopHandler;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.deatharena.DeathArenaPlayerComponent;
import org.agmas.noellesroles.deatharena.DeathArenaServerController;
import org.agmas.noellesroles.deatharena.DeathArenaStateHelper;
import org.agmas.noellesroles.deatharena.DeathArenaWorldComponent;
import org.agmas.noellesroles.morphling.BodyForgeryWorldComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingScramblePlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingShopHandler;
import org.agmas.noellesroles.item.ForgedPotionItem;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;
import org.agmas.noellesroles.packet.AttendantEditInfoC2SPacket;
import org.agmas.noellesroles.packet.OpenAttendantRegisterS2CPacket;
import org.agmas.noellesroles.packet.DeathArenaToggleC2SPacket;
import org.agmas.noellesroles.packet.EngineerDoorHighlightS2CPacket;
import org.agmas.noellesroles.packet.PhonismC2SPacket;
import org.agmas.noellesroles.packet.FerrymanBodyAgeSyncS2CPacket;
import org.agmas.noellesroles.packet.ForgedPotionSelectC2SPacket;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.agmas.noellesroles.packet.MorphCorpseToggleC2SPacket;
import org.agmas.noellesroles.packet.OpenForgedPotionSelectS2CPacket;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.agmas.noellesroles.packet.PuppeteerActionC2SPacket;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.puppeteer.PuppetRecord;
import org.agmas.noellesroles.puppeteer.PuppetWorldComponent;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import org.agmas.noellesroles.puppeteer.PuppeteerPlayerComponent;
import org.agmas.noellesroles.puppeteer.PuppeteerShopHandler;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.agmas.noellesroles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooHelper;
import org.agmas.noellesroles.voodoo.VoodooShopHandler;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.killerhistory.KillerHistoryCalculator;
import org.agmas.noellesroles.killerhistory.KillerHistoryPlayerComponent;
import org.agmas.noellesroles.rolehistory.RoleHistoryPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndsRadarWorldComponent;
import org.agmas.noellesroles.murdermayhem.MurderMayhemEvent;
import org.agmas.noellesroles.murdermayhem.MurderMayhemEventRegistry;
import org.agmas.noellesroles.murdermayhem.MurderMayhemGameMode;
import org.agmas.noellesroles.murdermayhem.MurderMayhemHelper;
import org.agmas.noellesroles.murdermayhem.MurderMayhemWorldComponent;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.pathogen.PathogenPlayerComponent;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;
import org.agmas.noellesroles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.orthopedist.OrthopedistPlayerComponent;
import org.agmas.noellesroles.commander.CommanderPlayerComponent;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.breacher.BreacherPlayerComponent;
import org.agmas.noellesroles.dreameater.DreamEaterKillRewardHelper;
import org.agmas.noellesroles.dreameater.DreamEaterPlayerComponent;
import org.agmas.noellesroles.dreameater.DreamEaterShopHandler;
import org.agmas.noellesroles.dreameater.SleepingPillHelper;
import org.agmas.noellesroles.dreameater.SleepingPillPlateComponent;
import org.agmas.noellesroles.phonism.PhonismSound;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.agmas.noellesroles.packet.DualRoleChoiceC2SPacket;
import org.agmas.noellesroles.packet.DualRoleOptionsS2CPacket;
import org.agmas.noellesroles.packet.EconomistReportS2CPacket;
import org.agmas.noellesroles.packet.OpenVoodooPanelS2CPacket;
import org.agmas.noellesroles.packet.OpenAssassinBookPanelS2CPacket;
import org.agmas.noellesroles.assassin.AssassinBookHelper;
import org.agmas.noellesroles.assassin.AssassinShopHandler;
import org.agmas.noellesroles.item.AssassinBookItem;
import org.agmas.noellesroles.economist.EconomistPlayerComponent;
import org.agmas.noellesroles.economist.EconomistShopHandler;
import org.agmas.noellesroles.packet.JunkerSearchC2SPacket;
import org.agmas.noellesroles.junker.JunkerLootDestroyHandler;
import org.agmas.noellesroles.junker.JunkerLootHelper;
import org.agmas.noellesroles.junker.JunkerLootWorldComponent;
import org.agmas.noellesroles.junker.JunkerPlayerComponent;
import org.agmas.noellesroles.junker.JunkerSearchHelper;
import org.agmas.noellesroles.commander.CommanderShopHandler;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.bomber.BomberShopHandler;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.bandit.BanditPlayerComponent;
import org.agmas.noellesroles.bandit.BanditSkillHelper;
import org.agmas.noellesroles.baosang.BaosangPlayerComponent;
import org.agmas.noellesroles.mengpo.MengPoHelper;
import org.agmas.noellesroles.mengpo.MengPoPlayerComponent;
import org.agmas.noellesroles.mengpo.MengPoSoupPlayerComponent;
import org.agmas.noellesroles.packet.MengPoChoiceC2SPacket;
import org.agmas.noellesroles.toxicologist.AntidotePlateShieldComponent;
import org.agmas.noellesroles.toxicologist.AntidoteShieldPlayerComponent;
import org.agmas.noellesroles.scavenger.HiddenBodiesWorldComponent;
import org.agmas.noellesroles.scavenger.ScavengerShopHandler;
import org.agmas.noellesroles.timekeeper.TimekeeperShopHandler;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.packet.ReporterMarkC2SPacket;
import org.agmas.noellesroles.packet.RoleBroadcastS2CPacket;
import org.agmas.noellesroles.packet.CommanderMarkC2SPacket;
import org.agmas.noellesroles.packet.SpectatorInfoRequestC2SPacket;
import org.agmas.noellesroles.packet.SpectatorInfoSyncS2CPacket;
import org.agmas.noellesroles.packet.SpectatorAssistTeleportC2SPacket;
import org.agmas.noellesroles.packet.SpectatorReplayDetailRequestC2SPacket;
import org.agmas.noellesroles.packet.SpectatorReplayDetailSyncS2CPacket;
import org.agmas.noellesroles.packet.ServerConfigUpdateC2SPacket;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterShopHandler;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerShopHandler;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.agmas.noellesroles.packet.TaotieSwallowC2SPacket;
import org.agmas.noellesroles.packet.SilencerSilenceC2SPacket;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.silencer.SilencerPlayerComponent;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.criminalreasoner.CriminalReasonerPlayerComponent;
import org.agmas.noellesroles.packet.CriminalReasonerInvisibilityC2SPacket;
import org.agmas.noellesroles.packet.CriminalReasonerReasonC2SPacket;
import org.agmas.noellesroles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.ferryman.FerrymanHelper;
import org.agmas.noellesroles.ferryman.FerrymanPlayerComponent;
import org.agmas.noellesroles.gangsterstar.GangsterStarPlayerComponent;
import org.agmas.noellesroles.gangsterstar.GangsterStarShopHandler;
import org.agmas.noellesroles.hallucination.HallucinationEffectId;
import org.agmas.noellesroles.hallucination.HallucinationHelper;
import org.agmas.noellesroles.hallucination.KillRewardContext;
import org.agmas.noellesroles.hallucination.KillRewardResolver;
import org.agmas.noellesroles.hallucination.KillRewardResult;
import org.agmas.noellesroles.hallucination.HallucinationPlayerComponent;
import org.agmas.noellesroles.hallucination.HallucinationShopHandler;
import org.agmas.noellesroles.commander.CommanderHelper;
import org.agmas.noellesroles.saint.SaintHelper;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownEntryAccessor;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownManagerAccessor;
import org.agmas.noellesroles.packet.HallucinationDummyHitC2SPacket;
import org.agmas.noellesroles.packet.HallucinationDummyUseC2SPacket;
import org.agmas.noellesroles.packet.HallucinationDummyStateS2CPacket;
import org.agmas.noellesroles.packet.HallucinationSoundS2CPacket;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.agmas.noellesroles.util.MisKillGunCooldownHelper;
import org.agmas.noellesroles.util.OpeningKnockbackResistanceHelper;
import org.agmas.noellesroles.util.PushAttributionTracker;
import org.agmas.noellesroles.util.PassengerDeathCompensationHelper;
import org.agmas.noellesroles.util.RoleUtils;
import org.agmas.noellesroles.util.SpectatorStateHelper;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;
import org.agmas.noellesroles.vulture.VultureHelper;
import org.agmas.noellesroles.walkie.WalkieTalkieChannels;
import org.agmas.noellesroles.item.RepairToolItem;
import org.agmas.noellesroles.music.WorldMusicComponent;
import org.agmas.noellesroles.poisoner.PoisonerShopHandler;
import org.agmas.noellesroles.bandit.BanditShopHandler;
import org.agmas.noellesroles.hunter.HunterShopHandler;
import org.agmas.noellesroles.looseend.LooseEndShopHandler;
import org.agmas.noellesroles.silencer.SilencerShopHandler;
import org.agmas.noellesroles.survivalmaster.SurvivalMasterPlayerComponent;
import org.agmas.noellesroles.voice.NoellesrolesVoiceChatPlugin;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.awt.*;
import java.util.*;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class Noellesroles implements ModInitializer {
    private static final int MURDER_MAYHEM_INTRO_TOTAL_TICKS = 380;
    public static String MOD_ID = "noellesroles";


    public static Identifier MORPHLING_ID = Identifier.of(MOD_ID, "morphling");
    public static Identifier CONDUCTOR_ID = Identifier.of(MOD_ID, "conductor");
    public static Identifier BARTENDER_ID = Identifier.of(MOD_ID, "bartender");
    public static Identifier NOISEMAKER_ID = Identifier.of(MOD_ID, "noisemaker");
    public static Identifier PHANTOM_ID = Identifier.of(MOD_ID, "phantom");
    public static Identifier AWESOME_BINGLUS_ID = Identifier.of(MOD_ID, "awesome_binglus");
    public static Identifier SWAPPER_ID = Identifier.of(MOD_ID, "swapper");
    public static Identifier VOODOO_ID = Identifier.of(MOD_ID, "voodoo");
    public static Identifier CORONER_ID = Identifier.of(MOD_ID, "coroner");
    public static Identifier RECALLER_ID = Identifier.of(MOD_ID, "recaller");
    public static Identifier VULTURE_ID = Identifier.of(MOD_ID, "vulture");
    public static Identifier THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID = Identifier.of(MOD_ID, "the_insane_damned_paranoid_killer");
    public static Identifier TIMEKEEPER_ID = Identifier.of(MOD_ID, "time_keeper");
    public static Identifier UNDERCOVER_ID = Identifier.of(MOD_ID, "undercover");
    public static Identifier TOXICOLOGIST_ID = Identifier.of(MOD_ID, "toxicologist");
    public static Identifier JESTER_ID = Identifier.of(MOD_ID, "jester");
    public static Identifier CORRUPT_COP_ID = Identifier.of(MOD_ID, "corrupt_cop");
    public static Identifier PATHOGEN_ID = Identifier.of(MOD_ID, "pathogen");
    public static Identifier BOMBER_ID = Identifier.of(MOD_ID, "bomber");
    public static Identifier ASSASSIN_ID = Identifier.of(MOD_ID, "assassin");
    public static Identifier SCAVENGER_ID = Identifier.of(MOD_ID, "scavenger");
    public static Identifier REPORTER_ID = Identifier.of(MOD_ID, "reporter");
    public static Identifier SERIAL_KILLER_ID = Identifier.of(MOD_ID, "serial_killer");
    public static Identifier PROFESSOR_ID = Identifier.of(MOD_ID, "professor");
    public static Identifier ATTENDANT_ID = Identifier.of(MOD_ID, "attendant");
    public static Identifier TAOTIE_ID = Identifier.of(MOD_ID, "taotie");
    public static Identifier SILENCER_ID = Identifier.of(MOD_ID, "silencer");
    public static Identifier BODYGUARD_ID = Identifier.of(MOD_ID, "bodyguard");
    public static Identifier POISONER_ID = Identifier.of(MOD_ID, "poisoner");
    public static Identifier BANDIT_ID = Identifier.of(MOD_ID, "bandit");
    public static Identifier SURVIVAL_MASTER_ID = Identifier.of(MOD_ID, "survival_master");
    public static Identifier ENGINEER_ID = Identifier.of(MOD_ID, "engineer");
    public static Identifier CRIMINAL_REASONER_ID = Identifier.of(MOD_ID, "criminal_reasoner");
    public static Identifier RIOT_PATROL_ID = Identifier.of(MOD_ID, "riot_patrol");
    public static Identifier HUNTER_ID = Identifier.of(MOD_ID, "hunter");
    public static Identifier ORTHOPEDIST_ID = Identifier.of(MOD_ID, "orthopedist");
    public static Identifier FERRYMAN_ID = Identifier.of(MOD_ID, "ferryman");
    public static Identifier GANGSTER_STAR_ID = Identifier.of(MOD_ID, "gangster_star");
    public static Identifier COMMANDER_ID = Identifier.of(MOD_ID, "commander");
    public static Identifier BREACHER_ID = Identifier.of(MOD_ID, "breacher");
    public static Identifier PHONISM_ID = Identifier.of(MOD_ID, "phonism");
    public static Identifier DREAM_EATER_ID = Identifier.of(MOD_ID, "dream_eater");
    public static Identifier PUPPETEER_ID = Identifier.of(MOD_ID, "puppeteer");
    public static Identifier SAINT_ID = Identifier.of(MOD_ID, "saint");
    public static Identifier JUNKER_ID = Identifier.of(MOD_ID, "junker");
    public static Identifier WHITE_KNIGHT_ID = Identifier.of(MOD_ID, "white_knight");
    public static Identifier ECONOMIST_ID = Identifier.of(MOD_ID, "economist");
    // 报丧角色 - 乘客阵营，有人死亡后延迟公布死讯
    public static Identifier BAOSANG_ID = Identifier.of(MOD_ID, "baosang");
    public static Identifier MENGPO_ID = Identifier.of(MOD_ID, "mengpo");
    public static final Identifier MURDER_MAYHEM_ID = Identifier.of(MOD_ID, "murder_mayhem");
    public static final dev.doctor4t.wathe.api.GameMode MURDER_MAYHEM =
            WatheGameModes.registerGameMode(MURDER_MAYHEM_ID, new MurderMayhemGameMode(MURDER_MAYHEM_ID));

    // 炸弹死亡原因
    public static Identifier DEATH_REASON_BOMB = Identifier.of(MOD_ID, "bomb");
    // 刺客死亡原因
    public static Identifier DEATH_REASON_ASSASSINATED = Identifier.of(MOD_ID, "assassinated");  // 被刺客猜中身份
    public static Identifier DEATH_REASON_ASSASSIN_MISFIRE = Identifier.of(MOD_ID, "assassin_misfire");  // 刺客猜错自己死亡
    public static Identifier DEATH_REASON_JESTER_TIMEOUT = Identifier.of(MOD_ID, "jester_timeout");
    // 饕餮吞噬死亡原因（游戏结束时被消化）
    public static Identifier DEATH_REASON_DIGESTED = Identifier.of(MOD_ID, "digested");
    // 饕餮饥饿值归零死亡原因
    public static Identifier DEATH_REASON_STARVED = Identifier.of(MOD_ID, "starved");
    // 投掷斧死亡原因
    public static Identifier DEATH_REASON_THROWING_AXE = Identifier.of(MOD_ID, "throwing_axe");
    public static Identifier DEATH_REASON_COMMANDER_SUICIDE = Identifier.of(MOD_ID, "commander_suicide");
    public static Identifier DEATH_REASON_GANGSTER_SPIRIT_TIMEOUT = Identifier.of(MOD_ID, "gangster_spirit_timeout");
    // 巫毒诅咒死亡原因
    public static Identifier DEATH_REASON_VOODOO = Identifier.of(MOD_ID, "voodoo");
    // 连环杀手死亡倒计时结束（疯魔反噬）
    public static Identifier DEATH_REASON_SERIAL_KILLER_DOOM = Identifier.of(MOD_ID, "serial_killer_doom");
    // 噬梦者：三击命中后完成睡觉任务，于睡梦中死亡
    public static Identifier DEATH_REASON_DREAM_EATER_SLEEP = Identifier.of(MOD_ID, "dream_eater_sleep");

    // 犯罪推理学家：这些死因在「无人担责」时把"凶手"视为死者本人（自行坠落出列车/小脑/精神崩溃/退出游戏）。
    // 若死者是被他人推下列车，killer 就是推人者（原版 lastAttacker，过期后由 PushAttributionTracker 补上），不再视为自杀。
    private static final Set<Identifier> CRIMINAL_REASONER_SELF_ATTRIBUTED_DEATHS = Set.of(
            GameConstants.DeathReasons.FELL_OUT_OF_TRAIN,
            GameConstants.DeathReasons.SHOT_INNOCENT,
            GameConstants.DeathReasons.MENTAL_BREAKDOWN,
            GameConstants.DeathReasons.ESCAPED
    );

    // 下毒来源
    public static Identifier POISON_SOURCE_NEEDLE = Identifier.of(MOD_ID, "needle");
    public static Identifier POISON_SOURCE_GAS_BOMB = Identifier.of(MOD_ID, "gas_bomb");
    public static Identifier POISON_SOURCE_TRAP = Identifier.of(MOD_ID, "trap");

    public static Role SWAPPER = WatheRoles.registerRole(new Role(SWAPPER_ID, new Color(57, 4, 170).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    public static Role PHANTOM = WatheRoles.registerRole(new Role(PHANTOM_ID, new Color(80, 5, 5, 192).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    public static Role MORPHLING = WatheRoles.registerRole(new Role(MORPHLING_ID, new Color(170, 2, 61).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    public static Role THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES = WatheRoles.registerRole(new Role(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID, new Color(255, 0, 0, 192).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 炸弹客角色 - 杀手阵营，无法购买刀和枪，只能用炸弹
    public static Role BOMBER = WatheRoles.registerRole(new Role(BOMBER_ID, new Color(50, 50, 50).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 刺客角色 - 杀手阵营，可以猜测玩家身份
    public static Role ASSASSIN = WatheRoles.registerRole(new Role(ASSASSIN_ID, new Color(139, 0, 0).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 清道夫角色 - 杀手阵营，杀人后尸体对其他人不可见（秃鹫和中立除外），杀人奖励+50金币，只能买刀，可以花100金币重置刀CD
    public static Role SCAVENGER = WatheRoles.registerRole(new Role(SCAVENGER_ID, new Color(101, 67, 33).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 连环杀手角色 - 杀手阵营，商店限购，刀冷却减半，技能进入无护盾疯魔模式并开启死亡倒计时
    public static Role SERIAL_KILLER = WatheRoles.registerRole(new Role(SERIAL_KILLER_ID, new Color(102, 34, 34).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 静语者角色 - 杀手阵营，可以让目标无法使用voicechat说话，且无法听到他人说话，持续60秒，冷却45秒
    public static Role SILENCER = WatheRoles.registerRole(new Role(SILENCER_ID, new Color(80, 70, 110).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 毒师角色 - 杀手阵营，使用毒针和毒气弹
    public static Role POISONER = WatheRoles.registerRole(new Role(POISONER_ID, new Color(30, 80, 20).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 强盗角色 - 杀手阵营，使用投掷斧远程贯穿击杀
    public static Role BANDIT = WatheRoles.registerRole(new Role(BANDIT_ID, new Color(90, 100, 40).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    public static Role HUNTER = WatheRoles.registerRole(new Role(HUNTER_ID, new Color(92, 76, 52).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    public static Role COMMANDER = WatheRoles.registerRole(new Role(COMMANDER_ID, new Color(46, 0, 107).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true, RoleAppearanceCondition.minKillers(3)));
    public static Role BREACHER = WatheRoles.registerRole(new Role(BREACHER_ID, new Color(120, 30, 30).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 谎言音效师角色 - 杀手阵营，选定玩家与音效，以该玩家为中心伪造音效
    public static Role PHONISM = WatheRoles.registerRole(new Role(PHONISM_ID, new Color(160, 130, 210).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 噬梦者角色 - 杀手阵营，标记玩家使其陷入困意，第三次命中后令其在睡梦中死亡
    public static Role DREAM_EATER = WatheRoles.registerRole(new Role(DREAM_EATER_ID, new Color(80, 70, 190).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 傀儡师角色 - 杀手阵营，把未化白骨的尸体做成傀儡并操纵它们行动；至少2名杀手时才会出场
    public static Role PUPPETEER = WatheRoles.registerRole(new Role(PUPPETEER_ID, new Color(46, 150, 140).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true, RoleAppearanceCondition.minKillers(2)));


    public static HashMap<Role, RoleAnnouncementTexts.RoleAnnouncementText> roleRoleAnnouncementTextHashMap = new HashMap<>();
    public static Role TIMEKEEPER = WatheRoles.registerRole(new Role(TIMEKEEPER_ID, new Color(0, 38, 255).getRGB(), true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), true));
    public static Role UNDERCOVER = WatheRoles.registerRole(new Role(UNDERCOVER_ID, new Color(192, 192, 192).getRGB(), true, false, Role.MoodType.NONE, GameConstants.getInTicks(0, 10), false, RoleAppearanceCondition.minKillers(2)));
    public static Role CONDUCTOR = WatheRoles.registerRole(new Role(CONDUCTOR_ID, new Color(255, 205, 84).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role AWESOME_BINGLUS = WatheRoles.registerRole(new Role(AWESOME_BINGLUS_ID, new Color(155, 255, 168).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role BARTENDER = WatheRoles.registerRole(new Role(BARTENDER_ID, new Color(217, 241, 240).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role NOISEMAKER = WatheRoles.registerRole(new Role(NOISEMAKER_ID, new Color(200, 255, 0).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role VOODOO = WatheRoles.registerRole(new Role(VOODOO_ID, new Color(128, 114, 253).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role CORONER = WatheRoles.registerRole(new Role(CORONER_ID, new Color(122, 122, 122).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role RECALLER = WatheRoles.registerRole(new Role(RECALLER_ID, new Color(158, 255, 255).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role TOXICOLOGIST = WatheRoles.registerRole(new Role(TOXICOLOGIST_ID, new Color(184, 41, 90).getRGB(), true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), false));
    // 记者角色 - 无辜者阵营，可以标记一个玩家并透视他
    public static Role REPORTER = WatheRoles.registerRole(new Role(REPORTER_ID, new Color(210, 180, 100).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 教授角色 - 无辜者阵营，开局自带铁人药剂，可以保护其他玩家
    public static Role PROFESSOR = WatheRoles.registerRole(new Role(PROFESSOR_ID, new Color(70, 130, 180).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 乘务员角色 - 无辜者阵营，开局获得一本书记录所有房间的乘客
    public static Role ATTENDANT = WatheRoles.registerRole(new Role(ATTENDANT_ID, new Color(100, 149, 237).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 保镖角色 - 无辜者阵营，空手按技能键进入潜行姿态，正面格挡刀刺与球棒击打
    public static Role BODYGUARD = WatheRoles.registerRole(new Role(BODYGUARD_ID, new Color(70, 130, 250).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 生存大师角色 - 无辜者阵营，无法被杀手本能察觉，触发生存时刻后杀手必须在120秒内找到并杀死他
    public static Role SURVIVAL_MASTER = WatheRoles.registerRole(new Role(SURVIVAL_MASTER_ID, new Color(50, 180, 160).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 工程师角色 - 无辜者阵营，感知被撬/被锁的门，维修工具修复/上锁/解锁
    public static Role ENGINEER = WatheRoles.registerRole(new Role(ENGINEER_ID, new Color(200, 160, 60).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role RIOT_PATROL = WatheRoles.registerRole(new Role(RIOT_PATROL_ID, new Color(45, 95, 145).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false, ctx -> false));
    public static Role ORTHOPEDIST = WatheRoles.registerRole(new Role(ORTHOPEDIST_ID, new Color(144, 179, 88).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role SAINT = WatheRoles.registerRole(new Role(SAINT_ID, new Color(238, 188, 120).getRGB(), true, false, Role.MoodType.NONE, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 报丧角色 - 乘客阵营，有人死亡后延迟10秒公布死讯，冷却30秒
    public static Role BAOSANG = WatheRoles.registerRole(new Role(BAOSANG_ID, new Color(230, 145, 70).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 拾荒者角色 - 乘客阵营，常亮透视8格内掉落物，注视尸体5秒后随机获得一件死者物品
    public static Role JUNKER = WatheRoles.registerRole(new Role(JUNKER_ID, new Color(145, 135, 110).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 白骑士角色 - 警类乘客阵营，概率替换义警出场，开局带左轮（警类10秒冷却），死亡时对杀手阵营有特殊报酬规则，可透视疯魔玩家并无视其护盾
    public static Role WHITE_KNIGHT = WatheRoles.registerRole(new Role(WHITE_KNIGHT_ID, new Color(232, 232, 240).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false, ctx -> false));
    // 经济学家角色 - 乘客阵营，靠任务赚钱，商店出售可观测他人金币的《经济财报》
    public static Role ECONOMIST = WatheRoles.registerRole(new Role(ECONOMIST_ID, new Color(60, 125, 110).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 孟婆角色 - 乘客阵营，开局选择渡人（他人可向其买孟婆汤）或渡己（立即转为随机乘客职业），孟婆汤60秒后令饮者转职，无法捡枪
    public static Role MENGPO = WatheRoles.registerRole(new Role(MENGPO_ID, new Color(168, 139, 181).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));


    // 小丑角色 - 中立阵营，被无辜者杀死时获胜
    public static Role JESTER = WatheRoles.registerRole(new Role(JESTER_ID, new Color(248, 200, 220).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role VULTURE = WatheRoles.registerRole(new Role(VULTURE_ID, new Color(181, 103, 0).getRGB(), false, false, Role.MoodType.FAKE, -1, false, VultureHelper::canSpawn));
    // 黑警角色 - 中立阵营，杀光所有人获胜，阻止其他阵营获胜
    public static Role CORRUPT_COP = WatheRoles.registerRole(new Role(CORRUPT_COP_ID, new Color(25, 50, 100).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), true));
    public static Role GANGSTER_STAR = WatheRoles.registerRole(new Role(GANGSTER_STAR_ID, new Color(74, 20, 140).getRGB(), false, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), true, ctx -> !ctx.isRoleAssigned(CORRUPT_COP)));
    // 病原体角色 - 中立阵营，感染所有存活玩家获胜
    public static Role PATHOGEN = WatheRoles.registerRole(new Role(PATHOGEN_ID, 0x7FFF00, false, false, Role.MoodType.FAKE, Integer.MAX_VALUE, false));
    // 饕餮角色 - 中立阵营，吞噬玩家获胜
    public static Role TAOTIE = WatheRoles.registerRole(new Role(TAOTIE_ID, new Color(139, 69, 19).getRGB(), false, false, Role.MoodType.FAKE, Integer.MAX_VALUE, false));
    // 犯罪推理学家角色 - 中立阵营，将死者与凶手正确匹配获胜
    public static Role CRIMINAL_REASONER = WatheRoles.registerRole(new Role(CRIMINAL_REASONER_ID, new Color(112, 75, 75).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role FERRYMAN = WatheRoles.registerRole(new Role(FERRYMAN_ID, new Color(129, 170, 196).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), false, FerrymanHelper::canSpawn));

    // All registered custom roles for dual role selection iteration
    public static final List<Role> ALL_ROLES = new ArrayList<>();

    public static final CustomPayload.Id<MorphC2SPacket> MORPH_PACKET = MorphC2SPacket.ID;
    public static final CustomPayload.Id<SwapperC2SPacket> SWAP_PACKET = SwapperC2SPacket.ID;
    public static final CustomPayload.Id<AbilityC2SPacket> ABILITY_PACKET = AbilityC2SPacket.ID;
    public static final CustomPayload.Id<VultureEatC2SPacket> VULTURE_PACKET = VultureEatC2SPacket.ID;
    public static final CustomPayload.Id<AssassinGuessRoleC2SPacket> ASSASSIN_GUESS_ROLE_PACKET = AssassinGuessRoleC2SPacket.ID;
    public static final CustomPayload.Id<PhonismC2SPacket> PHONISM_PACKET = PhonismC2SPacket.ID;
    public static final CustomPayload.Id<ReporterMarkC2SPacket> REPORTER_MARK_PACKET = ReporterMarkC2SPacket.ID;
    public static final CustomPayload.Id<CommanderMarkC2SPacket> COMMANDER_MARK_PACKET = CommanderMarkC2SPacket.ID;
    public static final CustomPayload.Id<CriminalReasonerReasonC2SPacket> CRIMINAL_REASONER_REASON_PACKET = CriminalReasonerReasonC2SPacket.ID;
    public static final CustomPayload.Id<CriminalReasonerInvisibilityC2SPacket> CRIMINAL_REASONER_INVISIBILITY_PACKET = CriminalReasonerInvisibilityC2SPacket.ID;
    public static final CustomPayload.Id<TaotieSwallowC2SPacket> TAOTIE_SWALLOW_PACKET = TaotieSwallowC2SPacket.ID;
    public static final CustomPayload.Id<SilencerSilenceC2SPacket> SILENCER_SILENCE_PACKET = SilencerSilenceC2SPacket.ID;
    public static final CustomPayload.Id<DualRoleChoiceC2SPacket> DUAL_ROLE_CHOICE_PACKET = DualRoleChoiceC2SPacket.ID;
    public static final CustomPayload.Id<AttendantEditInfoC2SPacket> ATTENDANT_EDIT_PACKET = AttendantEditInfoC2SPacket.ID;
    public static final ArrayList<Role> VANNILA_ROLES = new ArrayList<>();
    public static final ArrayList<Identifier> VANNILA_ROLE_IDS = new ArrayList<>();
    // 中立万能钥匙可用角色集合
    private static final Set<Role> NEUTRAL_MASTER_KEY_ROLES = Set.of(VULTURE, PATHOGEN, TAOTIE, FERRYMAN, CRIMINAL_REASONER);
    private static final int TRAP_DISMANTLE_SHARED_COOLDOWN_TICKS = 15 * 20;
    private static final int MOMENT_TRIGGER_MIN_THRESHOLD = 2;
    // Static helpers have been moved to:
    // - FerrymanHelper, VultureHelper, SaintHelper, CommanderHelper, RoleUtils

    private static final Set<Item> TRAP_DISMANTLE_EXTRA_COOLDOWN_ITEMS = Set.of(
        ModItems.ANTIDOTE,
        ModItems.IRON_MAN_VIAL,
        ModItems.POISON_NEEDLE,
        ModItems.DOUBLE_BARREL_SHOTGUN,
        ModItems.REPAIR_TOOL,
        ModItems.RIOT_SHIELD,
        ModItems.RIOT_FORK,
        ModItems.TIMED_BOMB,
        ModItems.NEUTRAL_MASTER_KEY
    );

    private static boolean isTrapDismantleCooldownItem(Item item) {
        return GameConstants.ITEM_COOLDOWNS.containsKey(item)
            || TRAP_DISMANTLE_EXTRA_COOLDOWN_ITEMS.contains(item);
    }

    public static boolean isVigilanteSlotRole(Role role) {
        return role == WatheRoles.VIGILANTE
            || role == WatheRoles.VETERAN
            || role == RIOT_PATROL
            || role == WHITE_KNIGHT;
    }

    /**
     * 双角色选择中的固定槽位角色：既不能作为玩家的备选被换出去，也不能被其他角色替换掉。
     * 用于保证义警类（VIGILANTE/VETERAN/RIOT_PATROL/白骑士）和列车长（CONDUCTOR）数量稳定。
     */
    public static boolean isFixedDualRoleSlot(Role role) {
        return role != null && (isVigilanteSlotRole(role) || role == CONDUCTOR);
    }

    /**
     * 只能由开局初始抽取获得的角色：不会出现在双角色选择的备选池里。
     */
    public static boolean isInitialDrawOnlyRole(Role role) {
        return role == CORRUPT_COP || role == GANGSTER_STAR;
    }

    public static boolean isPoliceRole(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role role = gameWorld.getRole(player);
        return role != null && isVigilanteSlotRole(role);
    }

    public static boolean isTrapDismantlerRole(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role role = gameWorld.getRole(player);
        return role == ENGINEER
            || role == CORRUPT_COP
            || (role != null && isVigilanteSlotRole(role) && role != WatheRoles.VIGILANTE);
    }

    private static int getRemainingCooldownTicks(PlayerEntity player, Item item) {
        if (!player.getItemCooldownManager().isCoolingDown(item)) {
            return 0;
        }

        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
        Object entry = accessor.getEntries().get(item);
        if (entry == null) {
            return 0;
        }

        return Math.max(0, ((ItemCooldownEntryAccessor) entry).getEndTick() - accessor.getTick());
    }

    private static void applyTrapDismantleCooldowns(PlayerEntity player) {
        Set<Item> cooldownItems = new HashSet<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || !isTrapDismantleCooldownItem(stack.getItem())) {
                continue;
            }
            cooldownItems.add(stack.getItem());
        }

        for (Item item : cooldownItems) {
            int cooldownTicks = Math.max(TRAP_DISMANTLE_SHARED_COOLDOWN_TICKS, getRemainingCooldownTicks(player, item));
            player.getItemCooldownManager().set(item, cooldownTicks);
        }
    }

    public static void checkAndTriggerMomentsForWorld(ServerWorld serverWorld) {
        if (serverWorld == null) return;
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);

        // 单次遍历统计存活且未被吞噬的玩家数
        int aliveNotSwallowed = RoleUtils.countAliveAndNotSwallowed(serverWorld);

        // 黑警时刻检查
        if (aliveNotSwallowed >= MOMENT_TRIGGER_MIN_THRESHOLD) {
            for (UUID uuid : gameComponent.getAllWithRole(CORRUPT_COP)) {
                PlayerEntity corruptCop = serverWorld.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(corruptCop) && GameFunctions.isPlayerAliveAndSurvival(corruptCop)
                        && !SwallowedPlayerComponent.isPlayerSwallowed(corruptCop)) {
                    CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(corruptCop);
                    corruptCopComp.checkAndTriggerMoment(aliveNotSwallowed);
                }
            }
        }

        // 饕餮时刻检查
        if (aliveNotSwallowed >= MOMENT_TRIGGER_MIN_THRESHOLD) {
            for (UUID uuid : gameComponent.getAllWithRole(TAOTIE)) {
                PlayerEntity taotie = serverWorld.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                    taotieComp.checkAndTriggerMoment(aliveNotSwallowed);
                }
            }
        }

        // 生存时刻检查（传入已计算的 aliveNotSwallowed 避免重复遍历）
        for (UUID uuid : gameComponent.getAllWithRole(SURVIVAL_MASTER)) {
            PlayerEntity survivalMaster = serverWorld.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerPlayingAndAlive(survivalMaster)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(survivalMaster);
                survivalComp.checkAndTriggerMoment(serverWorld, aliveNotSwallowed);
            }
        }
    }

    @Override
    public void onInitialize() {
        VANNILA_ROLES.add(WatheRoles.KILLER);
        VANNILA_ROLES.add(WatheRoles.VIGILANTE);
        VANNILA_ROLES.add(WatheRoles.CIVILIAN);
        VANNILA_ROLES.add(WatheRoles.LOOSE_END);
        VANNILA_ROLE_IDS.add(WatheRoles.LOOSE_END.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.VIGILANTE.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.CIVILIAN.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.KILLER.identifier());
        ALL_ROLES.add(SWAPPER); ALL_ROLES.add(PHANTOM); ALL_ROLES.add(MORPHLING);
        ALL_ROLES.add(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES);
        ALL_ROLES.add(BOMBER); ALL_ROLES.add(ASSASSIN); ALL_ROLES.add(SCAVENGER);
        ALL_ROLES.add(SERIAL_KILLER); ALL_ROLES.add(SILENCER); ALL_ROLES.add(POISONER);
        ALL_ROLES.add(BANDIT); ALL_ROLES.add(HUNTER); ALL_ROLES.add(COMMANDER);
        ALL_ROLES.add(BREACHER); ALL_ROLES.add(PHONISM); ALL_ROLES.add(DREAM_EATER);
        ALL_ROLES.add(PUPPETEER);
        ALL_ROLES.add(TIMEKEEPER); ALL_ROLES.add(UNDERCOVER); ALL_ROLES.add(CONDUCTOR);
        ALL_ROLES.add(AWESOME_BINGLUS); ALL_ROLES.add(BARTENDER); ALL_ROLES.add(NOISEMAKER);
        ALL_ROLES.add(VOODOO); ALL_ROLES.add(CORONER); ALL_ROLES.add(RECALLER);
        ALL_ROLES.add(TOXICOLOGIST); ALL_ROLES.add(REPORTER); ALL_ROLES.add(PROFESSOR);
        ALL_ROLES.add(ATTENDANT); ALL_ROLES.add(BODYGUARD); ALL_ROLES.add(SURVIVAL_MASTER);
        ALL_ROLES.add(ENGINEER); ALL_ROLES.add(RIOT_PATROL); ALL_ROLES.add(ORTHOPEDIST);
        ALL_ROLES.add(SAINT); ALL_ROLES.add(BAOSANG); ALL_ROLES.add(JUNKER);
        ALL_ROLES.add(WHITE_KNIGHT); ALL_ROLES.add(ECONOMIST); ALL_ROLES.add(MENGPO);
        ALL_ROLES.add(JESTER); ALL_ROLES.add(VULTURE); ALL_ROLES.add(CORRUPT_COP);
        ALL_ROLES.add(GANGSTER_STAR); ALL_ROLES.add(PATHOGEN); ALL_ROLES.add(TAOTIE);
        ALL_ROLES.add(CRIMINAL_REASONER); ALL_ROLES.add(FERRYMAN);
        NoellesRolesConfig.HANDLER.load();
        ModItems.init();
        ModParticles.init();
        SleepingPillHelper.init();
        JunkerLootHelper.init();
        ModSounds.init();
        ModEffects.init();

        // 金酒免疫关灯：有 GIN_IMMUNITY 效果时取消关灯失明
        dev.doctor4t.wathe.api.event.BlackoutEffect.BEFORE.register((player, durationTicks) -> {
            if (player.hasStatusEffect(ModEffects.GIN_IMMUNITY)) {
                return dev.doctor4t.wathe.api.event.BlackoutEffect.BlackoutResult.cancel();
            }
            return null;
        });
        PayloadTypeRegistry.playC2S().register(MorphC2SPacket.ID, MorphC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MorphCorpseToggleC2SPacket.ID, MorphCorpseToggleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityC2SPacket.ID, AbilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapperC2SPacket.ID, SwapperC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VultureEatC2SPacket.ID, VultureEatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AssassinGuessRoleC2SPacket.ID, AssassinGuessRoleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PhonismC2SPacket.ID, PhonismC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ReporterMarkC2SPacket.ID, ReporterMarkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CommanderMarkC2SPacket.ID, CommanderMarkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CriminalReasonerReasonC2SPacket.ID, CriminalReasonerReasonC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CriminalReasonerInvisibilityC2SPacket.ID, CriminalReasonerInvisibilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TaotieSwallowC2SPacket.ID, TaotieSwallowC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SilencerSilenceC2SPacket.ID, SilencerSilenceC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SpectatorInfoRequestC2SPacket.ID, SpectatorInfoRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SpectatorReplayDetailRequestC2SPacket.ID, SpectatorReplayDetailRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SpectatorAssistTeleportC2SPacket.ID, SpectatorAssistTeleportC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DeathArenaToggleC2SPacket.ID, DeathArenaToggleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(HallucinationDummyHitC2SPacket.ID, HallucinationDummyHitC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(HallucinationDummyUseC2SPacket.ID, HallucinationDummyUseC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DualRoleChoiceC2SPacket.ID, DualRoleChoiceC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(JunkerSearchC2SPacket.ID, JunkerSearchC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AttendantEditInfoC2SPacket.ID, AttendantEditInfoC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ServerConfigUpdateC2SPacket.ID, ServerConfigUpdateC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ForgedPotionSelectC2SPacket.ID, ForgedPotionSelectC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PuppeteerActionC2SPacket.ID, PuppeteerActionC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MengPoChoiceC2SPacket.ID, MengPoChoiceC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(EngineerDoorHighlightS2CPacket.ID, EngineerDoorHighlightS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(FerrymanBodyAgeSyncS2CPacket.ID, FerrymanBodyAgeSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(HallucinationDummyStateS2CPacket.ID, HallucinationDummyStateS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(HallucinationSoundS2CPacket.ID, HallucinationSoundS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RoleBroadcastS2CPacket.ID, RoleBroadcastS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SpectatorInfoSyncS2CPacket.ID, SpectatorInfoSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SpectatorReplayDetailSyncS2CPacket.ID, SpectatorReplayDetailSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DualRoleOptionsS2CPacket.ID, DualRoleOptionsS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(EconomistReportS2CPacket.ID, EconomistReportS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenVoodooPanelS2CPacket.ID, OpenVoodooPanelS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenAssassinBookPanelS2CPacket.ID, OpenAssassinBookPanelS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenAttendantRegisterS2CPacket.ID, OpenAttendantRegisterS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenForgedPotionSelectS2CPacket.ID, OpenForgedPotionSelectS2CPacket.CODEC);

        // 拾荒者：搜刮所得物品使用生效后销毁
        JunkerLootDestroyHandler.register();

        registerEvents();

        BartenderShopHandler.register();
        BomberShopHandler.register();
        MorphlingShopHandler.register();
        ScavengerShopHandler.register();
        TimekeeperShopHandler.register();
        PoisonerShopHandler.register();
        BanditShopHandler.register();
        ReporterShopHandler.register();
        HunterShopHandler.register();
        CommanderShopHandler.register();
        SilencerShopHandler.register();
        LooseEndShopHandler.register();
        HallucinationShopHandler.register();
        GangsterStarShopHandler.register();
        SerialKillerShopHandler.register();
        EconomistShopHandler.register();
        VoodooShopHandler.register();
        AssassinShopHandler.register();
        DreamEaterShopHandler.register();
        PuppeteerShopHandler.register();

        // 毒师手持毒针时允许攻击玩家
        AllowPlayerPunching.EVENT.register((attacker, victim) ->
                attacker.getMainHandStack().isOf(ModItems.POISON_NEEDLE)
                        || attacker.getMainHandStack().isOf(ModItems.RIOT_SHIELD)
        );

        // 注册职业聊天处理器（大嗓门广播、饕餮肚子、静语者拦截）
        org.agmas.noellesroles.chat.RoleChatHandler.register();

        // 注册 DLC 回放格式化器
        registerReplayFormatters();
        // 安眠药取餐回放显示蓝色「投药」：wathe 的默认餐盘拿取格式化器在其自身的 init 中注册，
        // 需等所有模组初始化完成（SERVER_STARTED）后再覆盖，避免被 wathe 后来居上重置
        ServerLifecycleEvents.SERVER_STARTED.register(server -> registerSleepingPillPlatterTakeFormatter());

        registerPackets();
        NoellesRolesEntities.init();

    }


    public void registerEvents() {

        ServerWorldEvents.LOAD.register((server, world) -> {
            GameWorldComponent.KEY.get(world).setRoleEnabled(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES, false);
            GameWorldComponent.KEY.get(world).setRoleEnabled(AWESOME_BINGLUS, false);
            GameWorldComponent.KEY.get(world).setRoleEnabled(JESTER, false);
            ConfigWorldComponent.KEY.get(world).reset();
            LooseEndsRadarWorldComponent.KEY.get(world).reset();
            LooseEndsRadarWorldComponent.KEY.sync(world);
            DeathArenaWorldComponent.KEY.get(world).reset();
            JunkerLootWorldComponent.KEY.get(world).reset();
            PuppetWorldComponent.KEY.get(world).reset();
            MurderMayhemWorldComponent.KEY.get(world).reset();
            MurderMayhemWorldComponent.KEY.sync(world);
        });

        // 修复：断线重连后清理语音群组 + 同步静语状态
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> {
                SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
                if (!swallowedComp.isSwallowed()) {
                    TrainVoicePlugin.addPlayer(player.getUuid());
                }
                ConfigWorldComponent.KEY.get(player.getServerWorld()).sync();
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // 傀儡师操纵中断线：先回到本体并换回本体物品栏，傀儡留在原地
            PuppeteerControl.returnToBody(player, false);
            server.execute(() -> {
                BreacherPlayerComponent.KEY.get(player).reset();
                DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(player.getWorld());
                if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.SELECTING) {
                    dsComp.handlePlayerDisconnect(player.getUuid());
                    DualRoleSelectionWorldComponent.KEY.sync(player.getWorld());
                }
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("fogradius")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(
                                org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent.MIN_FOG_RADIUS,
                                org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent.MAX_FOG_RADIUS
                        )).executes(context -> {
                            ServerWorld world = context.getSource().getWorld();
                            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
                            if (!MurderMayhemHelper.isMurderMayhem(gameComponent.getGameMode())) {
                                context.getSource().sendError(Text.literal("当前世界不是迷乱谋杀模式。"));
                                return 0;
                            }

                            MurderMayhemWorldComponent component = MurderMayhemWorldComponent.KEY.get(world);
                            if (!(component.getCurrentEvent() instanceof org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent)) {
                                context.getSource().sendError(Text.literal("当前事件不是战争迷雾。"));
                                return 0;
                            }

                            int radius = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "radius");
                            component.setFogRadius(radius);
                            MurderMayhemWorldComponent.KEY.sync(world);
                            context.getSource().sendFeedback(
                                    () -> Text.literal("已将当前对局迷雾半径调整为 " + radius + "。"),
                                    true
                            );
                            return 1;
                        }))
        ));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("hallucination")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("give")
                                .then(argument("targets", net.minecraft.command.argument.EntityArgumentType.players())
                                        .then(argument("effect", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(context -> {
                                                    var targets = net.minecraft.command.argument.EntityArgumentType.getPlayers(context, "targets");
                                                    String rawEffect = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "effect");
                                                    boolean all = "all".equalsIgnoreCase(rawEffect);
                                                    int changed = 0;
                                                    for (ServerPlayerEntity target : targets) {
                                                        HallucinationPlayerComponent component = HallucinationPlayerComponent.KEY.get(target);
                                                        if (all) {
                                                            for (HallucinationEffectId effectId : HallucinationEffectId.values()) {
                                                                if (component.canApply(effectId)) {
                                                                    HallucinationHelper.apply(target, target.getServerWorld(), GameWorldComponent.KEY.get(target.getWorld()), component, effectId);
                                                                    changed++;
                                                                }
                                                            }
                                                            component.sync();
                                                            continue;
                                                        }

                                                        HallucinationEffectId effectId = HallucinationEffectId.byId(rawEffect).orElse(null);
                                                        if (effectId != null && component.canApply(effectId)) {
                                                            HallucinationHelper.apply(target, target.getServerWorld(), GameWorldComponent.KEY.get(target.getWorld()), component, effectId);
                                                            component.sync();
                                                            changed++;
                                                        }
                                                    }
                                                    if (changed <= 0) {
                                                        context.getSource().sendError(Text.translatable("command.noellesroles.hallucination.no_change"));
                                                        return 0;
                                                    }
                                                    final int resultCount = changed;
                                                    context.getSource().sendFeedback(() -> Text.translatable("command.noellesroles.hallucination.give.success", resultCount), true);
                                                    return changed;
                                                }))))
                        .then(literal("clear")
                                .then(argument("targets", net.minecraft.command.argument.EntityArgumentType.players())
                                        .then(argument("effect", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(context -> {
                                                    var targets = net.minecraft.command.argument.EntityArgumentType.getPlayers(context, "targets");
                                                    String rawEffect = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "effect");
                                                    boolean all = "all".equalsIgnoreCase(rawEffect);
                                                    int changed = 0;
                                                    for (ServerPlayerEntity target : targets) {
                                                        HallucinationPlayerComponent component = HallucinationPlayerComponent.KEY.get(target);
                                                        if (all) {
                                                            if (component.clearAllHallucinations(false)) {
                                                                component.sync();
                                                                changed++;
                                                            }
                                                            continue;
                                                        }
                                                        HallucinationEffectId effectId = HallucinationEffectId.byId(rawEffect).orElse(null);
                                                        if (effectId != null && component.removeEffect(effectId)) {
                                                            component.sync();
                                                            changed++;
                                                        }
                                                    }
                                                    if (changed <= 0) {
                                                        context.getSource().sendError(Text.translatable("command.noellesroles.hallucination.no_change"));
                                                        return 0;
                                                    }
                                                    final int resultCount = changed;
                                                    context.getSource().sendFeedback(() -> Text.translatable("command.noellesroles.hallucination.clear.success", resultCount), true);
                                                    return changed;
                                                }))))
        ));

        // 动态杀手权重调试命令（与 Wathe 指令风格一致：wathe: 命名空间，op 2）
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("wathe:KillerHistory")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("set")
                                .then(argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                                        .executes(context -> setKillerHistory(context.getSource(),
                                                net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target"), 0))
                                        .then(argument("streak", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                                                .executes(context -> setKillerHistory(context.getSource(),
                                                        net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "streak"))))))
                        .then(literal("get")
                                .executes(context -> getKillerHistory(context.getSource(),
                                        context.getSource().getPlayerOrThrow()))
                                .then(argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                                        .executes(context -> getKillerHistory(context.getSource(),
                                                net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target")))))
        ));

        // 近3局角色历史调试命令（开局角色抽取避开规则使用）
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("wathe:RoleHistory")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("set")
                                .then(argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                                        .then(argument("roles", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                .executes(context -> setRoleHistory(context.getSource(),
                                                        net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target"),
                                                        com.mojang.brigadier.arguments.StringArgumentType.getString(context, "roles"))))))
                        .then(literal("get")
                                .executes(context -> getRoleHistory(context.getSource(),
                                        context.getSource().getPlayerOrThrow()))
                                .then(argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                                        .executes(context -> getRoleHistory(context.getSource(),
                                                net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target")))))
                        .then(literal("clear")
                                .executes(context -> clearRoleHistory(context.getSource(),
                                        context.getSource().getPlayerOrThrow()))
                                .then(argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                                        .executes(context -> clearRoleHistory(context.getSource(),
                                                net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target")))))
        ));

        // Master key should drop on death, except for Loose End's starting key
        ShouldDropOnDeath.EVENT.register((stack, victim) -> stack.isOf(ModItems.MASTER_KEY)
                && !GameWorldComponent.KEY.get(victim.getWorld()).isRole(victim, WatheRoles.LOOSE_END));

        // 傀儡师操纵傀儡时被杀：刀枪等物理伤害由傀儡承受（傀儡倒地、取消本次击杀），
        // 其余死因先回到本体再按正常流程结算。必须先于其他 BEFORE 逻辑，免得护盾等被白白消耗。
        KillPlayer.BEFORE.register(PuppeteerControl::handleBeforeKill);

        KillPlayer.BEFORE.register(((victim, killer, deathReason) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());
            if (victim instanceof ServerPlayerEntity serverVictim
                    && DeathArenaStateHelper.isDeathArenaParticipant(serverVictim)) {
                DeathArenaPlayerComponent.KEY.get(serverVictim).setPendingRespawn(true);
            }
            if (gameWorldComponent.isRole(victim, HUNTER)) {
                removeHunterShotgunDrops(victim);
            }
            if (gameWorldComponent.isRole(victim, WatheRoles.LOOSE_END)) {
                removeLooseEndDrops(victim);
            }
            if (gameWorldComponent.isRole(victim, GANGSTER_STAR)
                    && GangsterStarPlayerComponent.KEY.get(victim).isResolvingFinalStandDeath()) {
                return null;
            }

            // 黑警被杀时结束黑警时刻
            if (gameWorldComponent.isRole(victim, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(victim);
                if (corruptCopComp.isCorruptCopMomentActive() && deathReason == DEATH_REASON_ASSASSINATED) {
                    // 记录黑警时刻免疫刺客
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                                .actor(serverVictim)
                                .put("block_reason", "corrupt_cop_moment")
                                .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            // 饕餮时刻时无法被刺客杀死
            if (gameWorldComponent.isRole(victim, TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(victim);
                if (taotieComp.isTaotieMomentActive() && deathReason == DEATH_REASON_ASSASSINATED) {
                    // 记录饕餮时刻免疫刺客
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                                .actor(serverVictim)
                                .put("block_reason", "taotie_moment")
                                .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            // 生存时刻时生存大师免疫被刺客刺杀
            if (gameWorldComponent.isRole(victim, SURVIVAL_MASTER)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(victim);
                if (survivalComp.isSurvivalMomentActive() && deathReason == DEATH_REASON_ASSASSINATED) {
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                                .actor(serverVictim)
                                .put("block_reason", "survival_moment")
                                .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            if (gameWorldComponent.isRole(victim, JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                if (jesterComponent.inStasis) {
                    if (deathReason == GameConstants.DeathReasons.FELL_OUT_OF_TRAIN || deathReason == GameConstants.DeathReasons.ESCAPED) {
                        // 断线/逃跑死亡不被禁锢阻挡，重置小丑状态
                        jesterComponent.reset();
                    } else {
                        // 禁锢期间免疫其他死亡
                        if (victim instanceof ServerPlayerEntity serverVictim) {
                            var event = GameRecordManager.event("death_blocked")
                                    .actor(serverVictim)
                                    .put("block_reason", "jester_stasis")
                                    .put("death_reason", deathReason.toString());
                            if (killer instanceof ServerPlayerEntity serverKiller) {
                                event.target(serverKiller);
                            }
                            event.record();
                        }
                        return KillPlayer.KillResult.cancel();
                    }
                }
            }

            KillPlayer.KillResult saintResult = SaintHelper.handleBeforeKill(victim, killer, deathReason, gameWorldComponent);
            if (saintResult != null) return saintResult;

            if (deathReason == GameConstants.DeathReasons.FELL_OUT_OF_TRAIN) return null;

            if (gameWorldComponent.isRole(victim, JESTER) &&
                    deathReason == GameConstants.DeathReasons.GUN &&
                    killer != null) {
                Role killerRole = gameWorldComponent.getRole(killer);
                if (killerRole != null && killerRole.isInnocent()) {
                    JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                    if (!jesterComponent.inPsychoMode) {
                        jesterComponent.targetKiller = killer.getUuid();
                        jesterComponent.enterStasis(GameConstants.getInTicks(0, 5));
                        return KillPlayer.KillResult.cancel();
                    }
                }
            }

            // Iron Man buff protection (from Professor)
            IronManPlayerComponent ironManComp = IronManPlayerComponent.KEY.get(victim);
            if (ironManComp.hasBuff() && deathReason != GameConstants.DeathReasons.SHOT_INNOCENT && deathReason != DEATH_REASON_ASSASSINATED) {
                victim.getWorld().playSound(null, victim.getBlockPos(), WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5.0F, 1.0F);
                // 记录铁人药水保护生效
                if (victim instanceof ServerPlayerEntity serverVictim) {
                    var deathBlockedEvent = GameRecordManager.event("death_blocked")
                            .actor(serverVictim)
                            .put("block_reason", "iron_man_buff")
                            .put("death_reason", deathReason.toString());
                    if (killer instanceof ServerPlayerEntity serverKiller) {
                        deathBlockedEvent.target(serverKiller);
                    }
                    deathBlockedEvent.record();

                    var ironManEvent = GameRecordManager.event("iron_man_activated")
                            .actor(serverVictim)
                            .put("action", "block_damage");
                    if (killer instanceof ServerPlayerEntity serverKiller) {
                        ironManEvent.target(serverKiller);
                    }
                    ironManEvent.record();
                }
                ironManComp.removeBuff();
                return KillPlayer.KillResult.cancel();
            }

            // 威士忌护盾保护（可叠层，每次消耗一层）
            if (victim instanceof ServerPlayerEntity serverVictim2
                    && serverVictim2.hasStatusEffect(ModEffects.WHISKEY_SHIELD)
                    && deathReason != GameConstants.DeathReasons.SHOT_INNOCENT
                    && deathReason != DEATH_REASON_ASSASSINATED) {
                org.agmas.noellesroles.effect.WhiskeyShieldEffect.consumeShield(serverVictim2);
                victim.getWorld().playSound(null, victim.getBlockPos(), WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5.0F, 1.0F);
                // 记录威士忌护盾保护（仿铁人药剂模式）
                var deathBlockedEvent2 = GameRecordManager.event("death_blocked")
                        .actor(serverVictim2)
                        .put("block_reason", "whiskey_shield")
                        .put("death_reason", deathReason.toString());
                if (killer instanceof ServerPlayerEntity serverKiller) {
                    deathBlockedEvent2.target(serverKiller);
                }
                deathBlockedEvent2.record();

                var whiskeyEvent = GameRecordManager.event("whiskey_shield_activated")
                        .actor(serverVictim2)
                        .put("action", "block_damage");
                if (killer instanceof ServerPlayerEntity serverKiller) {
                    whiskeyEvent.target(serverKiller);
                }
                whiskeyEvent.record();
                return KillPlayer.KillResult.cancel();
            }
            // 球棒处于物品冷却中 → 左键挥击一律无效(Wathe 的 BAT 击杀走 attack 混入,不读冷却)
            if (deathReason == GameConstants.DeathReasons.BAT
                    && killer instanceof ServerPlayerEntity serverBatKiller
                    && serverBatKiller.getItemCooldownManager().isCoolingDown(WatheItems.BAT)) {
                return KillPlayer.KillResult.cancel();
            }
            if (victim instanceof ServerPlayerEntity serverVictim3
                    && killer instanceof ServerPlayerEntity serverKiller
                    && (deathReason == GameConstants.DeathReasons.KNIFE
                    || deathReason == GameConstants.DeathReasons.BAT
                    || deathReason == GameConstants.DeathReasons.GUN
                    || deathReason == GameConstants.DeathReasons.SHOT_INNOCENT
                    || deathReason == DEATH_REASON_THROWING_AXE)) {
                RiotPatrolPlayerComponent riotPatrolComponent = RiotPatrolPlayerComponent.KEY.get(serverVictim3);
                if (riotPatrolComponent.blocksAttacker(serverKiller)) {
                    riotPatrolComponent.playShieldBlockEffects();
                    if (gameWorldComponent.isRole(serverVictim3, WatheRoles.LOOSE_END)) {
                        riotPatrolComponent.lowerShield(true);
                        LooseEndPlayerComponent looseEndComponent = LooseEndPlayerComponent.KEY.get(serverVictim3);
                        if (looseEndComponent.registerBlockedDeath() >= 3) {
                            looseEndComponent.consumeOneRiotShield();
                        }
                    }
                    var deathBlockedEvent = GameRecordManager.event("death_blocked")
                        .actor(serverVictim3)
                        .put("block_reason", "riot_shield")
                        .put("death_reason", deathReason.toString());
                    deathBlockedEvent.target(serverKiller);
                    deathBlockedEvent.record();
                    return KillPlayer.KillResult.cancel();
                }
            }

            // 保镖格挡:姿态中的保镖正面格挡打向自己的匕首/球棒击杀
            if (victim instanceof ServerPlayerEntity serverVictimBg
                    && killer instanceof ServerPlayerEntity serverKillerBg
                    && (deathReason == GameConstants.DeathReasons.KNIFE || deathReason == GameConstants.DeathReasons.BAT)
                    && gameWorldComponent.isRole(serverVictimBg, BODYGUARD)) {
                BodyguardPlayerComponent bodyguardComp = BodyguardPlayerComponent.KEY.get(serverVictimBg);
                if (bodyguardComp.isStanceActive()
                        && serverVictimBg.squaredDistanceTo(serverKillerBg) <= 25.0
                        && bodyguardComp.blocksAttacker(serverKillerBg)) {
                    if (deathReason == GameConstants.DeathReasons.KNIFE) {
                        AbilityPlayerComponent.KEY.get(serverKillerBg).startKnifeCooldownOverride(
                                BodyguardPlayerComponent.KNIFE_BLOCK_OVERRIDE_TICKS);
                        serverKillerBg.getItemCooldownManager().set(WatheItems.KNIFE,
                                BodyguardPlayerComponent.KNIFE_BLOCK_OVERRIDE_TICKS);
                    } else {
                        serverKillerBg.getItemCooldownManager().set(WatheItems.BAT,
                                BodyguardPlayerComponent.BAT_BLOCK_COOLDOWN_TICKS);
                    }
                    bodyguardComp.onBlockedAttack();
                    serverVictimBg.getWorld().playSound(null, serverVictimBg.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.9F + serverVictimBg.getWorld().random.nextFloat() * 0.2F);
                    serverVictimBg.sendMessage(Text.translatable("tip.bodyguard.blocked", serverKillerBg.getDisplayName()), true);
                    GameRecordManager.recordSkillUse(serverVictimBg, BODYGUARD_ID, serverKillerBg, null);
                    return KillPlayer.KillResult.cancel();
                }
            }

            KillPlayer.KillResult ferrymanResult = FerrymanHelper.handleBeforeKill(victim, killer, deathReason, gameWorldComponent);
            if (ferrymanResult != null) return ferrymanResult;

            if (gameWorldComponent.isRole(victim, GANGSTER_STAR)) {
                GangsterStarPlayerComponent gangsterStarComponent = GangsterStarPlayerComponent.KEY.get(victim);
                if (deathReason == GameConstants.DeathReasons.GUN
                        || deathReason == GameConstants.DeathReasons.SHOT_INNOCENT) {
                    if (gangsterStarComponent.tryStartFinalStand(killer, deathReason)) {
                        return KillPlayer.KillResult.cancel();
                    }
                }
            }

            // 被吞噬的玩家可以被杀死，但不生成尸体
            SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(victim);
            if (swallowedComp.isSwallowed()) {
                return KillPlayer.KillResult.allowWithoutBody();
            }
            return null;
        }));
        CanSeePoison.EVENT.register((player) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
            return gameWorldComponent.isRole((PlayerEntity) player, Noellesroles.TOXICOLOGIST);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND) {
                return net.minecraft.util.ActionResult.PASS;
            }
            if (!world.isClient
                    && world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock
                    && HallucinationHelper.hasSleepCooldown(player)) {
                player.sendMessage(Text.translatable(
                        "hallucination.sleep.cooldown",
                        HallucinationHelper.getSleepCooldownSeconds(player)
                ), true);
                return net.minecraft.util.ActionResult.SUCCESS;
            }

            Box box = new Box(hitResult.getBlockPos()).expand(1.5);
            org.agmas.noellesroles.entity.HunterTrapEntity trap = world.getEntitiesByClass(org.agmas.noellesroles.entity.HunterTrapEntity.class, box, entity -> entity.squaredDistanceTo(hitResult.getPos()) < 2.25)
                .stream()
                .min(java.util.Comparator
                        .comparingDouble((org.agmas.noellesroles.entity.HunterTrapEntity entity) -> entity.squaredDistanceTo(hitResult.getPos()))
                        .thenComparingInt(net.minecraft.entity.Entity::getId))
                .orElse(null);

            if (player.isSneaking() && trap != null && player.getUuid().equals(trap.getOwnerUuid())) {
                if (!world.isClient) {
                    player.giveItemStack(org.agmas.noellesroles.ModItems.HUNTER_TRAP.getDefaultStack());
                    world.playSound(null, trap.getBlockPos(), SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8F, 1.2F);
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        NbtCompound extra = new NbtCompound();
                        GameRecordManager.putBlockPos(extra, "pos", trap.getBlockPos());
                        extra.putString("action", "pickup");
                        GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.HUNTER_TRAP), null, extra);
                    }
                    trap.unregisterFromOwner();
                    trap.discard();
                }
                return net.minecraft.util.ActionResult.SUCCESS;
            }

            if (player.isSneaking() && trap != null && trap.canBeRemovedBy(player)) {
                if (!world.isClient) {
                    applyTrapDismantleCooldowns(player);
                    world.playSound(null, trap.getBlockPos(), SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8F, 1.2F);
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        NbtCompound extra = new NbtCompound();
                        GameRecordManager.putBlockPos(extra, "pos", trap.getBlockPos());
                        extra.putString("action", "dismantle");
                        GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.HUNTER_TRAP), null, extra);
                    }
                    trap.unregisterFromOwner();
                    trap.discard();
                }
                return net.minecraft.util.ActionResult.SUCCESS;
            }

            if (!player.getStackInHand(hand).isOf(WatheItems.POISON_VIAL)) {
                return net.minecraft.util.ActionResult.PASS;
            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
            boolean isLooseEnd = gameWorldComponent.isRole(player, WatheRoles.LOOSE_END);
            if (!gameWorldComponent.canUseKillerFeatures(player) && !isLooseEnd) {
                return net.minecraft.util.ActionResult.PASS;
            }

            if (trap == null || trap.isPoisoned()) {
                return net.minecraft.util.ActionResult.PASS;
            }
            if (isLooseEnd && !player.getUuid().equals(trap.getOwnerUuid())) {
                return net.minecraft.util.ActionResult.PASS;
            }

            if (!world.isClient) {
                trap.setPoisoned(true);
                trap.setPoisonerUuid(player.getUuid());
                world.playSound(null, trap.getBlockPos(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 0.8F, 1.1F);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "poison");
                    GameRecordManager.putBlockPos(extra, "pos", trap.getBlockPos());
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.HUNTER_TRAP), null, extra);
                }
                player.getStackInHand(hand).decrement(1);
            }
            return net.minecraft.util.ActionResult.SUCCESS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // 客户端返回 PASS 让原版流程正常发包，实际逻辑由服务端执行。
            // 客户端每次右键会先后发送 INTERACT_AT（带命中点）与 INTERACT 两个包，服务端只在后者结算，避免一次右键给出两张纸条
            if (!(player instanceof ServerPlayerEntity giver) || giver.isSpectator() || hitResult != null) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof ServerPlayerEntity target) || target.isSpectator() || !target.isSneaking()) {
                return ActionResult.PASS;
            }
            ItemStack held = giver.getStackInHand(hand);
            if (!held.isOf(WatheItems.NOTE)) {
                return ActionResult.PASS;
            }

            PlayerInventory inventory = target.getInventory();
            int emptySlot = -1;
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                if (inventory.getStack(i).isEmpty()) {
                    emptySlot = i;
                    break;
                }
            }

            boolean placed = false;
            if (emptySlot >= 0) {
                inventory.setStack(emptySlot, held.copyWithCount(1));
                placed = true;
            } else {
                for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                    ItemStack slotStack = inventory.getStack(i);
                    if (slotStack.isOf(WatheItems.NOTE) && slotStack.getCount() < slotStack.getMaxCount()) {
                        slotStack.increment(1);
                        placed = true;
                        break;
                    }
                }
            }

            if (!placed) {
                giver.sendMessage(Text.translatable("tip.noellesroles.note.full").withColor(0xC90000), true);
                return ActionResult.FAIL;
            }

            if (!giver.isCreative()) {
                held.decrement(1);
                giver.getInventory().markDirty();
                giver.playerScreenHandler.sendContentUpdates();
            }
            inventory.markDirty();
            target.playerScreenHandler.sendContentUpdates();
            giver.swingHand(hand);

            giver.sendMessage(Text.translatable("tip.noellesroles.note.give", target.getDisplayName()), true);
            target.sendMessage(Text.translatable("tip.noellesroles.note.received", giver.getDisplayName()), true);

            NbtCompound extra = new NbtCompound();
            extra.putString("action", "give");
            GameRecordManager.recordItemUse(giver, Registries.ITEM.getId(WatheItems.NOTE), target, extra);
            return ActionResult.SUCCESS;
        });
        // 傀儡 / 本体替身被满蓄力球棒击中：与球棒打玩家相同的判定（Wathe 的球棒击杀只认玩家目标）
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(entity instanceof PuppetEntity puppet) || !(player instanceof ServerPlayerEntity attacker)) {
                return ActionResult.PASS;
            }
            if (!GameFunctions.isPlayerPlayingAndAlive(attacker)
                    || !attacker.getMainHandStack().isOf(WatheItems.BAT)
                    || attacker.getItemCooldownManager().isCoolingDown(WatheItems.BAT)
                    || attacker.getAttackCooldownProgress(0.5F) < 1.0F) {
                return ActionResult.PASS;
            }
            attacker.resetLastAttackedTicks();
            PuppeteerControl.onEntityHit(puppet, attacker, GameConstants.DeathReasons.BAT);
            return ActionResult.SUCCESS;
        });
        // 孟婆（渡人）：其他玩家潜行空手右键孟婆，花 250 金币买一碗孟婆汤
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                MengPoHelper.tryBuySoup(player, hand, entity, hitResult));
        RoleAssigned.EVENT.register((player, role) -> {
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(player);
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
            abilityPlayerComponent.cooldown = NoellesRolesConfig.HANDLER.instance().generalCooldownTicks;
            // 杀手阵营：开局对讲机切到本局共享的随机频道（Wathe 的对讲机发放挂在同一个事件上，延后一 tick 才能拿到）
            if (role != null && role.canUseKiller()) {
                WalkieTalkieChannels.scheduleKillerChannel(player);
            }
            if (role.equals(VULTURE)) {
                VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(player);
                vulturePlayerComponent.reset();
                vulturePlayerComponent.setBodiesRequired(gameWorldComponent.getAllPlayers().size() / 2);
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            } else if (role.equals(FERRYMAN)) {
                FerrymanPlayerComponent ferrymanPlayerComponent = FerrymanPlayerComponent.KEY.get(player);
                ferrymanPlayerComponent.reset();
                ferrymanPlayerComponent.setFerriedRequired(Math.max(1, gameWorldComponent.getAllPlayers().size() / 2));
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            } else if (role.equals(CONDUCTOR)) {
                player.giveItemStack(ModItems.MASTER_KEY.getDefaultStack());
            } else if (role.equals(AWESOME_BINGLUS)) {
                for (int i = 0; i < 16; i++) {
                    player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                }
            } else if (role.equals(JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);
                jesterComponent.reset();
                int totalPlayers = gameWorldComponent.getAllPlayers().size();
                jesterComponent.psychoArmour = Math.max(1, totalPlayers / 5);
            } else if (role.equals(CORRUPT_COP)) {
                player.giveItemStack(WatheItems.REVOLVER.getDefaultStack());
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
                // 初始化黑警时刻组件
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(player);
                corruptCopComp.initializeForGame(gameWorldComponent.getAllPlayers().size());
            } else if (role.equals(GANGSTER_STAR)) {
                GangsterStarPlayerComponent gangsterStarComponent = GangsterStarPlayerComponent.KEY.get(player);
                gangsterStarComponent.reset();
                player.giveItemStack(WatheItems.REVOLVER.getDefaultStack());
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            } else if (role.equals(PATHOGEN)) {
                PathogenPlayerComponent pathogenComp = PathogenPlayerComponent.KEY.get(player);
                pathogenComp.reset();
                // Set base cooldown based on player count (6-11: 20s, 12-17: 15s, 18-24: 10s, 24+: 7s)
                pathogenComp.setBaseCooldownByPlayerCount(gameWorldComponent.getAllPlayers().size());
                // Set initial cooldown to 10 seconds
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 10);
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            } else if (role.equals(CRIMINAL_REASONER)) {
                CriminalReasonerPlayerComponent reasonerComp = CriminalReasonerPlayerComponent.KEY.get(player);
                reasonerComp.reset();
                // 胜利所需成功推理数：开局人数的三分之一向下取整（整数除法即向下取整）
                reasonerComp.setRequiredReasoningCount(gameWorldComponent.getAllPlayers().size() / 3);
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            } else if (role.equals(ASSASSIN)) {
                AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(player);
                assassinComp.reset();
                int totalPlayers = gameWorldComponent.getAllPlayers().size();
                assassinComp.setMaxGuesses(totalPlayers);  // (totalPlayers + 3) / 4
                // 刺客开局冷却60秒
                assassinComp.setCooldown(GameConstants.getInTicks(0, 60));
                // 刺客没有开局道具，只依靠猜测技能
            } else if (role.equals(REPORTER)) {
                ReporterPlayerComponent reporterComp = ReporterPlayerComponent.KEY.get(player);
                reporterComp.reset();
                // 记者开局冷却30秒
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 30);
            } else if (role.equals(TOXICOLOGIST)) {
                // 毒理学家开局获得解毒剂，2分钟冷却
                player.giveItemStack(ModItems.ANTIDOTE.getDefaultStack());
                player.getItemCooldownManager().set(ModItems.ANTIDOTE, org.agmas.noellesroles.item.AntidoteItem.INITIAL_COOLDOWN_TICKS);
            } else if (role.equals(SERIAL_KILLER)) {
                SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(player);
                serialKillerComp.reset();
                // 连环杀手开局技能冷却3分钟
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(3, 0);
            } else if (role.equals(PROFESSOR)) {
                // Professor starts with 1 Iron Man Vial
                player.giveItemStack(ModItems.IRON_MAN_VIAL.getDefaultStack());
                player.getItemCooldownManager().set(ModItems.IRON_MAN_VIAL, 20 * 60 * 3);
            } else if (role.equals(ENGINEER)) {
                // 工程师开局获得维修工具，60秒开局冷却
                EngineerPlayerComponent engineerComp = EngineerPlayerComponent.KEY.get(player);
                engineerComp.reset();
                player.giveItemStack(ModItems.REPAIR_TOOL.getDefaultStack());
                player.getItemCooldownManager().set(ModItems.REPAIR_TOOL, GameConstants.getInTicks(0, 60));
            } else if (role.equals(RIOT_PATROL)) {
                RiotPatrolPlayerComponent riotPatrolComponent = RiotPatrolPlayerComponent.KEY.get(player);
                riotPatrolComponent.reset();
                player.giveItemStack(ModItems.RIOT_SHIELD.getDefaultStack());
                player.giveItemStack(ModItems.RIOT_FORK.getDefaultStack());
            } else if (role.equals(WHITE_KNIGHT)) {
                // 白骑士开局携带左轮手枪（警类标准10秒冷却）
                player.giveItemStack(WatheItems.REVOLVER.getDefaultStack());
            } else if (role.equals(ECONOMIST)) {
                EconomistPlayerComponent.KEY.get(player).reset();
            } else if (role.equals(WatheRoles.LOOSE_END)) {
                LooseEndPlayerComponent looseEndComponent = LooseEndPlayerComponent.KEY.get(player);
                looseEndComponent.reset();
            } else if (role.equals(HUNTER)) {
                HunterPlayerComponent hunterPlayerComponent = HunterPlayerComponent.KEY.get(player);
                hunterPlayerComponent.reset();
            } else if (role.equals(ORTHOPEDIST)) {
                OrthopedistPlayerComponent orthopedistPlayerComponent = OrthopedistPlayerComponent.KEY.get(player);
                orthopedistPlayerComponent.reset();
            } else if (role.equals(BAOSANG)) {
                BaosangPlayerComponent baosangComp = BaosangPlayerComponent.KEY.get(player);
                baosangComp.reset();
            } else if (role.equals(JUNKER)) {
                JunkerPlayerComponent.KEY.get(player).reset();
            } else if (role.equals(SAINT)) {
                // 圣徒禁止捡枪改由 RoleGunPickupMixin 按当前职业实时判断，转职后即可恢复
                SaintPlayerComponent saintPlayerComponent = SaintPlayerComponent.KEY.get(player);
                saintPlayerComponent.reset();
            } else if (role.equals(MENGPO)) {
                // 孟婆开局弹出「渡人 / 渡己」选择，超时默认渡人
                MengPoPlayerComponent.KEY.get(player).startChoice();
            } else if (role.equals(COMMANDER)) {
                CommanderPlayerComponent commanderPlayerComponent = CommanderPlayerComponent.KEY.get(player);
                commanderPlayerComponent.reset();
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 15);
            } else if (role.equals(ATTENDANT)) {
                // 乘务员开局获得乘务登记表，并预填所有乘客的 ID 与房间
                AttendantPlayerComponent attendantComponent = AttendantPlayerComponent.KEY.get(player);
                attendantComponent.reset();
                attendantComponent.ensureEntries();
                player.giveItemStack(ModItems.ATTENDANT_REGISTER.getDefaultStack());
            } else if (role.equals(TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(player);
                taotieComp.initializeForGame(gameWorldComponent.getAllPlayers().size());
                taotieComp.setSwallowCooldown(GameConstants.getInTicks(1, 0));
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            } else if (role.equals(BOMBER)) {
                player.getItemCooldownManager().set(ModItems.TIMED_BOMB, 20 * 45);
            } else if (role.equals(SILENCER)) {
                // 静语者开局冷却45秒
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 45);
            } else if (role.equals(BREACHER)) {
                BreacherPlayerComponent.KEY.get(player).setCooldownTicks(GameConstants.getInTicks(1, 0));
            } else if (role.equals(PHONISM)) {
                // 谎言音效师开局冷却45秒
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 45);
            } else if (role.equals(UNDERCOVER)) {
                player.giveItemStack(WatheItems.WALKIE_TALKIE.getDefaultStack());
            } else if (role.equals(BODYGUARD)) {
                BodyguardPlayerComponent bodyguardComp = BodyguardPlayerComponent.KEY.get(player);
                bodyguardComp.reset();
                abilityPlayerComponent.cooldown = BodyguardPlayerComponent.BLOCK_COOLDOWN_TICKS; // 开局技能冷却90秒
            } else if (role.equals(SURVIVAL_MASTER)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(player);
                survivalComp.reset();
                survivalComp.initializeForGame(gameWorldComponent.getAllKillerTeamPlayers().size());
            } else if (role.equals(POISONER)) {
                player.getItemCooldownManager().set(ModItems.POISON_NEEDLE, GameConstants.getInTicks(1, 0)); // 1分钟初始冷却
            } else if (role.equals(DREAM_EATER)) {
                // 噬梦者开局技能冷却45秒
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 45);
                // CCA 不会为已跟踪的玩家自动补发组件，主动推送一次全员睡任务状态供穿墙透视
                if (player instanceof ServerPlayerEntity serverDreamEater) {
                    for (ServerPlayerEntity target : serverDreamEater.getServerWorld().getPlayers()) {
                        DreamEaterPlayerComponent.KEY.get(target).sync();
                    }
                }
            } else if (role.equals(BANDIT)) {
                BanditPlayerComponent.KEY.get(player).reset();
                // 强盗开局技能冷却90秒（齐掷飞斧）
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 30);
            } else if (role.equals(PUPPETEER)) {
                // 傀儡师一局最多制造「本局杀手数量」具傀儡
                PuppeteerPlayerComponent.KEY.get(player).initializeForGame(gameWorldComponent.getAllKillerTeamPlayers().size());
            }
        });
        // 解毒剂预防护：未中毒玩家身上的护盾挡下一次中毒（不消耗投毒方行动，只提示被保护者）
        PlayerPoisoned.BEFORE.register((target, poisonTicks, poisonerUuid) -> {
            if (poisonTicks <= 0) {
                return PlayerPoisoned.PoisonResult.allow();
            }
            // 傀儡形态中毒：傀儡立即倒地，毒不会留到傀儡师身上
            if (target instanceof ServerPlayerEntity serverTarget
                    && PuppeteerControl.handlePoisoned(serverTarget, poisonerUuid)) {
                return PlayerPoisoned.PoisonResult.cancel();
            }
            PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
            if (poisonComp.poisonTicks > 0) {
                return PlayerPoisoned.PoisonResult.allow();
            }
            AntidoteShieldPlayerComponent shieldComp = AntidoteShieldPlayerComponent.KEY.get(target);
            if (shieldComp.hasShield()) {
                shieldComp.consumeShield();
                if (target instanceof ServerPlayerEntity serverTarget) {
                    serverTarget.sendMessage(Text.translatable("tip.noellesroles.antidote_shield_blocked"), true);
                }
                return PlayerPoisoned.PoisonResult.cancel();
            }
            return PlayerPoisoned.PoisonResult.allow();
        });
        ResetPlayer.EVENT.register(player -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
            BartenderPlayerComponent.KEY.get(player).reset();
            MorphlingPlayerComponent.KEY.get(player).reset();
            MorphlingScramblePlayerComponent.KEY.get(player).reset();
            VoodooPlayerComponent.KEY.get(player).reset();
            RecallerPlayerComponent.KEY.get(player).reset();
            VulturePlayerComponent.KEY.get(player).reset();
            JesterPlayerComponent.KEY.get(player).reset();
            InfectedPlayerComponent.KEY.get(player).reset();
            PathogenPlayerComponent.KEY.get(player).reset();
            BomberPlayerComponent.KEY.get(player).reset();
            AssassinPlayerComponent.KEY.get(player).reset();
            CriminalReasonerPlayerComponent.KEY.get(player).reset();
            GangsterStarPlayerComponent.KEY.get(player).reset();
            CorruptCopPlayerComponent.KEY.get(player).reset();
            ReporterPlayerComponent.KEY.get(player).reset();
            AttendantPlayerComponent.KEY.get(player).reset();
            SerialKillerPlayerComponent.KEY.get(player).reset();
            IronManPlayerComponent.KEY.get(player).reset();
            AntidoteShieldPlayerComponent.KEY.get(player).reset();
            TaotiePlayerComponent.KEY.get(player).reset();
            SwallowedPlayerComponent.KEY.get(player).reset();
            SilencedPlayerComponent.KEY.get(player).reset();
            SilencerPlayerComponent.KEY.get(player).reset();
            BodyguardPlayerComponent.KEY.get(player).reset();
            NoisemakerPlayerComponent.KEY.get(player).reset();
            SurvivalMasterPlayerComponent.KEY.get(player).reset();
            RiotPatrolPlayerComponent.KEY.get(player).reset();
            HunterPlayerComponent.KEY.get(player).reset();
            OrthopedistPlayerComponent.KEY.get(player).reset();
            FerrymanPlayerComponent.KEY.get(player).reset();
            CommanderPlayerComponent.KEY.get(player).reset();
            BreacherPlayerComponent.KEY.get(player).reset();
            DreamEaterPlayerComponent.KEY.get(player).reset();
            SaintPlayerComponent.KEY.get(player).reset();
            BaosangPlayerComponent.KEY.get(player).reset();
            JunkerPlayerComponent.KEY.get(player).reset();
            EconomistPlayerComponent.KEY.get(player).reset();
            LooseEndPlayerComponent.KEY.get(player).reset();
            HallucinationPlayerComponent.KEY.get(player).reset();
            BanditPlayerComponent.KEY.get(player).reset();
            PuppeteerPlayerComponent.KEY.get(player).reset();
            MengPoPlayerComponent.KEY.get(player).reset();
            MengPoSoupPlayerComponent.KEY.get(player).reset();
            DeathArenaPlayerComponent deathArenaPlayer = DeathArenaPlayerComponent.KEY.get(player);
            if (!deathArenaPlayer.isInArena() && !deathArenaPlayer.isPendingRespawn()) {
                deathArenaPlayer.reset();
            }
        });

        // Bartender and Recaller get +50 coins when completing tasks
        TaskComplete.EVENT.register((player, taskType) -> {
            // 仅噬梦者的自定义睡觉任务不触发「完成任务」奖励；
            // 以后新增的自定义任务默认照常发钱，除非单独说明要排除。
            if (NoellesTaskTypes.isDreamSleepTask(taskType)) return;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
            Role role = gameWorldComponent.getRole(player);
            if (role != null && (role.equals(BARTENDER) || role.equals(RECALLER) || role.equals(TIMEKEEPER) || role.equals(REPORTER) || role.equals(GANGSTER_STAR) || role.equals(ECONOMIST) || role.equals(VOODOO))) {
                PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(player);
                playerShopComponent.addToBalance(50);
            }
        });
        // 品酒：每完成两次「去喝点东西」任务，随机获得一份调酒材料（不含冰块与基酒）
        List<Item> wineTastingMaterials = List.of(ModItems.RUM, ModItems.GIN, ModItems.VODKA, ModItems.TEQUILA,
                ModItems.WHISKEY, ModItems.BRANDY, ModItems.LIQUEUR, ModItems.SPECIAL_SPICE);
        TaskComplete.EVENT.register((player, taskType) -> {
            if (NoellesTaskTypes.isDreamSleepTask(taskType)) return; // 噬梦者任务不推进任务奖励计数（以后的自定义任务默认照常计数）
            if (taskType != PlayerMoodComponent.Task.DRINK) return;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorldComponent.isRole(player, BARTENDER) || !GameFunctions.isPlayerPlayingAndAlive(player)) return;
            BartenderPlayerComponent bartenderComponent = BartenderPlayerComponent.KEY.get(player);
            if (++bartenderComponent.drinkTasksCompleted % 2 != 0) return;
            ItemStack gift = new ItemStack(wineTastingMaterials.get(player.getWorld().random.nextInt(wineTastingMaterials.size())));
            // giveItemStack 入包时会清空传入的 ItemStack，先取好名字
            Text giftName = gift.getName();
            if (!player.giveItemStack(gift)) {
                player.dropItem(gift, false);
            }
            player.sendMessage(Text.translatable("tip.bartender.wine_tasting.gain", giftName), true);
        });
        // 噬梦者：睡觉任务完成结算——解除理智下降加速；三击命中累积后在这次睡觉任务完成时无视护盾死亡。
        // 噬梦者的睡觉任务在追加出来的独立槽位 NoellesTaskTypes.DREAM_SLEEP 上（原版 SLEEP 槽位留给原版睡觉任务），
        // 所以这里的判定用 DREAM_SLEEP，而不是 PlayerMoodComponent.Task.SLEEP。
        TaskComplete.EVENT.register((player, taskType) -> {
            if (taskType != NoellesTaskTypes.DREAM_SLEEP.slot()) return;
            DreamEaterPlayerComponent dreamEaterComp = DreamEaterPlayerComponent.KEY.get(player);
            if (!dreamEaterComp.isSleepAwaiting()) return;
            boolean die = dreamEaterComp.consumeSleepCompletion();
            if (!die) return;

            ServerPlayerEntity killer = null;
            UUID applierUuid = dreamEaterComp.getLastDreamEaterUuid();
            if (applierUuid != null && player.getWorld().getPlayerByUuid(applierUuid) instanceof ServerPlayerEntity serverKiller) {
                killer = serverKiller;
            }
            GameFunctions.killPlayer(player, true, killer, DEATH_REASON_DREAM_EATER_SLEEP, true);
        });
        ShopPurchase.BEFORE.register((player, entry, amount) -> {
            if (SwallowedPlayerComponent.isPlayerSwallowed(player)) {
                return ShopPurchase.PurchaseResult.deny("shop.error.swallowed");
            }
            // 傀儡师本体无法购买，只有傀儡形态能用杀手商店
            if (PuppeteerShopHandler.isBodyForm(player)) {
                return ShopPurchase.PurchaseResult.deny(PuppeteerShopHandler.BODY_DENY_KEY);
            }
            String looseEndDenyReason = LooseEndShopHandler.getCooldownDenyReason(player, entry);
            if (looseEndDenyReason != null) {
                return ShopPurchase.PurchaseResult.deny(looseEndDenyReason);
            }
            looseEndDenyReason = LooseEndShopHandler.getOwnershipDenyReason(player, entry);
            if (looseEndDenyReason != null) {
                return ShopPurchase.PurchaseResult.deny(looseEndDenyReason);
            }
            String banditRecoverDenyReason = BanditShopHandler.getRecoverDenyReason(player, entry);
            if (banditRecoverDenyReason != null) {
                return ShopPurchase.PurchaseResult.deny(banditRecoverDenyReason);
            }
            String voodooDenyReason = VoodooShopHandler.getDuplicateDenyReason(player, entry);
            if (voodooDenyReason != null) {
                return ShopPurchase.PurchaseResult.deny(voodooDenyReason);
            }
            return ShopPurchase.PurchaseResult.allow();
        });
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) -> {
            // 秃鹫胜利检查（优先级最高）
            for (UUID uuid : gameComponent.getAllWithRole(VULTURE)) {
                PlayerEntity vulture = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(vulture)) {
                    VulturePlayerComponent component = VulturePlayerComponent.KEY.get(vulture);
                    if (component.hasWon()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) vulture);
                    }
                }
            }

            // 生存时刻完成 → 乘客胜利
            for (UUID uuid : gameComponent.getAllWithRole(FERRYMAN)) {
                PlayerEntity ferryman = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(ferryman)) {
                    FerrymanPlayerComponent component = FerrymanPlayerComponent.KEY.get(ferryman);
                    if (component.hasWon()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) ferryman);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(SURVIVAL_MASTER)) {
                PlayerEntity survivalMaster = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(survivalMaster)) {
                    SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(survivalMaster);
                    if (survivalComp.hasSurvivalMomentCompleted()) {
                        return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.PASSENGERS);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(PATHOGEN)) {
                PlayerEntity pathogen = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(pathogen)) {
                    boolean allInfected = true;
                    for (UUID playerUuid : gameComponent.getAllPlayers()) {
                        if (playerUuid.equals(uuid)) continue;
                        PlayerEntity player = world.getPlayerByUuid(playerUuid);
                        if (player == null) continue;
                        if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                        InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                        if (!infected.isInfected()) {
                            allInfected = false;
                            break;
                        }
                    }
                    if (allInfected && GameFunctions.isPlayerPlayingAndAlive(pathogen)) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) pathogen);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(jester)) {
                    JesterPlayerComponent component = JesterPlayerComponent.KEY.get(jester);
                    if (component.won) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) jester);
                    }
                }
            }

            // 犯罪推理学家胜利判定优先于普通杀手胜利：
            // 正确推理次数达到开局游戏人数的三分之一（向下取整）
            for (UUID uuid : gameComponent.getAllWithRole(CRIMINAL_REASONER)) {
                ServerPlayerEntity criminalReasoner = (ServerPlayerEntity) world.getPlayerByUuid(uuid);
                if (!GameFunctions.isPlayerPlayingAndAlive(criminalReasoner)) continue;

                CriminalReasonerPlayerComponent criminalReasonerComponent = CriminalReasonerPlayerComponent.KEY.get(criminalReasoner);
                int requiredReasoningCount = criminalReasonerComponent.getRequiredReasoningCount();
                if (requiredReasoningCount > 0 && criminalReasonerComponent.getSuccessfulReasoningCount() >= requiredReasoningCount) {
                    return CheckWinCondition.WinResult.neutralWin(criminalReasoner);
                }

            }

            // Taotie win condition check (priority over corrupt cop)
            for (UUID uuid : gameComponent.getAllWithRole(TAOTIE)) {
                PlayerEntity taotie = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);

                    // Win condition 1: Swallowed all players
                    if (taotieComp.hasSwallowedEveryone()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) taotie);
                    }

                    // Win condition 2: Taotie Moment completed
                    if (taotieComp.hasTaotieMomentCompleted()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) taotie);
                    }

                    // Block other factions from winning while Taotie is alive
                    if (currentStatus == GameFunctions.WinStatus.KILLERS
                            || currentStatus == GameFunctions.WinStatus.PASSENGERS) {
                        return CheckWinCondition.WinResult.block();
                    }
                }
            }

            // 小丑疯魔阻止胜利
            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(jester)) {
                    JesterPlayerComponent component = JesterPlayerComponent.KEY.get(jester);
                    if (component.inPsychoMode && (currentStatus == GameFunctions.WinStatus.KILLERS
                            || currentStatus == GameFunctions.WinStatus.PASSENGERS)) {
                        return CheckWinCondition.WinResult.block();
                    }
                }
            }

            // 黑警胜利/阻止检查（最低优先级）
            ServerPlayerEntity livingGangsterStar = null;
            for (UUID uuid : gameComponent.getAllWithRole(GANGSTER_STAR)) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(player)) {
                    livingGangsterStar = player;
                    break;
                }
            }

            if (livingGangsterStar != null) {
                int aliveCount = 0;
                boolean gangsterStarIsAlive = false;
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                    SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
                    if (swallowedComp.isSwallowed()) continue;
                    aliveCount++;
                    if (player.getUuid().equals(livingGangsterStar.getUuid())) {
                        gangsterStarIsAlive = true;
                    }
                }

                if (aliveCount == 1 && gangsterStarIsAlive) {
                    return CheckWinCondition.WinResult.neutralWin(livingGangsterStar);
                }

                if (currentStatus == GameFunctions.WinStatus.KILLERS
                        || currentStatus == GameFunctions.WinStatus.PASSENGERS) {
                    return CheckWinCondition.WinResult.block();
                }
            }

            ServerPlayerEntity livingCorruptCop = null;
            for (UUID uuid : gameComponent.getAllWithRole(CORRUPT_COP)) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(player)) {
                    livingCorruptCop = player;
                    break;
                }
            }

            if (livingCorruptCop != null) {
                int aliveCount = 0;
                boolean corruptCopIsAlive = false;
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                    SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
                    if (swallowedComp.isSwallowed()) continue;
                    aliveCount++;
                    if (player.getUuid().equals(livingCorruptCop.getUuid())) {
                        corruptCopIsAlive = true;
                    }
                }

                if (aliveCount == 1 && corruptCopIsAlive) {
                    return CheckWinCondition.WinResult.neutralWin(livingCorruptCop);
                }

                if (currentStatus == GameFunctions.WinStatus.KILLERS
                        || currentStatus == GameFunctions.WinStatus.PASSENGERS) {
                    return CheckWinCondition.WinResult.block();
                }
            }

            return null;
        });

        // 白骑士：攻击无视疯魔护盾（在原版护盾判定之前把护盾清零，随后正常击杀）
        KillPlayer.BEFORE.register((victim, killer, deathReason) -> {
            if (killer == null
                    || !GameWorldComponent.KEY.get(victim.getWorld()).isRole(killer, WHITE_KNIGHT)) {
                return null;
            }
            PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(victim);
            if (psychoComponent.getPsychoTicks() > 0 && psychoComponent.getArmour() > 0) {
                psychoComponent.setArmour(0);
                psychoComponent.sync();
            }
            return null;
        });

        // 记录受害者和凶手的匹配
        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            if (victim instanceof ServerPlayerEntity serverVictim) {
                // 傀儡师死亡：剩余傀儡全部原地倒地变回尸体
                PuppeteerControl.collapseAllOwnedBy(serverVictim.getServerWorld(), serverVictim);
                // 保镖：死亡瞬间立即解除格挡姿态，避免姿态状态残留在观战/幽灵等状态
                BodyguardPlayerComponent.KEY.get(serverVictim).endStance();
                HallucinationPlayerComponent hallucinationComponent = HallucinationPlayerComponent.KEY.get(serverVictim);
                if (hallucinationComponent.clearAllHallucinations(false)) {
                    hallucinationComponent.sync();
                }
                // 拾荒者：记录掉落结算后尸体上剩下的物品，供搜刮抽取
                JunkerLootWorldComponent.KEY.get(serverVictim.getServerWorld()).recordDeath(serverVictim);
            }
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(victim.getWorld());
            if (victim.getWorld() instanceof ServerWorld serverWorld
                    && MurderMayhemHelper.isMurderMayhem(gameComponent.getGameMode())) {
                MurderMayhemWorldComponent murderMayhemComponent = MurderMayhemWorldComponent.KEY.get(serverWorld);
                if (murderMayhemComponent.getCurrentEvent() instanceof org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent
                        && serverWorld.getRandom().nextBoolean()) {
                    int newRadius = MathHelper.clamp(
                            murderMayhemComponent.getFogRadius() - 1,
                            org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent.MIN_FOG_RADIUS,
                            org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent.MAX_FOG_RADIUS
                    );
                    if (newRadius != murderMayhemComponent.getFogRadius()) {
                        murderMayhemComponent.setFogRadius(newRadius);
                        MurderMayhemWorldComponent.KEY.sync(serverWorld);
                    }
                }
            }

            if (victim instanceof ServerPlayerEntity serverVictim
                    && victim.getWorld() instanceof ServerWorld serverWorld
                    && DeathArenaStateHelper.isDeathArenaParticipant(serverVictim)) {
                DeathArenaServerController.handleDeathAfter(serverVictim);
            }
            if (victim.getWorld() instanceof ServerWorld serverWorld) {
                UUID recordedKillerUuid = killer != null ? killer.getUuid() : null;
                // 被推下列车时 killer 就是推人者（原版 lastAttacker 只保留 5 秒，被推后仍在下坠时由
                // GameWorldComponentFellOutKillerMixin 用 PushAttributionTracker 补上），凶手即推人者；
                // 只有无人担责的死因（自行坠落出列车/小脑/精神崩溃/退出游戏）才把凶手记为死者本人（可被推理）
                if (recordedKillerUuid == null && deathReason != null
                        && CRIMINAL_REASONER_SELF_ATTRIBUTED_DEATHS.contains(deathReason)) {
                    recordedKillerUuid = victim.getUuid();
                }
                if (recordedKillerUuid != null) {
                    for (UUID uuid : gameComponent.getAllWithRole(CRIMINAL_REASONER)) {
                        PlayerEntity criminalReasoner = serverWorld.getPlayerByUuid(uuid);
                        if (criminalReasoner != null) {
                            CriminalReasonerPlayerComponent criminalReasonerComp = CriminalReasonerPlayerComponent.KEY.get(criminalReasoner);
                            criminalReasonerComp.recordReasoningTarget(victim.getUuid(), recordedKillerUuid);
                        }
                    }
                }
            }

            // 报丧：有人死亡时，若技能就绪则启动10秒延迟公布（清道夫击杀不触发）
            boolean isKillerScavenger = killer != null && gameComponent.isRole(killer, SCAVENGER);
            if (!isKillerScavenger && victim.getWorld() instanceof ServerWorld serverWorld) {
                for (UUID uuid : gameComponent.getAllWithRole(BAOSANG)) {
                    PlayerEntity baosangPlayer = serverWorld.getPlayerByUuid(uuid);
                    if (baosangPlayer != null && GameFunctions.isPlayerPlayingAndAlive(baosangPlayer)) {
                        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(baosangPlayer);
                        BaosangPlayerComponent baosangComp = BaosangPlayerComponent.KEY.get(baosangPlayer);
                        if (abilityComp.cooldown <= 0 && baosangComp.getPendingTicks() <= 0) {
                            baosangComp.startPending(victim.getName().getString());
                        }
                    }
                }
            }

            SaintHelper.handleAfterKill(victim, killer, gameComponent);

            // 记录被吞玩家肚内死亡标记
            SwallowedPlayerComponent victimSwallowedCheck = SwallowedPlayerComponent.KEY.get(victim);
            if (victimSwallowedCheck.isSwallowed() && victim instanceof ServerPlayerEntity serverVictim) {
                UUID taotieUuid = victimSwallowedCheck.getSwallowedBy();
                var event = GameRecordManager.event("death_in_stomach")
                        .actor(serverVictim)
                        .put("death_reason", deathReason.toString());
                if (taotieUuid != null) {
                    event.putUuid("taotie_uuid", taotieUuid);
                }
                if (killer != null) {
                    event.putUuid("killer_uuid", killer.getUuid());
                }
                event.record();
            }

            BomberPlayerComponent bomberPlayerComponent = BomberPlayerComponent.KEY.get(victim);
            KillRewardResult rewardResult = KillRewardResolver.resolve(new KillRewardContext(
                    victim,
                    killer,
                    false,
                    deathReason,
                    bomberPlayerComponent.hasBomb() ? bomberPlayerComponent.getBomberUuid() : null,
                    PlayerPoisonComponent.KEY.get(victim).poisoner,
                    null
            ));
            if (killer != null) {
                if (rewardResult.moneyDelta() != 0) {
                    PlayerShopComponent.KEY.get(killer).addToBalance(rewardResult.moneyDelta());
                }
                if (rewardResult.timeDeltaSeconds() > 0) {
                    GameTimeComponent.KEY.get(victim.getWorld()).addTime(rewardResult.timeDeltaSeconds() * 20);
                }
            }

            // 乘客白白阵亡补偿：乘客死于杀手/中立阵营以外之手时，存活杀手平分50金币
            PassengerDeathCompensationHelper.handleAfterKill(victim, killer, deathReason, gameComponent);

            // 误杀冷却惩罚：误杀者死于误杀惩罚时，把其遗落的枪标记为误杀枪
            MisKillGunCooldownHelper.handleMisKillShooterDeath(victim, deathReason);

            // 毒捕兽夹致死奖励：被毒死时给猎人和下毒者加钱
            if (deathReason == GameConstants.DeathReasons.POISON) {
                HunterPlayerComponent hunterComp = HunterPlayerComponent.KEY.get(victim);
                boolean wasTrapPoison = hunterComp.hasTrapPoisonInfo();
                if (wasTrapPoison) {
                    UUID trapOwner = hunterComp.getTrapPoisonOwnerUuid();
                    UUID trapPoisoner = hunterComp.getTrapPoisonPoisonerUuid();
                    if (trapPoisoner != null) {
                        PlayerEntity poisoner = victim.getWorld().getPlayerByUuid(trapPoisoner);
                        if (poisoner != null) {
                            PlayerShopComponent.KEY.get(poisoner).addToBalance(25);
                        }
                    } else if (trapOwner != null) {
                        PlayerEntity owner = victim.getWorld().getPlayerByUuid(trapOwner);
                        if (owner != null) {
                            if (gameComponent.isRole(owner, WatheRoles.LOOSE_END)) {
                                PlayerShopComponent.KEY.get(owner).addToBalance(25);
                            } else {
                                PlayerShopComponent.KEY.get(owner).addToBalance(50);
                            }
                        }
                    }
                    hunterComp.clearTrapPoisonInfo();
                }

                if (!wasTrapPoison) {
                    PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(victim);
                    if (poisonComp.poisoner != null) {
                        PlayerEntity poisoner = victim.getWorld().getPlayerByUuid(poisonComp.poisoner);
                        if (poisoner != null && gameComponent.isRole(poisoner, WatheRoles.LOOSE_END)) {
                            PlayerShopComponent.KEY.get(poisoner).addToBalance(25);
                        }
                    }
                }
            }

            // 噬梦者「永眠」击杀：凶手吃满原版 100，另外由存活杀手队友平分 50 并收到 actionbar 提示。
            // 挂在 AFTER 上，因此被圣徒/保镖等 BEFORE 逻辑拦下的未遂死亡不会发钱。
            if (deathReason == DEATH_REASON_DREAM_EATER_SLEEP) {
                DreamEaterKillRewardHelper.payout(victim, killer);
            }

            // 记录清道夫杀人
            if (killer != null && deathReason == GameConstants.DeathReasons.KNIFE && gameComponent.isRole(killer, SCAVENGER) && !gameComponent.isRole(victim, NOISEMAKER)) {
                HiddenBodiesWorldComponent hiddenBodies = HiddenBodiesWorldComponent.KEY.get(victim.getWorld());
                hiddenBodies.addHiddenBody(victim.getUuid());
            }

            if (NoellesRolesConfig.HANDLER.instance().voodooNonKillerDeaths || killer != null) {
                // 巫毒师与持有娃娃的拾荒者死亡时都会触发连锁诅咒
                if (gameComponent.isRole(victim, Noellesroles.VOODOO) || gameComponent.isRole(victim, Noellesroles.JUNKER)) {
                    VoodooPlayerComponent voodooPlayerComponent = VoodooPlayerComponent.KEY.get(victim);
                    if (voodooPlayerComponent.target != null && (deathReason != DEATH_REASON_ASSASSINATED || !gameComponent.isRole(voodooPlayerComponent.target, ASSASSIN))) {
                        // 被绑定的是傀儡（绑定目标为其生前玩家）：10秒后傀儡倒地，傀儡师随之死亡
                        UUID boundTarget = voodooPlayerComponent.target;
                        PuppetRecord boundPuppet = victim.getWorld() instanceof ServerWorld puppetCurseWorld
                                ? PuppetWorldComponent.get(puppetCurseWorld).get(boundTarget) : null;
                        if (boundPuppet != null && victim.getWorld() instanceof ServerWorld puppetCurseWorld) {
                            if (boundPuppet.getState() == PuppetRecord.State.CONTROLLED
                                    && puppetCurseWorld.getPlayerByUuid(boundPuppet.getOwnerUuid()) instanceof ServerPlayerEntity puppeteer) {
                                puppeteer.networkHandler.sendPacket(new TitleS2CPacket(
                                        Text.translatable("title.noellesroles.voodoo_curse_imminent")
                                                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                                ));
                                puppeteer.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 10));
                            }
                            Scheduler.schedule(() -> {
                                VoodooPlayerComponent currentComp = VoodooPlayerComponent.KEY.get(victim);
                                if (!boundTarget.equals(currentComp.target)) {
                                    return;
                                }
                                PuppeteerControl.applyVoodooCurse(puppetCurseWorld, boundTarget, victim);
                            }, 200);
                        }
                        ServerPlayerEntity voodooed = (ServerPlayerEntity) victim.getWorld().getPlayerByUuid(voodooPlayerComponent.target);
                        if (voodooed != null) {
                            // 圣徒免疫巫毒魔法：可以成为绑定目标，但不会因此死亡，也不收到任何提示
                            if (GameFunctions.isPlayerPlayingAndAlive(voodooed) && voodooed != victim
                                    && !gameComponent.isRole(voodooed, Noellesroles.SAINT)) {
                                // 巫毒师死亡：先向被绑定者发送原版Title提示，巫毒魔法将于10秒后生效
                                if (voodooed instanceof ServerPlayerEntity serverVoodooed) {
                                    serverVoodooed.networkHandler.sendPacket(new TitleS2CPacket(
                                            Text.translatable("title.noellesroles.voodoo_curse_imminent")
                                                    .formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                                    ));
                                    serverVoodooed.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 10));
                                }

                                // 延迟10秒（200tick）执行连锁死亡，期间被绑定者若已被他人击杀/回合结束重置则不再处死
                                UUID boundTargetUuid = voodooPlayerComponent.target;
                                ServerWorld voodooWorld = (ServerWorld) victim.getWorld();
                                Scheduler.schedule(() -> {
                                    if (!(voodooWorld.getPlayerByUuid(boundTargetUuid) instanceof ServerPlayerEntity boundPlayer)
                                            || !GameFunctions.isPlayerPlayingAndAlive(boundPlayer)) {
                                        return;
                                    }
                                    // 回合重置后绑定目标已被清除，取消诅咒
                                    VoodooPlayerComponent currentComp = VoodooPlayerComponent.KEY.get(victim);
                                    if (currentComp.target == null || !currentComp.target.equals(boundTargetUuid)) {
                                        return;
                                    }
                                    // 记录 Voodoo 连锁死亡
                                    if (victim instanceof ServerPlayerEntity serverVictim) {
                                        GameRecordManager.event("voodoo_chain_death")
                                                .actor(serverVictim)
                                                .target(boundPlayer)
                                                .put("voodoo_death_reason", deathReason.toString())
                                                .record();
                                    }
                                    GameFunctions.killPlayer(boundPlayer, true, victim, DEATH_REASON_VOODOO);
                                }, 200);
                            }
                        }
                    }
                }
            }


            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = victim.getWorld().getPlayerByUuid(uuid);
                if (jester != null) {
                    JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(jester);
                    if (jesterComponent.targetKiller != null &&
                            victim.getUuid().equals(jesterComponent.targetKiller)) {
                        jesterComponent.won = true;
                        break;
                    }
                }
            }

            // 如果小丑在疯魔模式中被杀，重置状态（停止BGM等）
            if (gameComponent.isRole(victim, JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                if (jesterComponent.inPsychoMode) {
                    jesterComponent.reset();
                }
            }

            // 黑帮巨星在黑帮精神期间被中途击杀时，重置状态并停止 Wathe 原生疯魔BGM。
            if (gameComponent.isRole(victim, GANGSTER_STAR)) {
                GangsterStarPlayerComponent gangsterStarComponent = GangsterStarPlayerComponent.KEY.get(victim);
                if (gangsterStarComponent.isFinalStandActive()) {
                    gangsterStarComponent.reset();
                }
            }

            // 黑警击杀处理和黑警时刻检查
            if (killer != null && gameComponent.isRole(killer, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(killer);
                if (corruptCopComp.isCorruptCopMomentActive()) {
                    corruptCopComp.onKill();
                }
            }

            // 黑警被杀时结束黑警时刻
            if (gameComponent.isRole(victim, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(victim);
                corruptCopComp.endCorruptCopMoment();
            }

            CommanderHelper.handleAfterKill(victim, killer, gameComponent);

            // 生存大师被杀时结束生存时刻
            if (gameComponent.isRole(victim, SURVIVAL_MASTER)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(victim);
                survivalComp.endSurvivalMoment();
            }

            // 饕餮被杀时释放所有被吞玩家并结束饕餮时刻
            if (gameComponent.isRole(victim, TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(victim);
                taotieComp.releaseAllPlayers(victim.getPos());
                taotieComp.endTaotieMoment();
            }

            // 被吞玩家死亡后的特殊处理（不生成尸体，播放打嗝音效）
            SwallowedPlayerComponent victimSwallowed = SwallowedPlayerComponent.KEY.get(victim);
            if (victimSwallowed.isSwallowed()) {
                UUID taotieUuid = victimSwallowed.getSwallowedBy();
                if (taotieUuid != null && victim.getWorld() instanceof ServerWorld serverWorld2) {
                    PlayerEntity taotie = serverWorld2.getPlayerByUuid(taotieUuid);
                    if (taotie != null) {
                        TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                        taotieComp.removeSwallowedPlayer((ServerPlayerEntity) victim);
                        // 被吞玩家无论因何死亡，饕餮都回补消化份量的饥饿值
                        taotieComp.addDigestReward();
                        // 播放打嗝音效
                        serverWorld2.playSound(null, taotie.getBlockPos(),
                                SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.5F, 0.8F);
                    }
                }
                NoellesrolesVoiceChatPlugin.removeFromVoiceChat(victim.getUuid());
                victimSwallowed.reset();
                TrainVoicePlugin.addPlayer(victim.getUuid());
            }

            bomberPlayerComponent.reset();
        });

        ShouldPunishGunShooter.EVENT.register((shooter, victim) -> {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(shooter.getWorld());
            if (gameComponent.isRole(shooter, CORRUPT_COP)) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            if (gameComponent.isRole(shooter, GANGSTER_STAR)) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            if (gameComponent.isRole(shooter, HUNTER)) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            return null;
        });

        DoorInteraction.EVENT.register((DoorInteraction.DoorInteractionContext context) -> {
            if (context.isBlasted() || context.isJammed()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            if (context.isOpen()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            PlayerEntity player = context.getPlayer();

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.getWorld());
            if (gameWorld.isRole(player, Noellesroles.JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);
                if (jesterComponent.inPsychoMode) {
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                }
            }

            ItemStack handItem = context.getHandItem();
            DoorInteraction.DoorType doorType = context.getDoorType();
            if (handItem.isOf(ModItems.MASTER_KEY)) {
                if (doorType == DoorInteraction.DoorType.TRAIN_DOOR || context.requiresKey()) {
                    playMasterKeyDoorSound(context);
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                }
            }
            if (handItem.isOf(ModItems.NEUTRAL_MASTER_KEY) && (doorType == DoorInteraction.DoorType.TRAIN_DOOR || context.requiresKey())) {
                if (player.getItemCooldownManager().isCoolingDown(ModItems.NEUTRAL_MASTER_KEY)) {
                    return DoorInteraction.DoorInteractionResult.DENY;
                }
                Role playerRole = gameWorld.getRole(player);
                if (NEUTRAL_MASTER_KEY_ROLES.contains(playerRole)) {
                    player.getItemCooldownManager().set(ModItems.NEUTRAL_MASTER_KEY, 200);
                    playMasterKeyDoorSound(context);
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                } else if (gameWorld.isRole(player, Noellesroles.JUNKER)) {
                    // 拾荒者：搜刮所得的中立万能钥匙开门后即销毁，因此不设冷却
                    playMasterKeyDoorSound(context);
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                } else if ((gameWorld.isRole(player, Noellesroles.CORRUPT_COP) || gameWorld.isRole(player, Noellesroles.GANGSTER_STAR))
                        && doorType == DoorInteraction.DoorType.SMALL_DOOR) {
                    player.getItemCooldownManager().set(ModItems.NEUTRAL_MASTER_KEY, 200);
                    playMasterKeyDoorSound(context);
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                }
            }
            return DoorInteraction.DoorInteractionResult.PASS;
        });

        // 工程师维修工具 DoorInteraction 处理（非潜行的修复/解锁；潜行上锁在 RepairToolItem#useOnBlock，
        // 因为潜行手持物品时原版会跳过方块交互、不触发该事件）
        DoorInteraction.EVENT.register((DoorInteraction.DoorInteractionContext context) -> {
            if (!context.getHandItem().isOf(ModItems.REPAIR_TOOL)) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            if (!context.isServerSide()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }

            DoorBlockEntity entity = context.getEntity();
            // 正常门：按普通开关门处理
            if (!entity.isBlasted() && !entity.isJammed()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }

            PlayerEntity player = context.getPlayer();
            if (player.getItemCooldownManager().isCoolingDown(ModItems.REPAIR_TOOL)) {
                return DoorInteraction.DoorInteractionResult.DENY;
            }
            RepairToolItem.applyToDoor(player, context.getWorld(), context.getLowerPos(), entity,
                    entity.isBlasted() ? RepairToolItem.DoorAction.REPAIR : RepairToolItem.DoorAction.UNLOCK);
            return DoorInteraction.DoorInteractionResult.HANDLED;
        });

        // 工程师 - 门被撬开时通知
        DoorStateChanged.BLAST.register((world, pos, doorEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
            for (UUID uuid : gameComponent.getAllWithRole(ENGINEER)) {
                PlayerEntity engineer = serverWorld.getPlayerByUuid(uuid);
                if (engineer != null && GameFunctions.isPlayerPlayingAndAlive(engineer)
                        && engineer instanceof ServerPlayerEntity serverEngineer) {
                    serverEngineer.sendMessage(Text.translatable("tip.engineer.door_blasted"), true);
                    ServerPlayNetworking.send(serverEngineer, new EngineerDoorHighlightS2CPacket(pos));
                }
            }
        });

        // 工程师 - 门被堵住时通知
        DoorStateChanged.JAM.register((world, pos, doorEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
            for (UUID uuid : gameComponent.getAllWithRole(ENGINEER)) {
                PlayerEntity engineer = serverWorld.getPlayerByUuid(uuid);
                if (engineer != null && GameFunctions.isPlayerPlayingAndAlive(engineer)
                        && engineer instanceof ServerPlayerEntity serverEngineer) {
                    serverEngineer.sendMessage(Text.translatable("tip.engineer.door_jammed"), true);
                    ServerPlayNetworking.send(serverEngineer, new EngineerDoorHighlightS2CPacket(pos));
                }
            }
        });

        // 游戏结束时触发
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            MurderMayhemWorldComponent murderMayhemComponent = MurderMayhemWorldComponent.KEY.get(world);
            MurderMayhemEvent currentEvent = murderMayhemComponent.getCurrentEvent();
            if (currentEvent != null && world instanceof ServerWorld serverWorld) {
                currentEvent.onRoundFinalize(serverWorld, gameComponent);
            }
            murderMayhemComponent.reset();
            MurderMayhemWorldComponent.KEY.sync(world);
            if (world instanceof ServerWorld serverWorld) {
                DeathArenaServerController.forceShutdown(serverWorld, true);
                BreacherPlayerComponent.cleanupMarkers(serverWorld);
                OpeningKnockbackResistanceHelper.reset(serverWorld.getServer());
                PushAttributionTracker.reset();
            }
            HiddenBodiesWorldComponent.KEY.get(world).reset();
            BodyForgeryWorldComponent.KEY.get(world).reset();
            PuppetWorldComponent.KEY.get(world).reset();
            if (world instanceof ServerWorld serverWorld) {
                PuppeteerControl.discardAllEntities(serverWorld);
            }
            LooseEndsRadarWorldComponent.KEY.get(world).reset();
            LooseEndsRadarWorldComponent.KEY.sync(world);
            AntidotePlateShieldComponent plateShieldComp = AntidotePlateShieldComponent.KEY.get(world);
            plateShieldComp.reset();
            AntidotePlateShieldComponent.KEY.sync(world);
            SleepingPillPlateComponent pillPlateComp = SleepingPillPlateComponent.KEY.get(world);
            pillPlateComp.reset();
            SleepingPillPlateComponent.KEY.sync(world);
            if (world instanceof ServerWorld serverWorld) {
                for (var entity : serverWorld.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.entity.ThrowingAxeEntity.class), e -> true)) {
                    entity.discard();
                }
            }

            // 重置BGM组件，确保对局结束后不会继续播放
            WorldMusicComponent worldMusic = WorldMusicComponent.KEY.get(world);
            worldMusic.stopMusic();
        });

        // Enable RoleAssigned.EVENT deferral BEFORE game initialization starts
        // The mixin will intercept and store assignments instead of firing events
        GameEvents.ON_GAME_START.register(gameMode -> {
            DualRoleSelectionWorldComponent.enableDeferral();
            // 每局重掷杀手阵营共享的对讲机频道
            WalkieTalkieChannels.rollForNewRound();
        });

        GameEvents.ON_FINISH_INITIALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            DeathArenaServerController.resetForNewRound(serverWorld.getServer());
            JunkerLootWorldComponent.KEY.get(serverWorld).reset();
            PuppetWorldComponent.KEY.get(serverWorld).reset();

            if (MurderMayhemHelper.isMurderMayhem(gameComponent.getGameMode())) {
                MurderMayhemWorldComponent murderMayhemComponent = MurderMayhemWorldComponent.KEY.get(world);
                MurderMayhemEvent event = MurderMayhemEventRegistry.selectRandom(serverWorld);
                murderMayhemComponent.setCurrentEventId(event.id());
                murderMayhemComponent.startIntroWindow(MURDER_MAYHEM_INTRO_TOTAL_TICKS);
                event.onRoundInitialized(serverWorld, gameComponent);
                MurderMayhemWorldComponent.KEY.sync(world);
            } else {
                MurderMayhemWorldComponent murderMayhemComponent = MurderMayhemWorldComponent.KEY.get(world);
                murderMayhemComponent.reset();
                MurderMayhemWorldComponent.KEY.sync(world);
            }

            // Dual role selection - after all roles assigned, before game starts
            DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(world);
            dsComp.startSelection(serverWorld, gameComponent);
            DualRoleSelectionWorldComponent.KEY.sync(world);

            // If dual role was skipped (not enough roles), fire pending events immediately
            if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.INACTIVE) {
                dsComp.firePendingEvents(serverWorld, gameComponent);
                dsComp.markOpeningPhaseApplied();
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (gameComponent.isRole(player, WatheRoles.LOOSE_END)) {
                        DeathArenaStateHelper.applyLooseEndsOpeningState(player, gameComponent);
                    }
                }
            }
        });

        // 每 tick 检查时刻触发条件（tick 循环驱动，替代事件驱动）
        // 推人坠车归属：跟踪被推者是否离地/落地
        ServerTickEvents.END_SERVER_TICK.register(PushAttributionTracker::tick);

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            // 开局刀冷却结束即摘掉击退抗性（冷却在双角色选择期间被冻结，不能按世界时间推算）
            OpeningKnockbackResistanceHelper.tick(world.getServer());
            for (ServerPlayerEntity player : world.getPlayers()) {
                LooseEndPlayerComponent looseEndComponent = LooseEndPlayerComponent.KEY.get(player);
                if (looseEndComponent.isOpeningPhased()) {
                    looseEndComponent.tickOpeningPhaseState();
                }
            }
            if (world instanceof ServerWorld serverWorld) {
                DeathArenaServerController.tick(serverWorld);
            }

            GameWorldComponent gc = GameWorldComponent.KEY.get(world);
            DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(world);

            if (!gc.isRunning()) {
                // 倒计时中途停局/服务器重启会让 phase 永远停在 SELECTING，冻结也就一直不解除
                if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.SELECTING) {
                    dsComp.cancelSelection();
                    dsComp.markOpeningPhaseApplied(); // 对局已结束，别在此时补发义警开局阶段
                    DualRoleSelectionWorldComponent.KEY.sync(world);
                }
                return;
            }

            // Dual role selection tick - drives countdown and applies results
            if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.SELECTING) {
                if (dsComp.tickSelection()) {
                    if (world instanceof ServerWorld sw) {
                        dsComp.applyResults(sw, gc);
                    }
                    DualRoleSelectionWorldComponent.KEY.sync(world);
                } else {
                    // 选择期间体力冻结在开局状态（0、疲惫），选完后与原版开局一样从 0 起回复
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        Role role = gc.getRole(player);
                        if (role == null || role.getMaxSprintTime() < 0) continue;
                        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
                        if (stamina.getSprintingTicks() != 0f) stamina.setSprintingTicks(0f);
                        if (!stamina.isExhausted()) stamina.setExhausted(true);
                    }
                }
            }

            // Apply delayed opening phase after selection completes (or if no selection)
            if (!dsComp.isOpeningPhaseApplied() && dsComp.getPhase() != DualRoleSelectionWorldComponent.Phase.SELECTING && world instanceof ServerWorld sw) {
                dsComp.markOpeningPhaseApplied();
                for (ServerPlayerEntity player : sw.getPlayers()) {
                    if (gc.isRole(player, WatheRoles.LOOSE_END)) {
                        DeathArenaStateHelper.applyLooseEndsOpeningState(player, gc);
                    }
                }
                DualRoleSelectionWorldComponent.KEY.sync(world);
            }

            if (world instanceof ServerWorld serverWorld && MurderMayhemHelper.isMurderMayhem(gc.getGameMode())) {
                MurderMayhemWorldComponent murderMayhemComponent = MurderMayhemWorldComponent.KEY.get(world);
                boolean shouldSync = false;
                // 选择期间介绍窗口与事件时间轴暂停，选完再开始推进
                if (dsComp.getPhase() != DualRoleSelectionWorldComponent.Phase.SELECTING) {
                    if (murderMayhemComponent.tickIntroWindow()) {
                        shouldSync = true;
                    }
                    MurderMayhemEvent currentEvent = murderMayhemComponent.getCurrentEvent();
                    if (currentEvent != null && currentEvent.onRoundTick(serverWorld, gc)) {
                        shouldSync = true;
                    }
                }
                if (shouldSync) {
                    MurderMayhemWorldComponent.KEY.sync(world);
                }
                if (murderMayhemComponent.getCurrentEvent() instanceof org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent) {
                    for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                        HallucinationHelper.tickPlayer(player, serverWorld, gc, murderMayhemComponent);
                        HallucinationHelper.tickSleepRecovery(player);
                    }
                }
            }
            if (world.getServer().getTicks() % 5 != 0) return;
            checkAndTriggerMomentsForWorld(world);

            FerrymanHelper.handleTick(world, gc);
            // 列车长身份广播延后到选择结束，否则广播会被选择界面挡住
            if (dsComp.getPhase() != DualRoleSelectionWorldComponent.Phase.SELECTING) {
                CommanderHelper.tryBroadcastIdentity(world);
            }
            CommanderHelper.checkLastKillerDeath(world);
        });

        // 游戏胜利确定时，杀死所有被饕餮吞噬的玩家
        GameEvents.ON_WIN_DETERMINED.register((world, gameComponent, winStatus, neutralWinner) -> {
            for (var trap : world.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.entity.HunterTrapEntity.class), e -> true)) {
                trap.discard();
            }
            for (UUID taotieUuid : gameComponent.getAllWithRole(TAOTIE)) {
                ServerPlayerEntity taotie = (ServerPlayerEntity) world.getPlayerByUuid(taotieUuid);
                if (taotie != null && GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                    List<UUID> swallowedPlayers = taotieComp.getSwallowedPlayers();
                    for (UUID swallowedUuid : swallowedPlayers) {
                        ServerPlayerEntity swallowed = (ServerPlayerEntity) world.getPlayerByUuid(swallowedUuid);
                        if (GameFunctions.isPlayerPlayingAndAlive(swallowed)) {
                            GameFunctions.killPlayer(swallowed, false, taotie, DEATH_REASON_DIGESTED);
                        }
                    }
                }
            }
        });

        // 对局胜负确定时更新所有玩家的杀手历史（供动态权重使用）
        GameEvents.ON_WIN_DETERMINED.register((world, gameComponent, winStatus, neutralWinner) -> {
            for (Map.Entry<UUID, Role> entry : gameComponent.getRoles().entrySet()) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(entry.getKey());
                if (player == null) continue; // 中途离场的玩家无法更新实体组件（已知限制）
                boolean wasKillerTeam = entry.getValue() != null && entry.getValue().canUseKiller();
                KillerHistoryPlayerComponent.KEY.get(player).recordGame(wasKillerTeam);
            }
        });

        // 对局胜负确定时记录每名玩家的最终角色（供开局角色抽取避开近3局使用）
        GameEvents.ON_WIN_DETERMINED.register((world, gameComponent, winStatus, neutralWinner) -> {
            for (Map.Entry<UUID, Role> entry : gameComponent.getRoles().entrySet()) {
                Role role = entry.getValue();
                if (role == null || role.equals(WatheRoles.NO_ROLE)) continue;
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(entry.getKey());
                if (player == null) continue; // 中途离场的玩家无法更新实体组件（已知限制）
                RoleHistoryPlayerComponent.KEY.get(player).recordRole(role.identifier());
            }
        });
    }

    /** /wathe:KillerHistory set 执行：设置玩家"没玩到杀手的局数"（streak=0 表示清除历史） */
    private static int setKillerHistory(ServerCommandSource source, ServerPlayerEntity target, int streak) {
        KillerHistoryPlayerComponent.KEY.get(target).setConsecutiveNonKillerGames(streak);
        String name = target.getName().getString();
        if (streak <= 0) {
            source.sendFeedback(() -> Text.literal("已清除 " + name + " 的杀手历史记录。"), true);
        } else {
            source.sendFeedback(() -> Text.literal("已将 " + name + " 的未玩杀手场次设置为 " + streak + " 局。"), true);
        }
        return 1;
    }

    /** /wathe:KillerHistory get 执行：查看连空场次、近 6 场杀手次数与当前权重 */
    private static int getKillerHistory(ServerCommandSource source, ServerPlayerEntity target) {
        KillerHistoryPlayerComponent history = KillerHistoryPlayerComponent.KEY.get(target);
        int streak = history.getConsecutiveNonKillerGames();
        int recent = history.getKillerGamesLast6();
        double weight = KillerHistoryCalculator.computeKillerWeight(streak, recent);
        source.sendFeedback(() -> Text.literal(target.getName().getString()
                + "：连续未玩杀手 " + streak + " 局，近 6 场杀手 " + recent + " 次，当前杀手权重 "
                + String.format("%.2f", weight) + "x"), false);
        return 1;
    }

    /** /wathe:RoleHistory set 执行：直接设置玩家的近 3 局角色（空格分隔的角色 id，可省略命名空间） */
    private static int setRoleHistory(ServerCommandSource source, ServerPlayerEntity target, String rawRoles) {
        List<Identifier> parsed = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        for (String token : rawRoles.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            Role role = resolveRoleToken(token);
            if (role == null) {
                notFound.add(token);
            } else {
                parsed.add(role.identifier());
            }
        }
        if (parsed.isEmpty()) {
            source.sendError(Text.literal("未找到任何有效角色：" + String.join(", ", notFound) + "。"));
            return 0;
        }
        RoleHistoryPlayerComponent.KEY.get(target).setRecentRoles(parsed);
        if (!notFound.isEmpty()) {
            source.sendError(Text.literal("已设置 " + parsed.size() + " 个，未找到：" + String.join(", ", notFound) + "。"));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("已将 " + target.getName().getString()
                + " 的近 3 局角色设置为 " + parsed.size() + " 个。"), true);
        return 1;
    }

    /** /wathe:RoleHistory get 执行：查看玩家的近 3 局角色 */
    private static int getRoleHistory(ServerCommandSource source, ServerPlayerEntity target) {
        List<Identifier> recent = RoleHistoryPlayerComponent.KEY.get(target).getRecentRoleIdentifiers();
        List<String> ids = new ArrayList<>();
        for (Identifier id : recent) {
            ids.add(id.toString());
        }
        String feedback = ids.isEmpty() ? "（无记录）" : String.join(", ", ids);
        source.sendFeedback(() -> Text.literal(target.getName().getString() + "：近 3 局角色：" + feedback), false);
        return 1;
    }

    /** /wathe:RoleHistory clear 执行：清除玩家的近 3 局角色历史 */
    private static int clearRoleHistory(ServerCommandSource source, ServerPlayerEntity target) {
        RoleHistoryPlayerComponent.KEY.get(target).clear();
        source.sendFeedback(() -> Text.literal("已清除 " + target.getName().getString() + " 的近 3 局角色历史。"), true);
        return 1;
    }

    /** 把命令 token 解析为 Role：完整 id 直接匹配；无命名空间时尝试 noellesroles:/wathe: 前缀 */
    private static Role resolveRoleToken(String token) {
        if (token.indexOf(':') < 0) {
            for (String namespace : new String[]{"noellesroles", "wathe"}) {
                try {
                    Role role = findRoleByIdentifier(Identifier.of(namespace, token));
                    if (role != null) return role;
                } catch (IllegalArgumentException ignored) {
                    // 非法字符的角色名按未找到处理
                }
            }
            return null;
        }
        Identifier id = Identifier.tryParse(token);
        return id == null ? null : findRoleByIdentifier(id);
    }

    private static Role findRoleByIdentifier(Identifier id) {
        for (Role role : WatheRoles.ROLES) {
            if (role.identifier().equals(id)) return role;
        }
        for (Role role : ALL_ROLES) {
            if (role.identifier().equals(id)) return role;
        }
        return null;
    }

    // 万能钥匙开门走 ALLOW，Wathe 只为 USE_KEY 播音效，这里补上与普通钥匙相同的开门声
    private static void playMasterKeyDoorSound(DoorInteraction.DoorInteractionContext context) {
        net.minecraft.util.math.BlockPos lowerPos = context.getLowerPos();
        context.getWorld().playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f,
                WatheSounds.ITEM_KEY_DOOR, SoundCategory.BLOCKS, 1f, 1f);
    }

    private static void movePlayerIntoReadyArea(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null || server == null) {
            return;
        }

        GameFunctions.teleportPlayer(player);

        ServerWorld world = player.getServerWorld();
        dev.doctor4t.wathe.cca.MapVariablesWorldComponent mapVariables = dev.doctor4t.wathe.cca.MapVariablesWorldComponent.KEY.get(world);
        Box readyArea = mapVariables.getReadyArea();
        if (readyArea != null && !readyArea.contains(player.getPos())) {
            Vec3d center = readyArea.getCenter();
            player.teleport(world, center.getX(), readyArea.minY + 1, center.getZ(), player.getYaw(), player.getPitch());
        }

        GameFunctions.setPlayerSpawnToMapSpawn(player, world);
    }

    // 推理成功 5 秒后的广播：警类/中立/杀手阵营存活玩家 + 所有旁观者（不含推理学家自己）
    private static void broadcastCriminalReasonerNotice(ServerPlayerEntity reasoner) {
        if (!(reasoner.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);
        // 5 秒内回合已结束（重置/新局）则不再补发
        if (!gameWorld.isRunning()) {
            return;
        }
        Text notice = Text.translatable("tip.criminal_reasoner.notice_broadcast");
        for (ServerPlayerEntity target : serverWorld.getPlayers()) {
            if (target.getUuid().equals(reasoner.getUuid())) continue;
            boolean spectatorViewer = target.isSpectator();
            boolean factionAlive = GameFunctions.isPlayerPlayingAndAlive(target)
                    && gameWorld.hasAnyRole(target.getUuid())
                    && (isPoliceRole(target) || !gameWorld.getRole(target).isInnocent());
            if (spectatorViewer || factionAlive) {
                target.sendMessage(notice, true);
            }
        }
    }

    // 死者/嫌疑人可能已退出游戏（离线），优先取在线名，再回退到本局存档的 GameProfile 名
    private static String resolveReasoningPlayerName(ServerPlayerEntity reasoner, UUID uuid) {
        PlayerEntity online = reasoner.getWorld().getPlayerByUuid(uuid);
        if (online != null) {
            return online.getName().getString();
        }
        var profile = GameWorldComponent.KEY.get(reasoner.getWorld()).getGameProfiles().get(uuid);
        if (profile != null) {
            return profile.getName();
        }
        return Text.translatable("screen.criminal_reasoner.unknown_player").getString();
    }

    public void registerPackets() {
        // O 键配置界面：OP 在线修改 gameplay 服务端选项（写服务端配置文件并广播新值）
        ServerPlayNetworking.registerGlobalReceiver(ServerConfigUpdateC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            context.server().execute(() -> {
                if (!requester.hasPermissionLevel(2)) {
                    requester.sendMessage(Text.translatable("tip.noellesroles.config.no_permission"), false);
                    return;
                }
                NoellesRolesConfig config = NoellesRolesConfig.HANDLER.instance();
                config.insanePlayersSeeMorphs = payload.insanePlayersSeeMorphs();
                config.voodooNonKillerDeaths = payload.voodooNonKillerDeaths();
                config.showFogRadiusHud = payload.showFogRadiusHud();
                config.showHallucinationHud = payload.showHallucinationHud();
                config.skipModeVoting = payload.skipModeVoting();
                config.skipMapVoting = payload.skipMapVoting();
                config.generalCooldownTicks = Math.max(0, Math.min(20 * 60 * 10, payload.generalCooldownTicks()));
                config.matchLengthSeconds = Math.max(1, Math.min(999999, payload.matchLengthSeconds()));
                config.whiteKnightChancePercent = Math.max(0, Math.min(100, payload.whiteKnightChancePercent()));
                config.misKillGunCooldown = payload.misKillGunCooldown();
                NoellesRolesConfig.HANDLER.save();
                for (ServerWorld world : context.server().getWorlds()) {
                    ConfigWorldComponent.KEY.get(world).sync();
                }
                requester.sendMessage(Text.translatable("tip.noellesroles.config.server_saved"), false);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(HallucinationDummyHitC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            context.server().execute(() -> {
                HallucinationPlayerComponent component = HallucinationPlayerComponent.KEY.get(requester);
                GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(requester.getWorld());
                GangsterStarPlayerComponent gangsterStarComponent = GangsterStarPlayerComponent.KEY.get(requester);
                boolean gangsterStarGunShot = gameWorldComponent.isRole(requester, GANGSTER_STAR)
                        && requester.getMainHandStack().isOf(WatheItems.REVOLVER)
                        && GameConstants.DeathReasons.GUN.equals(payload.deathReason());
                if (gangsterStarGunShot && !gangsterStarComponent.hasRevolverShot()) {
                    requester.sendMessage(Text.translatable("tip.noellesroles.gangster_star.no_shots"), true);
                    // 空仓音效与德林加一致（服务端广播，周围玩家也能听见）；冷却中保持静默
                    if (!requester.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER)) {
                        requester.getWorld().playSound(null, requester.getX(), requester.getEyeY(), requester.getZ(),
                                WatheSounds.ITEM_REVOLVER_CLICK, SoundCategory.PLAYERS,
                                0.5f, 1f + requester.getRandom().nextFloat() * .1f - .05f);
                    }
                    return;
                }
                if (component.handleDummyHit(payload)) {
                    if (gangsterStarGunShot) {
                        // 幻觉假人命中不走 Wathe 的 GunShootPayload，服务端确认命中后在这里扣除左轮次数。
                        gangsterStarComponent.consumeRevolverShot();
                    }
                    ServerPlayNetworking.send(
                            requester,
                            new HallucinationDummyStateS2CPacket(
                                    List.of(),
                                    List.of(payload.dummyId()),
                                    component.getFakeBodyIds(),
                                    component.getFakeBodyDeathReasons()
                            )
                    );
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(HallucinationDummyUseC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            context.server().execute(() -> {
                HallucinationPlayerComponent component = HallucinationPlayerComponent.KEY.get(requester);
                if (component.handleDummyUse(payload)) {
                    component.sync();
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(SpectatorInfoRequestC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(requester.getWorld());
            if (!SpectatorStateHelper.isInGameRealSpectator(requester, gameWorld)
                    || DeathArenaStateHelper.isDeathArenaParticipant(requester)) {
                return;
            }

            GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
            if (match == null) {
                match = GameRecordManager.getLastFinishedMatch();
            }
            long matchStartTick = match != null ? match.getStartTick() : -1L;
            Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache =
                    match != null ? ReplayGenerator.getPlayerInfoCache(match) : Map.of();
            // Spectator sync formats events directly through ReplayRegistry, so refresh default formatter cache here.
            DefaultReplayFormatters.setPlayerInfoCache(playerInfoCache);
            List<SpectatorInfoSyncS2CPacket.ReplayToast> replayToasts =
                    buildSpectatorReplayToasts(match, payload.lastSeenReplayTick(), playerInfoCache, requester.getServerWorld());
            long latestReplayTick = getLatestReplayTick(match);

            List<SpectatorInfoSyncS2CPacket.Entry> entries = new ArrayList<>();
            for (UUID uuid : gameWorld.getAllPlayers()) {
                entries.add(buildSpectatorEntry(uuid, match, requester.getServerWorld(), playerInfoCache));
            }
            ServerPlayNetworking.send(requester, new SpectatorInfoSyncS2CPacket(payload.requestId(), matchStartTick, latestReplayTick, entries, replayToasts));
        });

        ServerPlayNetworking.registerGlobalReceiver(SpectatorReplayDetailRequestC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(requester.getWorld());
            if (!SpectatorStateHelper.isInGameRealSpectator(requester, gameWorld)
                    || DeathArenaStateHelper.isDeathArenaParticipant(requester)) {
                return;
            }

            if (!gameWorld.getAllPlayers().contains(payload.targetUuid())) {
                ServerPlayNetworking.send(requester, new SpectatorReplayDetailSyncS2CPacket(payload.requestId(), payload.targetUuid(), List.of()));
                return;
            }

            GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
            if (match == null) {
                match = GameRecordManager.getLastFinishedMatch();
            }
            Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache =
                    match != null ? ReplayGenerator.getPlayerInfoCache(match) : Map.of();
            DefaultReplayFormatters.setPlayerInfoCache(playerInfoCache);
            List<String> replayLines = buildSpectatorReplayLines(payload.targetUuid(), match, requester.getServerWorld());
            ServerPlayNetworking.send(requester, new SpectatorReplayDetailSyncS2CPacket(payload.requestId(), payload.targetUuid(), replayLines));
        });

        ServerPlayNetworking.registerGlobalReceiver(SpectatorAssistTeleportC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(requester.getWorld());
            if (!SpectatorStateHelper.isInGameRealSpectator(requester, gameWorld)
                    || DeathArenaStateHelper.isDeathArenaParticipant(requester)) {
                return;
            }

            UUID targetUuid = payload.targetUuid();
            if (!gameWorld.getAllPlayers().contains(targetUuid)) {
                return;
            }

            PlayerEntity liveTarget = requester.getServerWorld().getPlayerByUuid(targetUuid);
            if (liveTarget instanceof ServerPlayerEntity && !gameWorld.isPlayerDead(targetUuid)) {
                requester.networkHandler.onSpectatorTeleport(new net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket(targetUuid));
                return;
            }

            Vec3d deathPos = findSpectatorDeathPosition(targetUuid, requester.getServerWorld());
            if (deathPos == null) {
                return;
            }

            requester.setCameraEntity(requester);
            if (requester.hasVehicle()) {
                requester.stopRiding();
            }
            requester.requestTeleport(deathPos.x, deathPos.y, deathPos.z);
        });

        ServerPlayNetworking.registerGlobalReceiver(DeathArenaToggleC2SPacket.ID, (payload, context) ->
                context.server().execute(() -> DeathArenaServerController.handleToggle(context.player()))
        );

        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.MORPH_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());

            if (payload.player() == null) return;
            // 技能冷却拦截只针对变形者；巫毒娃娃绑定不受拾荒者搜刮冷却等限制
            if (abilityPlayerComponent.cooldown > 0 && gameWorldComponent.isRole(context.player(), MORPHLING)) return;
            PlayerEntity abilityTarget = context.player().getWorld().getPlayerByUuid(payload.player());

            if (abilityTarget != null && (gameWorldComponent.isRole(context.player(), VOODOO) || gameWorldComponent.isRole(context.player(), JUNKER)) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (SwallowedInteractionHelper.blocksPlayerTarget(abilityTarget, SwallowedInteractionHelper.TargetingRule.VOODOO_TARGET)) return;
                // 绑定/更换目标消耗一个巫毒娃娃；未持有则不生效
                if (!VoodooHelper.consumeDoll(context.player())) return;
                VoodooPlayerComponent voodooPlayerComponent = (VoodooPlayerComponent) VoodooPlayerComponent.KEY.get(context.player());
                voodooPlayerComponent.setTarget(payload.player());
                ServerPlayerEntity recordTarget = abilityTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                GameRecordManager.recordSkillUse(context.player(), VOODOO_ID, recordTarget, null);

            }
            // 变形者允许变形为已死亡玩家（旁观者），只需目标在线即可
            if (abilityTarget != null && gameWorldComponent.isRole(context.player(), MORPHLING) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (SwallowedInteractionHelper.blocksPlayerTarget(abilityTarget)) return;
                if (!gameWorldComponent.getAllPlayers().contains(payload.player())) return;
                MorphlingPlayerComponent morphlingPlayerComponent = (MorphlingPlayerComponent) MorphlingPlayerComponent.KEY.get(context.player());
                // 服务端验证冷却是否结束，防止作弊
                if (morphlingPlayerComponent.getMorphTicks() != 0) return;
                morphlingPlayerComponent.startMorph(payload.player());
                ServerPlayerEntity recordTarget = abilityTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "morph");
                extra.putUuid("disguise_as", payload.player());
                GameRecordManager.recordSkillUse(context.player(), MORPHLING_ID, recordTarget, extra);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(MorphCorpseToggleC2SPacket.ID, (payload, context) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            if (!gameWorldComponent.isRole(context.player(), MORPHLING)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(context.player())) return;

            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(context.player());
            comp.toggleCorpseMode();
        });
        // 变形者「赝造药水」：把选人界面指定的玩家写入手持药水
        ServerPlayNetworking.registerGlobalReceiver(ForgedPotionSelectC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
                if (!gameWorldComponent.isRole(player, MORPHLING)) return;
                if (!GameFunctions.isPlayerPlayingAndAlive(player) || SwallowedPlayerComponent.isPlayerSwallowed(player)) return;
                if (payload.target() == null || !gameWorldComponent.getAllPlayers().contains(payload.target())) return;

                ItemStack stack = player.getMainHandStack();
                if (!stack.isOf(ModItems.FORGED_POTION)) {
                    stack = player.getOffHandStack();
                    if (!stack.isOf(ModItems.FORGED_POTION)) return;
                }

                PlayerEntity target = player.getWorld().getPlayerByUuid(payload.target());
                String targetName = target != null ? target.getName().getString() : payload.target().toString();
                ForgedPotionItem.setTarget(stack, payload.target(), targetName);
                player.playerScreenHandler.sendContentUpdates();
                player.sendMessage(Text.translatable("tip.noellesroles.forged_potion.selected", targetName), true);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.VULTURE_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(context.player());

            if (gameWorldComponent.isRole(context.player(), VULTURE) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (abilityPlayerComponent.getCooldown() > 0) return;
                List<PlayerBodyEntity> playerBodyEntities = context.player().getWorld().getEntitiesByType(TypeFilter.equals(PlayerBodyEntity.class), context.player().getBoundingBox().expand(5), (playerBodyEntity -> {
                    return playerBodyEntity.getUuid().equals(payload.playerBody())
                            && BodyTargetHelper.canPlayerSeeBody(context.player(), playerBodyEntity);
                }));
                if (!playerBodyEntities.isEmpty()) {
                    PlayerBodyEntity body = playerBodyEntities.getFirst();
                    UUID bodyPlayerUuid = body.getPlayerUuid();
                    Vec3d bodyPos = body.getPos();
                    abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 5));
                    VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(context.player());
                    vulturePlayerComponent.addBody(body.getUuid());

                    // 生成粒子效果
                    if (context.player().getWorld() instanceof ServerWorld serverWorld) {
                        Vec3d pos = body.getPos();
                        serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.5, pos.z, 30, 0.3, 0.3, 0.3, 0.02);
                        serverWorld.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.01);
                    }

                    // 吃掉尸体后获得速度加成
                    context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, GameConstants.getInTicks(0, 10), 2, false, false, true));

                    // 吃掉尸体后获得10秒透视
                    vulturePlayerComponent.activateHighlight(GameConstants.getInTicks(0, 10));

                    // 移除尸体
                    body.discard();
                    ServerPlayerEntity recordTarget = null;
                    if (bodyPlayerUuid != null) {
                        PlayerEntity targetPlayer = context.player().getServerWorld().getPlayerByUuid(bodyPlayerUuid);
                        if (targetPlayer instanceof ServerPlayerEntity serverTarget) {
                            recordTarget = serverTarget;
                        }
                    }
                    NbtCompound extra = new NbtCompound();
                    extra.putUuid("body_uuid", body.getUuid());
                    if (bodyPlayerUuid != null) {
                        extra.putUuid("body_player_uuid", bodyPlayerUuid);
                        extra.putUuid("target", bodyPlayerUuid);
                    }
                    GameRecordManager.putPos(extra, "body_pos", bodyPos);
                    GameRecordManager.recordSkillUse(context.player(), VULTURE_ID, recordTarget, extra);
                }

            }
        });
        // 傀儡师：傀儡术 / 操纵 / 切回本体 / 回收
        ServerPlayNetworking.registerGlobalReceiver(PuppeteerActionC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                switch (payload.action()) {
                    case CONVERT -> {
                        if (payload.target() != null) {
                            PuppeteerControl.tryConvert(player, payload.target());
                        }
                    }
                    case CONTROL -> PuppeteerControl.tryControl(player, payload.target());
                    case RETURN -> PuppeteerControl.tryReturn(player, false);
                    case RECALL -> PuppeteerControl.tryReturn(player, true);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(JunkerSearchC2SPacket.ID, (payload, context) -> {
            context.server().execute(() ->
                    JunkerSearchHelper.handleSearchRequest(context.player(), payload.playerBody()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SWAP_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            if (gameWorldComponent.isRole(context.player(), SWAPPER) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (payload.player() != null) {
                    if (context.player().getWorld().getPlayerByUuid(payload.player()) != null) {
                        if (payload.player2() != null) {
                            if (context.player().getWorld().getPlayerByUuid(payload.player2()) != null) {

                                // 傀儡（以其生前玩家选中）换的是场内的傀儡；正在操纵傀儡的傀儡师换的是其本体替身
                                Entity player1 = PuppeteerControl.resolvePositionalTarget(context.player().getServerWorld(), payload.player());
                                Entity player2 = PuppeteerControl.resolvePositionalTarget(context.player().getServerWorld(), payload.player2());

                                if (player1 == null)
                                    return;
                                if (player2 == null)
                                    return;
                                if (player1 instanceof PlayerEntity swapPlayer1 && SwallowedPlayerComponent.isPlayerSwallowed(swapPlayer1))
                                    return;
                                if (player2 instanceof PlayerEntity swapPlayer2 && SwallowedPlayerComponent.isPlayerSwallowed(swapPlayer2))
                                    return;

                                if (player1 instanceof LivingEntity living1 && living1.isSleeping()) {
                                    living1.wakeUp();
                                }
                                if (player2 instanceof LivingEntity living2 && living2.isSleeping()) {
                                    living2.wakeUp();
                                }

                                if (player1.hasVehicle()) {
                                    player1.stopRiding();
                                }
                                if (player2.hasVehicle()) {
                                    player2.stopRiding();
                                }

                                Vec3d swappedPos1 = player1.getPos();
                                Vec3d swappedPos2 = player2.getPos();
                                var x1 = swappedPos1.x;
                                var y1 = swappedPos1.y;
                                var z1 = swappedPos1.z;
                                var yaw1 = player1.getYaw();
                                var pitch1 = player1.getPitch();
                                var world1 = (ServerWorld) player1.getWorld();
                                var x2 = swappedPos2.x;
                                var y2 = swappedPos2.y;
                                var z2 = swappedPos2.z;
                                var yaw2 = player2.getYaw();
                                var pitch2 = player2.getPitch();
                                var world2 = (ServerWorld) player2.getWorld();

//                                if (!context.player().getWorld().isSpaceEmpty(player1)) return;
//                                if (!context.player().getWorld().isSpaceEmpty(player2)) return;

                                Set<PositionFlag> movementFlags = EnumSet.noneOf(PositionFlag.class);
                                boolean swapped = false;
                                if (player1.teleport(world2, x2, y2, z2, movementFlags, yaw2, pitch2)) {
                                    AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(context.player());
                                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 0);
                                    abilityPlayerComponent.sync();
                                    if (!(player1 instanceof LivingEntity flying1 && flying1.isFallFlying())) {
                                        player1.setVelocity(player1.getVelocity().multiply(1.0, 0.0, 1.0));
                                        player1.setOnGround(true);
                                    }
                                    if (player2.teleport(world1, x1, y1, z1, movementFlags, yaw1, pitch1)) {
                                        if (!(player2 instanceof LivingEntity flying2 && flying2.isFallFlying())) {
                                            player2.setVelocity(player1.getVelocity().multiply(1.0, 0.0, 1.0));
                                            player2.setOnGround(true);
                                        }
                                        swapped = true;
                                    }
                                }
                                if (swapped) {
                                    NbtCompound extra = new NbtCompound();
                                    extra.putString("action", "swap");
                                    extra.putUuid("target1", payload.player());
                                    extra.putUuid("target2", payload.player2());
                                    GameRecordManager.putPos(extra, "target1_pos", swappedPos1);
                                    GameRecordManager.putPos(extra, "target2_pos", swappedPos2);
                                    ServerPlayerEntity recordTarget = player1 instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                                    GameRecordManager.recordSkillUse(context.player(), SWAPPER_ID, recordTarget, extra);
                                }
                            }
                        }
                    }
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ABILITY_PACKET, (payload, context) -> {
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            if (abilityPlayerComponent.getCooldown() > 0) {
                return;
            }
            if (gameWorldComponent.isRole(context.player(), SERIAL_KILLER) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                SerialKillerPlayerComponent.KEY.get(context.player()).tryUseSkill();
                return;
            }
            if (gameWorldComponent.isRole(context.player(), SAINT) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(context.player());
                if (!saintComponent.isHellfireActive()) {
                    saintComponent.activateHellfire();
                }
                return;
            }
            if (gameWorldComponent.isRole(context.player(), FERRYMAN) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                FerrymanPlayerComponent ferrymanComponent = FerrymanPlayerComponent.KEY.get(context.player());
                if (ferrymanComponent.isReactionActive()) {
                    FerrymanPlayerComponent.ReactionResult result = ferrymanComponent.triggerReaction();
                    if (!result.success()) {
                        return;
                    }

                    abilityPlayerComponent.setCooldown(FerrymanPlayerComponent.REACTION_COOLDOWN_TICKS);
                    if (context.player().getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.SOUL, context.player().getX(), context.player().getBodyY(0.5), context.player().getZ(), 12, 0.25, 0.35, 0.25, 0.02);
                    }

                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "reaction");
                    extra.putBoolean("blessing", result.consumeBlessing());

                    ServerPlayerEntity attacker = null;
                    if (result.attackerUuid() != null) {
                        PlayerEntity found = context.player().getServerWorld().getPlayerByUuid(result.attackerUuid());
                        if (found instanceof ServerPlayerEntity serverAttacker) {
                            attacker = serverAttacker;
                        }
                    }

                    if (attacker != null) {
                        extra.putUuid("attacker", attacker.getUuid());
                    }

                    if (attacker != null && result.deathReason() == GameConstants.DeathReasons.KNIFE) {
                        AbilityPlayerComponent.KEY.get(attacker).markKnifeCooldownOverride(GameConstants.getInTicks(0, 10));
                    }
                    if (attacker != null && result.reactionType() == FerrymanPlayerComponent.ReactionType.COUNTER_SWALLOW) {
                        TaotiePlayerComponent.KEY.get(attacker).setSwallowCooldown(GameConstants.getInTicks(0, 10));
                    }

                    if (result.consumeBlessing()) {
                        if (attacker != null) {
                            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FerrymanPlayerComponent.COUNTER_STUN_TICKS, FerrymanHelper.COUNTER_STUN_AMPLIFIER, false, true, true));
                            attacker.sendMessage(Text.translatable("tip.ferryman.attacker_warn"), true);
                        }
                        context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, FerrymanPlayerComponent.COUNTER_SPEED_TICKS, 1, false, true, true));
                    }

                    GameRecordManager.recordSkillUse(context.player(), FERRYMAN_ID, attacker, extra);
                    if (result.reactionType() == FerrymanPlayerComponent.ReactionType.COUNTER_SWALLOW) {
                        var blockedSwallowEvent = GameRecordManager.event("iron_man_activated")
                                .actor(context.player())
                                .put("action", "block_swallow");
                        if (attacker != null) {
                            blockedSwallowEvent.target(attacker);
                        }
                        blockedSwallowEvent.record();
                    } else {
                        var blockedEvent = GameRecordManager.event("death_blocked")
                                .actor(context.player())
                                .put("block_reason", "ferryman_reaction")
                                .put("death_reason", result.deathReason().toString());
                        if (attacker != null) {
                            blockedEvent.target(attacker);
                        }
                        blockedEvent.record();
                    }
                    return;
                }

                PlayerBodyEntity body = FerrymanHelper.findTargetBody(context.player(), ferrymanComponent);
                if (body == null) {
                    abilityPlayerComponent.setCooldown(FerrymanPlayerComponent.EMPTY_PENALTY_TICKS);
                    return;
                }

                if (!ferrymanComponent.addFerriedBody(body.getUuid())) {
                    abilityPlayerComponent.setCooldown(FerrymanPlayerComponent.EMPTY_PENALTY_TICKS);
                    return;
                }

                if (context.player().getWorld() instanceof ServerWorld serverWorld) {
                    FerrymanHelper.markBodyFerried(serverWorld, body);
                    Vec3d pos = body.getPos();
                    serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.45, pos.z, 12, 0.2, 0.2, 0.2, 0.01);
                    serverWorld.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.6, pos.z, 18, 0.25, 0.25, 0.25, 0.02);
                }
                context.player().getWorld().playSound(null, body.getBlockPos(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.9F, 0.75F);

                UUID bodyPlayerUuid = body.getPlayerUuid();
                ServerPlayerEntity recordTarget = null;
                if (bodyPlayerUuid != null) {
                    PlayerEntity targetPlayer = context.player().getServerWorld().getPlayerByUuid(bodyPlayerUuid);
                    if (targetPlayer instanceof ServerPlayerEntity serverTarget) {
                        recordTarget = serverTarget;
                    }
                }

                NbtCompound extra = new NbtCompound();
                extra.putString("action", "ferry");
                extra.putUuid("body_uuid", body.getUuid());
                if (bodyPlayerUuid != null) {
                    extra.putUuid("body_player_uuid", bodyPlayerUuid);
                    extra.putUuid("target", bodyPlayerUuid);
                }
                extra.putInt("count", ferrymanComponent.getFerriedCount());
                extra.putInt("required", ferrymanComponent.getFerriedRequired());
                GameRecordManager.putPos(extra, "body_pos", body.getPos());
                GameRecordManager.recordSkillUse(context.player(), FERRYMAN_ID, recordTarget, extra);
                abilityPlayerComponent.setCooldown(FerrymanPlayerComponent.FERRY_COOLDOWN_TICKS);
                return;
            }
            if (gameWorldComponent.isRole(context.player(), ORTHOPEDIST) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                PlayerEntity target = org.agmas.noellesroles.util.CrosshairTargetHelper.findCrosshairTarget(context.player(), 3.0D, 0.85D);

                if (target instanceof ServerPlayerEntity serverTarget) {
                    OrthopedistPlayerComponent.applyBoneSetting(serverTarget);
                    abilityPlayerComponent.setCooldown(GameConstants.getInTicks(1, 0));
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "bone_setting");
                    GameRecordManager.recordSkillUse(context.player(), ORTHOPEDIST_ID, serverTarget, extra);
                }
            }
            // 噬梦者：标记3格内准星瞄准的玩家（延迟10秒生效）
            if (gameWorldComponent.isRole(context.player(), DREAM_EATER) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                PlayerEntity dreamTarget = org.agmas.noellesroles.util.CrosshairTargetHelper.findCrosshairTarget(context.player(), 3.0D, 0.85D);
                if (dreamTarget instanceof ServerPlayerEntity serverTarget) {
                    DreamEaterPlayerComponent.KEY.get(serverTarget).applySkillHit(context.player().getUuid());
                    abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 30));
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "mark");
                    GameRecordManager.recordSkillUse(context.player(), DREAM_EATER_ID, serverTarget, extra);
                }
            }
            if (gameWorldComponent.isRole(context.player(), BREACHER) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                BreacherPlayerComponent breacherComp = BreacherPlayerComponent.KEY.get(context.player());
                breacherComp.tryCreatePoint();
            }
            if (gameWorldComponent.isRole(context.player(), RECALLER) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                RecallerPlayerComponent recallerPlayerComponent = RecallerPlayerComponent.KEY.get(context.player());
                PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(context.player());
                if (!recallerPlayerComponent.placed) {
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 10);
                    recallerPlayerComponent.setPosition();
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "place");
                    extra.putDouble("x", recallerPlayerComponent.x);
                    extra.putDouble("y", recallerPlayerComponent.y);
                    extra.putDouble("z", recallerPlayerComponent.z);
                    GameRecordManager.recordSkillUse(context.player(), RECALLER_ID, null, extra);
                } else if (playerShopComponent.balance >= 100) {
                    playerShopComponent.balance -= 100;
                    playerShopComponent.sync();
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 30);
                    double targetX = recallerPlayerComponent.x;
                    double targetY = recallerPlayerComponent.y;
                    double targetZ = recallerPlayerComponent.z;
                    recallerPlayerComponent.teleport();
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "teleport");
                    extra.putDouble("x", targetX);
                    extra.putDouble("y", targetY);
                    extra.putDouble("z", targetZ);
                    GameRecordManager.recordSkillUse(context.player(), RECALLER_ID, null, extra);
                }

            }
            if (gameWorldComponent.isRole(context.player(), PHANTOM) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 30 * 20, 0, true, false, true));
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 30);
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "invisible");
                GameRecordManager.recordSkillUse(context.player(), PHANTOM_ID, null, extra);
            }
            // Pathogen infection ability
            if (gameWorldComponent.isRole(context.player(), PATHOGEN) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                // Find nearest uninfected player within 3 blocks (with line of sight)
                PlayerEntity nearestTarget = null;
                double nearestDistance = 9.0; // 3^2 = 9

                for (UUID playerUuid : gameWorldComponent.getAllPlayers()) {
                    PlayerEntity player = context.player().getWorld().getPlayerByUuid(playerUuid);
                    if (player == null || player.equals(context.player())) continue;
                    if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                    if (SwallowedInteractionHelper.blocksPlayerTarget(player)) continue;

                    InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                    if (infected.isInfected()) continue;

                    // 傀儡无需也无法感染：正在操纵傀儡的傀儡师只能通过其本体替身被感染
                    Entity infectAnchor = PuppeteerControl.getBodyAnchor(player);
                    double distance = context.player().squaredDistanceTo(infectAnchor);
                    if (distance < nearestDistance) {
                        // 检查视线（不能隔墙感染）
                        if (context.player().canSee(infectAnchor)) {
                            nearestDistance = distance;
                            nearestTarget = player;
                        }
                    }
                }

                // Infect the nearest target
                if (nearestTarget != null) {
                    InfectedPlayerComponent targetInfected = InfectedPlayerComponent.KEY.get(nearestTarget);
                    targetInfected.setInfected(true, context.player().getUuid());

                    // Set cooldown based on player count (calculated at game start)
                    PathogenPlayerComponent pathogenComp = PathogenPlayerComponent.KEY.get(context.player());
                    abilityPlayerComponent.setCooldown(pathogenComp.getBaseCooldownTicks());
                    ServerPlayerEntity recordTarget = nearestTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "infect");
                    GameRecordManager.recordSkillUse(context.player(), PATHOGEN_ID, recordTarget, extra);
                }
            }
            // 大嗓门广播技能：语音广播给所有玩家10秒，CD 180秒
            if (gameWorldComponent.isRole(context.player(), NOISEMAKER) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                NoisemakerPlayerComponent noisemakerComp = NoisemakerPlayerComponent.KEY.get(context.player());
                noisemakerComp.startBroadcasting();
                abilityPlayerComponent.setCooldown(GameConstants.getInTicks(3, 0)); // 180秒 = 3分钟
                context.player().sendMessage(net.minecraft.text.Text.translatable("noellesroles.noisemaker.broadcast_start"), true);
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "broadcast");
                GameRecordManager.recordSkillUse(context.player(), NOISEMAKER_ID, null, extra);
            }
            // 保镖技能:空手进入潜行姿态 / 姿态中再按取消(冷却早退在接收器开头,姿态激活期间冷却恒为0故可取消)
            if (gameWorldComponent.isRole(context.player(), BODYGUARD)) {
                ServerPlayerEntity bodyguardPlayer = context.player();
                BodyguardPlayerComponent bodyguardComp = BodyguardPlayerComponent.KEY.get(bodyguardPlayer);
                if (bodyguardComp.isStanceActive()) {
                    // 双击防抖:进入后至少 2 tick 才允许取消
                    if (bodyguardComp.getStanceStartedTick() < 0
                            || bodyguardPlayer.getServerWorld().getTime() - bodyguardComp.getStanceStartedTick() >= 2) {
                        bodyguardComp.cancelStance();
                    }
                } else if (GameFunctions.isPlayerPlayingAndAlive(bodyguardPlayer)
                        && !SwallowedPlayerComponent.isPlayerSwallowed(bodyguardPlayer)) {
                    if (bodyguardPlayer.getMainHandStack().isEmpty()) {
                        bodyguardComp.startStance();
                    } else {
                        bodyguardPlayer.sendMessage(Text.translatable("tip.bodyguard.need_empty_hand"), true);
                    }
                }
                return;
            }
            // 强盗技能：一次性掷出背包内全部飞斧（面前横向无空隙排开同时掷出），使用后冷却90秒
            if (gameWorldComponent.isRole(context.player(), BANDIT) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (BanditSkillHelper.tryVolley(context.player())) {
                    abilityPlayerComponent.setCooldown(BanditSkillHelper.VOLLEY_COOLDOWN_TICKS);
                }
                return;
            }
        });

        // 刺客猜测角色（唯一需要服务器处理的数据包）
        // 书模式（刺客之书）：刺客与拾荒者可用，跳过技能次数/冷却校验，改由书自身消耗与冷却控制
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ASSASSIN_GUESS_ROLE_PACKET, (payload, context) -> {
            ServerPlayerEntity guesser = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(guesser.getWorld());

            boolean fromBook = payload.fromBook();
            boolean isAssassin = gameWorldComponent.isRole(guesser, ASSASSIN);
            boolean isJunker = gameWorldComponent.isRole(guesser, JUNKER);

            // 验证角色和状态
            if (!isAssassin && !(fromBook && isJunker)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(guesser)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(guesser)) return;

            AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(guesser);
            if (fromBook) {
                // 书模式：要求持有书且书不在冷却中
                if (guesser.getItemCooldownManager().isCoolingDown(ModItems.ASSASSIN_BOOK)) return;
                if (!AssassinBookHelper.hasBook(guesser)) return;
            } else if (!assassinComp.canGuess()) {
                return;
            }

            // 验证目标
            ServerPlayerEntity target = (ServerPlayerEntity) guesser.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (SwallowedInteractionHelper.blocksPlayerTarget(target, SwallowedInteractionHelper.TargetingRule.ASSASSIN_GUESS_TARGET)) return;

            // 🔒 关键安全验证：防止恶意客户端猜测不可猜测的角色
            if (target.equals(guesser)) return;  // 不能猜测自己
            if (gameWorldComponent.isRole(target, WatheRoles.VIGILANTE)) return;  // 义警不能被猜测
            Role targetRole = gameWorldComponent.getRole(target);
            if (targetRole == null) return;
            if (WatheRoles.SPECIAL_ROLES.contains(targetRole)) return;  // 特殊角色不能被猜测
            if (targetRole.equals(ASSASSIN)) return;  // 不能猜测其他刺客

            // 判断猜测是否正确
            boolean guessedCorrectly = targetRole.identifier().equals(payload.guessedRole());

            if (fromBook) {
                // 先消耗并进入冷却：拾荒者反噬身亡时用掉的书不应留在尸体上
                AssassinBookHelper.consumeBook(guesser);
                guesser.getItemCooldownManager().set(ModItems.ASSASSIN_BOOK, AssassinBookItem.COOLDOWN_TICKS);
            }

            // 执行结果
            if (guessedCorrectly) {
                // 发送消息（通过 actionbar 显示）- 先发送消息再击杀
                guesser.sendMessage(
                        net.minecraft.text.Text.translatable("tip.assassin.guess_correct", target.getName())
                                .formatted(net.minecraft.util.Formatting.GREEN, net.minecraft.util.Formatting.BOLD),
                        true
                );

                // 播放枪响音效（对所有玩家可见）
                guesser.getWorld().playSound(
                        null,  // 所有人都能听到
                        target.getX(), target.getY(), target.getZ(),
                        WatheSounds.ITEM_REVOLVER_SHOOT,
                        SoundCategory.PLAYERS,
                        2.0F,  // 音量
                        1.0F   // 音调
                );

                // 猜对：杀死目标
                GameFunctions.killPlayer(target, true, guesser, DEATH_REASON_ASSASSINATED);
            } else if (fromBook && isAssassin) {
                // 刺客用书猜错：书挡下反噬，仅红字提示，无枪声、不死亡
                guesser.sendMessage(
                        net.minecraft.text.Text.translatable("tip.assassin.book_guess_wrong")
                                .formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD),
                        true
                );
            } else {
                // 技能猜错、或拾荒者用书猜错（书的力量反噬拾荒者）
                net.minecraft.text.MutableText wrongText = fromBook
                        ? net.minecraft.text.Text.translatable("tip.junker.book_guess_wrong")
                        : net.minecraft.text.Text.translatable("tip.assassin.guess_wrong", target.getName());
                guesser.sendMessage(
                        wrongText.formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD),
                        true
                );

                // 播放枪响音效（对所有玩家可见）
                guesser.getWorld().playSound(
                        null,  // 所有人都能听到
                        guesser.getX(), guesser.getY(), guesser.getZ(),
                        WatheSounds.ITEM_REVOLVER_SHOOT,
                        SoundCategory.PLAYERS,
                        2.0F,  // 音量
                        1.0F   // 音调
                );
                // 猜错：自己死亡
                GameFunctions.killPlayer(guesser, true, null, DEATH_REASON_ASSASSIN_MISFIRE);
            }

            if (fromBook) {
                NbtCompound extra = new NbtCompound();
                extra.putString("guessed_role", payload.guessedRole().toString());
                extra.putBoolean("correct", guessedCorrectly);
                GameRecordManager.recordItemUse(guesser, Registries.ITEM.getId(ModItems.ASSASSIN_BOOK), target, extra);
            } else {
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "guess");
                extra.putString("guessed_role", payload.guessedRole().toString());
                extra.putBoolean("correct", guessedCorrectly);
                GameRecordManager.recordSkillUse(guesser, ASSASSIN_ID, target, extra);

                // 消耗猜测次数，设置冷却
                assassinComp.useGuess();
            }
        });

        // 谎言音效师：选定玩家与音效，以该玩家为中心向附近所有玩家播放该音效
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.PHONISM_PACKET, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity phonism = context.player();
                GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(phonism.getWorld());
                AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(phonism);

                // 验证角色和状态
                if (!gameWorldComponent.isRole(phonism, PHONISM)) return;
                if (!GameFunctions.isPlayerPlayingAndAlive(phonism)) return;
                if (SwallowedPlayerComponent.isPlayerSwallowed(phonism)) return;
                if (abilityPlayerComponent.cooldown > 0) return;

                // 验证音效，客户端传来的键必须在服务端枚举中
                PhonismSound sound = PhonismSound.byKey(payload.sound());
                if (sound == null) return;

                // 验证目标
                if (payload.targetPlayer() == null) return;
                PlayerEntity target = phonism.getWorld().getPlayerByUuid(payload.targetPlayer());
                if (target == null) return;
                if (target.equals(phonism)) return;
                if (SwallowedInteractionHelper.blocksPlayerTarget(target, SwallowedInteractionHelper.TargetingRule.PHONISM_TARGET)) return;

                // 选出音效即消耗技能：目标已死亡时同样进入冷却，但不播放任何声音
                abilityPlayerComponent.setCooldown(GameConstants.getInTicks(1, 0));

                // 附近所有玩家都能听到；带时长限制的音效由 playAt 负责到点停掉
                // 目标是场内傀儡时在傀儡处播放，操纵傀儡中的傀儡师则在其本体替身处播放
                if (target.getWorld() instanceof ServerWorld soundWorld
                        && (GameFunctions.isPlayerPlayingAndAlive(target)
                        || PuppetWorldComponent.get(soundWorld).isPuppetInWorld(target.getUuid()))) {
                    Entity soundAnchor = PuppeteerControl.resolvePositionalTarget(soundWorld, target.getUuid());
                    if (soundAnchor != null) {
                        sound.playAt(soundWorld, soundAnchor.getX(), soundAnchor.getY(), soundAnchor.getZ());
                    }
                }

                NbtCompound extra = new NbtCompound();
                extra.putString("sound", sound.key());
                GameRecordManager.recordSkillUse(phonism, PHONISM_ID, target instanceof ServerPlayerEntity serverTarget ? serverTarget : null, extra);
            });
        });

        // 记者标记目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.REPORTER_MARK_PACKET, (payload, context) -> {
            ServerPlayerEntity reporter = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(reporter.getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(reporter);

            // 验证角色和状态
            if (!gameWorldComponent.isRole(reporter, REPORTER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(reporter)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(reporter)) return;
            if (abilityPlayerComponent.cooldown > 0) return;

            // 验证目标
            if (payload.targetPlayer() == null) return;
            PlayerEntity target = reporter.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(reporter)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (SwallowedInteractionHelper.blocksPlayerTarget(target)) return;

            // 验证距离（3格内）
            double distance = reporter.squaredDistanceTo(target);
            if (distance > 9.0) return; // 3^2 = 9

            // 验证视线
            if (!reporter.canSee(target)) return;

            // 设置标记
            ReporterPlayerComponent reporterComp = ReporterPlayerComponent.KEY.get(reporter);
            reporterComp.setMarkedTarget(target.getUuid());
            // 设置冷却30秒
            abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 30));
            NbtCompound extra = new NbtCompound();
            extra.putString("action", "mark");
            GameRecordManager.recordSkillUse(reporter, REPORTER_ID, target instanceof ServerPlayerEntity serverTarget ? serverTarget : null, extra);
        });

        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.COMMANDER_MARK_PACKET, (payload, context) -> {
            ServerPlayerEntity commander = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(commander.getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(commander);
            CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(commander);

            if (!gameWorldComponent.isRole(commander, COMMANDER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(commander)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(commander)) return;
            if (abilityPlayerComponent.cooldown > 0) return;
            if (!commanderComp.canMarkMore()) return;
            if (payload.targetPlayer() == null) return;

            ServerPlayerEntity target = (ServerPlayerEntity) commander.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null || target.equals(commander)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (SwallowedInteractionHelper.blocksPlayerTarget(target)) return;
            if (commanderComp.isThreatTarget(target.getUuid())) return;

            String targetName = target.getName().getString();
            if (!commanderComp.addThreatTarget(target.getUuid(), targetName)) return;

            abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 30));
            for (ServerPlayerEntity player : commander.getServerWorld().getPlayers()) {
                if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                if (!CommanderHelper.isNotificationRecipient(gameWorldComponent, player)) continue;
                player.sendMessage(Text.translatable("tip.commander.target_marked", target.getName()), true);
            }

            NbtCompound extra = new NbtCompound();
            extra.putString("action", "mark");
            extra.putUuid("target", target.getUuid());
            GameRecordManager.recordSkillUse(commander, COMMANDER_ID, target, extra);
        });

        // 犯罪推理学家推理目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.CRIMINAL_REASONER_REASON_PACKET, (payload, context) -> {
            ServerPlayerEntity criminalReasoner = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(criminalReasoner.getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(criminalReasoner);

            // 验证角色和状态
            if (!gameWorldComponent.isRole(criminalReasoner, CRIMINAL_REASONER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(criminalReasoner)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(criminalReasoner)) return;
            if (abilityPlayerComponent.cooldown > 0) return;

            // 验证推理对象，死者必须已死亡，嫌疑人则允许是存活或死亡玩家。
            // 退出游戏者（escaped）已离线，死者与嫌疑人实体都可能为 null，因此按 UUID 判定。
            if (payload.victimPlayer() == null || payload.suspectPlayer() == null) return;
            UUID victimUuid = payload.victimPlayer();
            UUID suspectUuid = payload.suspectPlayer();
            if (victimUuid.equals(criminalReasoner.getUuid())) return;
            if (!gameWorldComponent.isPlayerDead(victimUuid)) return;
            PlayerEntity suspect = criminalReasoner.getWorld().getPlayerByUuid(suspectUuid);
            if (suspect != null && SwallowedInteractionHelper.blocksPlayerTarget(suspect, SwallowedInteractionHelper.TargetingRule.CRIMINAL_REASONER_SUSPECT)) return;

            CriminalReasonerPlayerComponent criminalReasonerComponent = CriminalReasonerPlayerComponent.KEY.get(criminalReasoner);
            boolean reasonSuccess = criminalReasonerComponent.isCorrectReasoning(victimUuid, suspectUuid);

            if (reasonSuccess) {
                boolean newSuccess = criminalReasonerComponent.recordSuccessfulReasoning(victimUuid);
                // 每次新推理成功 5 秒后，向警类/中立/杀手阵营与所有旁观者广播（不含自己）
                if (newSuccess) {
                    Scheduler.schedule(() -> broadcastCriminalReasonerNotice(criminalReasoner), 100);
                }
                criminalReasoner.sendMessage(
                        Text.translatable("tip.criminal_reasoner.guess_success",
                                        resolveReasoningPlayerName(criminalReasoner, victimUuid),
                                        resolveReasoningPlayerName(criminalReasoner, suspectUuid))
                                .formatted(net.minecraft.util.Formatting.GREEN),
                        true
                );
            } else {
                criminalReasoner.sendMessage(
                        Text.translatable("tip.criminal_reasoner.guess_wrong")
                                .formatted(net.minecraft.util.Formatting.RED),
                        true
                );
            }

            // 推理成功固定 5 秒冷却，推理失败固定 60 秒冷却。
            abilityPlayerComponent.setCooldown(reasonSuccess
                    ? GameConstants.getInTicks(0, 5)
                    : GameConstants.getInTicks(1, 0));

            NbtCompound extra = new NbtCompound();
            extra.putString("action", "reason");
            extra.putUuid("victim_uuid", victimUuid);
            extra.putUuid("suspect_uuid", suspectUuid);
            extra.putBoolean("success", reasonSuccess);
            GameRecordManager.recordSkillUse(criminalReasoner, CRIMINAL_REASONER_ID, suspect instanceof ServerPlayerEntity serverSuspect ? serverSuspect : null, extra);
        });

        // 犯罪推理学家隐身（技能键2）
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.CRIMINAL_REASONER_INVISIBILITY_PACKET, (payload, context) -> {
            ServerPlayerEntity criminalReasoner = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(criminalReasoner.getWorld());

            if (!gameWorldComponent.isRole(criminalReasoner, CRIMINAL_REASONER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(criminalReasoner)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(criminalReasoner)) return;
            // 隐身期间禁止再次激活，避免浪费充能
            if (criminalReasoner.hasStatusEffect(StatusEffects.INVISIBILITY)) return;

            CriminalReasonerPlayerComponent criminalReasonerComponent = CriminalReasonerPlayerComponent.KEY.get(criminalReasoner);
            if (!criminalReasonerComponent.consumeInvisibilityCharge()) return;

            criminalReasoner.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 20, 0, true, false, true));

            NbtCompound extra = new NbtCompound();
            extra.putString("action", "invisible");
            extra.putInt("charges_left", criminalReasonerComponent.getInvisibilityCharges());
            GameRecordManager.recordSkillUse(criminalReasoner, CRIMINAL_REASONER_ID, null, extra);
        });

        //饕餮吞噬
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.TAOTIE_SWALLOW_PACKET, (payload, context) -> {
            ServerPlayerEntity taotie = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(taotie.getWorld());

            // 验证角色和状态
            if (!gameWorldComponent.isRole(taotie, TAOTIE)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(taotie)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(taotie)) return;

            TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
            if (taotieComp.getSwallowCooldown() > 0) return;

            // 验证目标
            if (payload.targetPlayer() == null) return;
            ServerPlayerEntity target = (ServerPlayerEntity) taotie.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(taotie)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;
            if (SwallowedInteractionHelper.blocksPlayerTarget(target)) return;
            // 傀儡已经是尸体：饕餮无需也无法吞噬傀儡形态的傀儡师
            if (PuppeteerPlayerComponent.isControllingPuppet(target)) return;

            // 验证距离（3格内）
            double distance = taotie.squaredDistanceTo(target);
            if (distance > TaotiePlayerComponent.SWALLOW_DISTANCE_SQUARED) return;

            // 验证视线
            if (!taotie.canSee(target)) return;

            if (FerrymanHelper.beginSwallowReaction(target, taotie, gameWorldComponent)) {
                return;
            }

            // 执行吞噬
            if (taotieComp.swallowPlayer(target)) {
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "swallow");
                GameRecordManager.recordSkillUse(taotie, TAOTIE_ID, target, extra);
            }
        });

        // 静语者沉默目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SILENCER_SILENCE_PACKET, (payload, context) -> {
            ServerPlayerEntity silencer = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(silencer.getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(silencer);
            SilencerPlayerComponent silencerComp = SilencerPlayerComponent.KEY.get(silencer);

            // 验证角色和状态
            if (!gameWorldComponent.isRole(silencer, SILENCER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(silencer)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(silencer)) return;
            if (abilityPlayerComponent.cooldown > 0) return;

            // 检查是否已有标记目标
            if (silencerComp.hasMarkedTarget()) {
                // 已有标记 → 执行沉默（不判断瞄准/距离/视线）
                ServerPlayerEntity target = (ServerPlayerEntity) silencer.getWorld().getPlayerByUuid(silencerComp.getMarkedTargetUuid());
                // 清除标记
                silencerComp.clearMark();

                if (target == null) return;
                if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
                if (SwallowedInteractionHelper.blocksPlayerTarget(target)) return;

                // 检查目标是否已被沉默
                SilencedPlayerComponent silencedComp = SilencedPlayerComponent.KEY.get(target);
                if (silencedComp.isSilenced()) return;

                // 执行沉默
                silencedComp.applySilence(silencer.getUuid());

                // 设置冷却45秒
                abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 45));

                // 给静语者发送成功提示
                silencer.sendMessage(
                        Text.translatable("tip.silencer.success", target.getName())
                                .formatted(net.minecraft.util.Formatting.GRAY),
                        true
                );

                // 记录技能使用
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "silence");
                GameRecordManager.recordSkillUse(silencer, SILENCER_ID, target, extra);
                return;
            }

            // 没有有效标记 → 进行标记（需要瞄准目标）
            if (payload.targetPlayer() == null) return;
            ServerPlayerEntity target = (ServerPlayerEntity) silencer.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(silencer)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (SwallowedInteractionHelper.blocksPlayerTarget(target)) return;

            // 验证距离（3格内）
            double distance = silencer.squaredDistanceTo(target);
            if (distance > 9.0) return; // 3^2 = 9

            // 验证视线
            if (!silencer.canSee(target)) return;

            // 检查目标是否已被沉默
            SilencedPlayerComponent silencedComp = SilencedPlayerComponent.KEY.get(target);
            if (silencedComp.isSilenced()) {
                silencer.sendMessage(
                        Text.translatable("tip.silencer.already_silenced", target.getName())
                                .formatted(net.minecraft.util.Formatting.GRAY),
                        true
                );
                return;
            }

            // 标记目标（通过组件，自动同步到客户端）
            silencerComp.markTarget(target.getUuid(), target.getName().getString());

            // 给静语者发送标记提示
            silencer.sendMessage(
                    Text.translatable("tip.silencer.marked", target.getName())
                            .formatted(net.minecraft.util.Formatting.GRAY),
                    true
            );
        });

        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.DUAL_ROLE_CHOICE_PACKET, (payload, context) -> {
            context.server().execute(() -> {
                DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(context.player().getWorld());
                if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.SELECTING) {
                    dsComp.handlePlayerChoice(context.player(), payload.choseSecond());
                    DualRoleSelectionWorldComponent.KEY.sync(context.player().getWorld());
                }
            });
        });

        // 孟婆开局「渡人 / 渡己」选择
        ServerPlayNetworking.registerGlobalReceiver(MengPoChoiceC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, MENGPO)) return;
                if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;
                if (!MengPoPlayerComponent.KEY.get(player).isChoosing()) return;
                MengPoHelper.applyChoice(player, payload.duji());
            });
        });

        // 乘务员在登记面板中修改某位乘客的登记信息
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ATTENDANT_EDIT_PACKET, (payload, context) -> {
            ServerPlayerEntity attendant = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(attendant.getWorld());
            if (!gameWorldComponent.isRole(attendant, ATTENDANT)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(attendant)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(attendant)) return;

            UUID targetUuid = payload.target();
            AttendantPlayerComponent attendantComponent = AttendantPlayerComponent.KEY.get(attendant);
            if (attendantComponent.getEntry(targetUuid) == null) return;

            String value = payload.value();
            switch (payload.field()) {
                case 0, 1 -> {
                    if (!value.equals("0") && !value.equals("1") && !value.equals("2")) return;
                }
                case 2 -> {
                    if (!value.isEmpty()) {
                        if (value.length() > 64) return;
                        Identifier roleId = Identifier.tryParse(value);
                        if (roleId == null) return;
                        Role role = null;
                        for (Role candidate : WatheRoles.ROLES) {
                            if (candidate.identifier().equals(roleId)) {
                                role = candidate;
                                break;
                            }
                        }
                        if (role == null || !gameWorldComponent.isRoleEnabled(role)) return;
                    }
                }
                default -> {
                    return;
                }
            }
            attendantComponent.setField(targetUuid, payload.field(), value);
        });
    }

    private static SpectatorInfoSyncS2CPacket.Entry buildSpectatorEntry(UUID targetUuid,
                                                                         GameRecordManager.MatchRecord match,
                                                                         ServerWorld world,
                                                                         Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache) {
        int balance = resolveSpectatorBalance(world.getServer().getPlayerManager().getPlayer(targetUuid));
        if (match == null) {
            return new SpectatorInfoSyncS2CPacket.Entry(targetUuid, "", 0xFFAAAAAA, "", -1L, -1, -1L, "", balance);
        }

        long nowTick = world.getTime();
        String deathReasonRaw = "";
        long deathTick = -1L;
        long latestRelevantReplayTick = -1L;
        String replaySummary = "";
        ReplayGenerator.PlayerInfo playerInfo = playerInfoCache.get(targetUuid);
        String roleTranslationKey = playerInfo != null ? playerInfo.roleTranslationKey() : "";
        int roleColor = playerInfo != null ? (0xFF000000 | playerInfo.roleColor()) : 0xFFAAAAAA;
        List<GameRecordEvent> events = new ArrayList<>(match.getEvents());
        events.sort(Comparator.comparingLong(GameRecordEvent::worldTick));

        for (GameRecordEvent event : events) {
            if (GameRecordTypes.DEATH.equals(event.type())
                    && event.data().containsUuid("target")
                    && targetUuid.equals(event.data().getUuid("target"))) {
                deathReasonRaw = event.data().getString("death_reason");
                deathTick = event.worldTick();
            }

            if (!isReplayEventRelatedToPlayer(targetUuid, event)) {
                continue;
            }

            Text formattedLine = formatSpectatorReplayLine(event, match, world);
            if (formattedLine != null) {
                latestRelevantReplayTick = event.worldTick();
                // 序列化为 JSON 组件发送，由客户端按自身语言设置本地化
                replaySummary = Text.Serialization.toJsonString(formattedLine, world.getRegistryManager());
            }
        }

        int deathAgeSeconds = deathTick >= 0 ? (int) Math.max(0, (nowTick - deathTick) / 20L) : -1;

        return new SpectatorInfoSyncS2CPacket.Entry(
                targetUuid,
                roleTranslationKey,
                roleColor,
                deathReasonRaw == null ? "" : deathReasonRaw,
                deathTick,
                deathAgeSeconds,
                latestRelevantReplayTick,
                replaySummary,
                balance
        );
    }

    /** 观战面板的金币显示：只有拥有金币系统（商店条目非空）的在线玩家才返回余额，否则返回 -1 表示不显示。 */
    private static int resolveSpectatorBalance(ServerPlayerEntity target) {
        if (target == null || !ShopUtils.canAccessShop(target)) {
            return -1;
        }
        return PlayerShopComponent.KEY.get(target).getBalance();
    }

    private static Vec3d findSpectatorDeathPosition(UUID targetUuid, ServerWorld world) {
        GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
        if (match == null) {
            match = GameRecordManager.getLastFinishedMatch();
        }
        if (match == null) {
            return null;
        }

        List<GameRecordEvent> events = new ArrayList<>(match.getEvents());
        events.sort(Comparator.comparingLong(GameRecordEvent::worldTick));

        for (int i = events.size() - 1; i >= 0; i--) {
            GameRecordEvent event = events.get(i);
            if (!GameRecordTypes.DEATH.equals(event.type())) {
                continue;
            }
            NbtCompound data = event.data();
            if (!data.containsUuid("target") || !targetUuid.equals(data.getUuid("target"))) {
                continue;
            }
            if (data.contains("death_pos")) {
                NbtCompound deathPos = data.getCompound("death_pos");
                if (deathPos.contains("x") && deathPos.contains("y") && deathPos.contains("z")) {
                    return new Vec3d(deathPos.getDouble("x"), deathPos.getDouble("y"), deathPos.getDouble("z"));
                }
            }
            break;
        }

        for (PlayerBodyEntity body : world.getEntitiesByType(
                TypeFilter.equals(PlayerBodyEntity.class),
                world.getWorldBorder().asVoxelShape().getBoundingBox(),
                body -> targetUuid.equals(body.getPlayerUuid()))) {
            return body.getPos();
        }

        return null;
    }

    private static List<String> buildSpectatorReplayLines(UUID targetUuid,
                                                          GameRecordManager.MatchRecord match,
                                                          ServerWorld world) {
        if (match == null) {
            return List.of();
        }

        List<String> replayLines = new ArrayList<>();
        List<GameRecordEvent> events = new ArrayList<>(match.getEvents());
        events.sort(Comparator.comparingLong(GameRecordEvent::worldTick));

        for (GameRecordEvent event : events) {
            if (!isReplayEventRelatedToPlayer(targetUuid, event)) {
                continue;
            }

            Text formattedLine = formatSpectatorReplayLine(event, match, world);
            if (formattedLine != null) {
                // 序列化为 JSON 组件发送，由客户端按自身语言设置本地化
                replayLines.add(Text.Serialization.toJsonString(formattedLine, world.getRegistryManager()));
            }
        }

        return replayLines;
    }

    private static Text formatSpectatorReplayLine(GameRecordEvent event,
                                                  GameRecordManager.MatchRecord match,
                                                  ServerWorld world) {
        ReplayEventFormatter formatter = ReplayRegistry.getFormatter(event.type());
        if (formatter == null) {
            return null;
        }

        Text formatted = formatter.format(event, match, world);
        if (formatted == null) {
            return null;
        }

        String time = ReplayGenerator.formatTime(event.worldTick(), match.getStartTick());
        return Text.literal("[" + time + "]").append(formatted);
    }

    private static boolean isReplayEventRelatedToPlayer(UUID targetUuid, GameRecordEvent event) {
        NbtCompound data = event.data();
        if (data.containsUuid("actor") && targetUuid.equals(data.getUuid("actor"))) {
            return true;
        }
        if (data.containsUuid("target") && targetUuid.equals(data.getUuid("target"))) {
            return true;
        }
        if (GameRecordTypes.ROLE_ASSIGNED.equals(event.type()) && data.contains("player")) {
            NbtCompound player = data.getCompound("player");
            return player.containsUuid("uuid") && targetUuid.equals(player.getUuid("uuid"));
        }
        return false;
    }

    private static long getLatestReplayTick(GameRecordManager.MatchRecord match) {
        if (match == null || match.getEvents().isEmpty()) {
            return -1L;
        }
        return match.getEvents().stream()
                .mapToLong(GameRecordEvent::worldTick)
                .max()
                .orElse(-1L);
    }

    private static List<SpectatorInfoSyncS2CPacket.ReplayToast> buildSpectatorReplayToasts(GameRecordManager.MatchRecord match,
                                                                                            long lastSeenReplayTick,
                                                                                            Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache,
                                                                                            ServerWorld world) {
        if (match == null) {
            return List.of();
        }

        List<SpectatorInfoSyncS2CPacket.ReplayToast> toasts = new ArrayList<>();
        List<GameRecordEvent> events = new ArrayList<>(match.getEvents());
        events.sort(Comparator.comparingLong(GameRecordEvent::worldTick));

        for (GameRecordEvent event : events) {
            if (event.worldTick() <= lastSeenReplayTick) {
                continue;
            }
            if (!GameRecordTypes.DEATH.equals(event.type())) {
                continue;
            }

            NbtCompound data = event.data();
            if (!data.containsUuid("target")) {
                continue;
            }

            UUID targetUuid = data.getUuid("target");
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            ReplayGenerator.PlayerInfo actorInfo = actorUuid != null ? playerInfoCache.get(actorUuid) : null;
            ReplayGenerator.PlayerInfo targetInfo = playerInfoCache.get(targetUuid);
            String actorRoleKey = actorInfo != null ? actorInfo.roleTranslationKey() : "";
            String targetRoleKey = targetInfo != null ? targetInfo.roleTranslationKey() : "screen.spectator_assist_panel.role_unknown";
            String deathReasonRaw = data.getString("death_reason");

            toasts.add(new SpectatorInfoSyncS2CPacket.ReplayToast(
                    event.worldTick(),
                    actorRoleKey,
                    targetRoleKey,
                    deathReasonRaw == null ? "" : deathReasonRaw
            ));
        }

        int maxToasts = 6;
        if (toasts.size() > maxToasts) {
            return List.copyOf(toasts.subList(toasts.size() - maxToasts, toasts.size()));
        }
        return toasts;
    }

    /** 安眠药取餐回放：显示蓝色「投药」；非安眠药事件委托回 wathe 默认实现。 */
    private static void registerSleepingPillPlatterTakeFormatter() {
        ReplayRegistry.registerFormatter(GameRecordTypes.PLATTER_TAKE, (event, match, world) -> {
            NbtCompound data = event.data();
            if (!data.getBoolean("sleeping_pill")) {
                return dev.doctor4t.wathe.record.replay.DefaultReplayFormatters.formatPlatterTake(event, match, world);
            }
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID poisonerUuid = data.containsUuid("poisoner") ? data.getUuid("poisoner") : null;
            if (actorUuid == null || poisonerUuid == null) {
                return null;
            }
            Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text poisonerText = ReplayGenerator.formatPlayerName(poisonerUuid, playerInfoCache);
            Text itemName = ReplayGenerator.formatItemName(data, world);
            return Text.translatable("replay.platter_take.sleeping_pill", actorText, itemName, poisonerText);
        });
    }

    /**
     * 注册 DLC 回放格式化器
     */
    private void registerReplayFormatters() {
        // 小丑时刻/禁锢 全局事件格式化器（显示触发者）
        ReplayEventFormatter jesterTriggerFormatter = (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String eventId = data.getString("event");
            Identifier id = Identifier.tryParse(eventId);
            String translationKey = id != null ? "replay.global." + id.getNamespace() + "." + id.getPath() : "replay.global.unknown";

            if (data.containsUuid("trigger")) {
                Text triggerText = ReplayGenerator.formatPlayerName(data.getUuid("trigger"), playerInfoCache);
                return Text.translatable(translationKey, actorText, triggerText);
            }
            return Text.translatable(translationKey, actorText);
        };
        ReplayRegistry.registerGlobalEventFormatter(Identifier.of(MOD_ID, "jester_moment_start"), jesterTriggerFormatter);
        ReplayRegistry.registerGlobalEventFormatter(Identifier.of(MOD_ID, "jester_stasis_start"), jesterTriggerFormatter);

        // death_blocked 格式化器
        ReplayRegistry.registerFormatter("death_blocked", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            String blockReason = data.getString("block_reason");

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            // 铁人药剂和威士忌护盾由各自专属事件处理，此处返回null避免重复
            if ("iron_man_buff".equals(blockReason) || "whiskey_shield".equals(blockReason)) return null;

            if ("riot_shield".equals(blockReason)) {
                if (targetUuid == null) return null;
                Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                return Text.translatable("replay.death_blocked.riot_shield", actorText, targetText);
            }

            // 根据 block_reason 选择翻译键
            String translationKey = switch (blockReason) {
                case "corrupt_cop_moment" -> "replay.death_blocked.corrupt_cop_moment";
                case "taotie_moment" -> "replay.death_blocked.taotie_moment";
                case "jester_stasis" -> "replay.death_blocked.jester_stasis";
                case "ferryman_reaction" -> "replay.death_blocked.ferryman_reaction";
                default -> "replay.death_blocked.unknown";
            };

            return Text.translatable(translationKey, actorText);
        });

        // iron_man_activated 铁人药剂生效格式化器
        ReplayRegistry.registerFormatter("gangster_spirit_triggered", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;

            Text victimText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text shooterText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.gangster_spirit_triggered", victimText, shooterText);
        });

        // gun_miss 枪械开火未命中格式化器（按物品 ID 取子翻译键）
        ReplayRegistry.registerFormatter("gun_miss", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            String itemIdStr = data.getString("item");
            Identifier itemId = itemIdStr.isEmpty() ? null : Identifier.tryParse(itemIdStr);
            if (actorUuid == null || itemId == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.gun_miss." + itemId.getNamespace() + "." + itemId.getPath(), actorText);
        });

        ReplayRegistry.registerFormatter("iron_man_activated", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            String action = data.getString("action");

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            String translationKey = switch (action) {
                case "block_swallow" -> "replay.iron_man_activated.block_swallow";
                case "block_damage" -> "replay.iron_man_activated.block_damage";
                default -> "replay.iron_man_activated.block_damage";
            };

            return Text.translatable(translationKey, actorText);
        });

        // whiskey_shield_activated 威士忌护盾生效格式化器
        ReplayRegistry.registerFormatter("whiskey_shield_activated", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            String action = data.getString("action");

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            String translationKey = switch (action) {
                case "block_swallow" -> "replay.whiskey_shield_activated.block_swallow";
                case "block_damage" -> "replay.whiskey_shield_activated.block_damage";
                default -> "replay.whiskey_shield_activated.block_damage";
            };

            return Text.translatable(translationKey, actorText);
        });

        // voodoo_chain_death 格式化器
        ReplayRegistry.registerFormatter("voodoo_chain_death", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;

            if (actorUuid == null || targetUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);

            return Text.translatable("replay.voodoo_chain_death", actorText, targetText);
        });

        // death_in_stomach 格式化器
        ReplayRegistry.registerFormatter("death_in_stomach", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            return Text.translatable("replay.death_in_stomach", actorText);
        });

        // 交换者技能格式化器（显示交换的两个玩家）
        ReplayRegistry.registerSkillFormatter(SWAPPER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID target1Uuid = data.containsUuid("target1") ? data.getUuid("target1") : null;
            UUID target2Uuid = data.containsUuid("target2") ? data.getUuid("target2") : null;

            if (actorUuid == null || target1Uuid == null || target2Uuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text target1Text = ReplayGenerator.formatPlayerName(target1Uuid, playerInfoCache);
            Text target2Text = ReplayGenerator.formatPlayerName(target2Uuid, playerInfoCache);

            return Text.translatable("replay.skill.noellesroles.swapper.target", actorText, target1Text, target2Text);
        });

        // 乘务员登记格式化器（见面登记 / 确认死亡 / 无法辨认的尸体）
        ReplayRegistry.registerSkillFormatter(ATTENDANT_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            return switch (action) {
                case "met" -> {
                    UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
                    if (targetUuid == null) yield null;
                    Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                    if (data.containsUuid("morphling")) {
                        yield Text.translatable("replay.skill.noellesroles.attendant.met.disguise",
                                actorText,
                                ReplayGenerator.formatPlayerName(data.getUuid("morphling"), playerInfoCache),
                                targetText);
                    }
                    yield Text.translatable("replay.skill.noellesroles.attendant.met", actorText, targetText);
                }
                case "dead" -> {
                    UUID victimUuid = data.containsUuid("victim") ? data.getUuid("victim") : null;
                    if (victimUuid == null) yield null;
                    Text victimText = ReplayGenerator.formatPlayerName(victimUuid, playerInfoCache);
                    if (data.containsUuid("morphling")) {
                        yield Text.translatable("replay.skill.noellesroles.attendant.dead.disguise",
                                actorText,
                                ReplayGenerator.formatPlayerName(data.getUuid("morphling"), playerInfoCache),
                                victimText);
                    }
                    if (data.containsUuid("forged_as")) {
                        yield Text.translatable("replay.skill.noellesroles.attendant.dead.forged",
                                actorText,
                                victimText,
                                ReplayGenerator.formatPlayerName(data.getUuid("forged_as"), playerInfoCache));
                    }
                    yield Text.translatable("replay.skill.noellesroles.attendant.dead", actorText, victimText);
                }
                case "unrecognizable" -> Text.translatable("replay.skill.noellesroles.attendant.unrecognizable", actorText);
                default -> null;
            };
        });

        // 回溯者技能格式化器（双阶段：标记位置 / 传送回溯）
        ReplayRegistry.registerSkillFormatter(RECALLER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            int x = (int) Math.floor(data.getDouble("x"));
            int y = (int) Math.floor(data.getDouble("y"));
            int z = (int) Math.floor(data.getDouble("z"));
            String coordStr = String.format("(%d, %d, %d)", x, y, z);

            if ("teleport".equals(action)) {
                return Text.translatable("replay.skill.noellesroles.recaller.teleport", actorText, coordStr);
            } else {
                return Text.translatable("replay.skill.noellesroles.recaller.place", actorText, coordStr);
            }
        });

        // silencer skill 格式化器（通过 skillUse 系统自动处理，这里只需要注册翻译键）
        // 翻译键 replay.skill.noellesroles.silencer.target 在语言文件中定义

        // ===== 物品使用格式化器 =====

        ReplayRegistry.registerSkillFormatter(BREACHER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            String coordStr = String.format("(%d, %d, %d)", data.getInt("x"), data.getInt("y"), data.getInt("z"));

            if ("maintain".equals(action)) {
                UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
                if (targetUuid == null) return null;

                Text maintainerText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                int maintainerCount = Math.max(1, data.getInt("maintainerCount"));
                if (maintainerCount > 1) {
                    return Text.translatable("replay.skill.noellesroles.breacher.maintain.multiple", maintainerText, maintainerCount - 1, actorText, coordStr);
                }
                return Text.translatable("replay.skill.noellesroles.breacher.maintain", maintainerText, actorText, coordStr);
            }
            if ("reward".equals(action)) {
                int reward = data.contains("reward") ? data.getInt("reward") : 100;
                return Text.translatable("replay.skill.noellesroles.breacher.reward", actorText, coordStr, reward);
            }
            return Text.translatable("replay.skill.noellesroles.breacher.place", actorText, coordStr);
        });

        // 噬梦者技能格式化器（标记玩家）
        ReplayRegistry.registerSkillFormatter(DREAM_EATER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            return Text.translatable("replay.skill.noellesroles.dream_eater.mark",
                    ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache),
                    ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache));
        });

        // 傀儡师技能格式化器（傀儡术 / 操纵 / 切回 / 回收 / 傀儡被击倒 / 连带死亡）
        ReplayRegistry.registerSkillFormatter(PUPPETEER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorName = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("chain_death".equals(action)) {
                return Text.translatable("replay.skill.noellesroles.puppeteer.chain_death", actorName);
            }
            UUID puppetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (puppetUuid == null) return null;
            Text puppetName = ReplayGenerator.formatPlayerName(puppetUuid, playerInfoCache);
            return switch (action) {
                case "convert", "control", "release", "recall" ->
                        Text.translatable("replay.skill.noellesroles.puppeteer." + action, actorName, puppetName);
                case "destroyed" -> data.containsUuid("attacker")
                        ? Text.translatable("replay.skill.noellesroles.puppeteer.destroyed", actorName, puppetName,
                                ReplayGenerator.formatPlayerName(data.getUuid("attacker"), playerInfoCache))
                        : Text.translatable("replay.skill.noellesroles.puppeteer.destroyed_unknown", actorName, puppetName);
                default -> null;
            };
        });

        // 谎言音效师技能格式化器（对某玩家周围施加幻听）
        ReplayRegistry.registerSkillFormatter(PHONISM_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            PhonismSound sound = PhonismSound.byKey(data.getString("sound"));
            if (actorUuid == null || targetUuid == null || sound == null) return null;

            // 音效名用角色色高亮：回放界面按文本样式取色，写在文案里的 § 颜色码不会生效
            Text soundText = Text.literal("【")
                    .append(Text.translatable("screen.phonism.sound." + sound.key()))
                    .append(Text.literal("】"))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Noellesroles.PHONISM.color())));

            return Text.translatable("replay.skill.noellesroles.phonism.hallucinate",
                    ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache),
                    ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache),
                    soundText);
        });

        // 拾荒者技能格式化器（搜刮尸体：从谁身上搜到了什么）
        ReplayRegistry.registerSkillFormatter(JUNKER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID victimUuid = data.containsUuid("victim") ? data.getUuid("victim")
                    : (data.containsUuid("target") ? data.getUuid("target") : null);
            if (actorUuid == null || victimUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text victimText = ReplayGenerator.formatPlayerName(victimUuid, playerInfoCache);
            return Text.translatable("replay.skill.noellesroles.junker.search", actorText, victimText,
                    ReplayGenerator.formatItemName(data, world));
        });

        // 拾荒者搜刮所得物品销毁（主动销毁 / 使用后销毁）
        ReplayRegistry.registerFormatter("junker_loot_destroyed", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text itemText = ReplayGenerator.formatItemName(data, world);
            String translationKey = "manual".equals(data.getString("action"))
                    ? "replay.junker_loot_destroyed.manual"
                    : "replay.junker_loot_destroyed.used";
            return Text.translatable(translationKey, actorText, itemText);
        });

        Identifier fineDrinkId = Registries.ITEM.getId(ModItems.FINE_DRINK);
        Identifier hallucinationMedicineId = Registries.ITEM.getId(ModItems.HALLUCINATION_MEDICINE);
        Identifier timedBombId = Registries.ITEM.getId(ModItems.TIMED_BOMB);
        Identifier antidoteId = Registries.ITEM.getId(ModItems.ANTIDOTE);
        Identifier ironManVialId = Registries.ITEM.getId(ModItems.IRON_MAN_VIAL);
        Identifier poisonNeedleId = Registries.ITEM.getId(ModItems.POISON_NEEDLE);
        Identifier poisonGasBombId = Registries.ITEM.getId(ModItems.POISON_GAS_BOMB);
        Identifier portableRadarId = Registries.ITEM.getId(ModItems.PORTABLE_RADAR);
        Identifier adrenalineId = Registries.ITEM.getId(ModItems.ADRENALINE);
        Identifier medicalSplintId = Registries.ITEM.getId(ModItems.MEDICAL_SPLINT);
        Identifier assassinBookId = Registries.ITEM.getId(ModItems.ASSASSIN_BOOK);
        Identifier forgedPotionId = Registries.ITEM.getId(ModItems.FORGED_POTION);

        // 赝造药水 - 伪装尸体（回放显示真实死者与伪装对象）
        ReplayRegistry.registerItemUseFormatter(forgedPotionId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID victimUuid = data.containsUuid("victim") ? data.getUuid("victim") : null;
            UUID forgedUuid = data.containsUuid("forged_as") ? data.getUuid("forged_as") : null;
            if (actorUuid == null || victimUuid == null || forgedUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text victimText = ReplayGenerator.formatPlayerName(victimUuid, playerInfoCache);
            Text forgedText = ReplayGenerator.formatPlayerName(forgedUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.forged_potion", actorText, victimText, forgedText);
        });

        // 上等佳酿 - 喝下 / 放置到餐盘
        ReplayRegistry.registerItemUseFormatter(fineDrinkId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("place".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.fine_drink.place", actorText);
            }
            return Text.translatable("replay.item_use.noellesroles.fine_drink.drink", actorText);
        });

        ReplayRegistry.registerItemUseFormatter(hallucinationMedicineId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("place".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.hallucination_medicine.place", actorText);
            }
            return Text.translatable("replay.item_use.noellesroles.hallucination_medicine.drink", actorText);
        });

        // 定时炸弹 - 传递
        ReplayRegistry.registerItemUseFormatter(timedBombId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.timed_bomb.transfer", actorText, targetText);
        });

        // 解毒剂 - 解毒 / 清除餐盘毒素
        ReplayRegistry.registerItemUseFormatter(antidoteId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("cure_plate".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.antidote.cure_plate", actorText);
            }
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (targetUuid == null) return null;
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.antidote", actorText, targetText);
        });

        // 铁人药剂 - 注射
        ReplayRegistry.registerItemUseFormatter(ironManVialId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.iron_man_vial", actorText, targetText);
        });

        // 毒针 - 刺
        ReplayRegistry.registerItemUseFormatter(poisonNeedleId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.poison_needle", actorText, targetText);
        });

        // 毒气弹 - 投掷
        ReplayRegistry.registerItemUseFormatter(poisonGasBombId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.poison_gas_bomb", actorText);
        });

        // 便携式雷达 - 启动
        ReplayRegistry.registerItemUseFormatter(portableRadarId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.portable_radar", actorText);
        });

        // 肾上腺素 - 注射
        ReplayRegistry.registerItemUseFormatter(adrenalineId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.adrenaline", actorText);
        });

        // 刺客之书 - 猜测身份
        ReplayRegistry.registerItemUseFormatter(assassinBookId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.assassin_book", actorText, targetText);
        });

        // 医用夹板 - 使用
        ReplayRegistry.registerItemUseFormatter(medicalSplintId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.medical_splint", actorText);
        });

        // ===== 餐盘拿取格式化器 =====

        // 上等佳酿 - 从餐盘拿取
        ReplayRegistry.registerPlatterTakeFormatter(fineDrinkId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.platter_take.noellesroles.fine_drink", actorText);
        });

        ReplayRegistry.registerPlatterTakeFormatter(hallucinationMedicineId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.platter_take.noellesroles.hallucination_medicine", actorText);
        });

        // 催化剂
        Identifier catalystId = Registries.ITEM.getId(ModItems.CATALYST);
        ReplayRegistry.registerItemUseFormatter(catalystId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (targetUuid != null) {
                Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                return Text.translatable("replay.item_use.noellesroles.catalyst", actorText, targetText);
            }
            return Text.translatable("replay.item_use.noellesroles.catalyst.no_target", actorText);
        });

        // 投掷斧 - 使用
        Identifier throwingAxeId = Registries.ITEM.getId(ModItems.THROWING_AXE);
        ReplayRegistry.registerItemUseFormatter(throwingAxeId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.throwing_axe", actorText);
        });

        // 强盗技能格式化器（齐掷飞斧）
        ReplayRegistry.registerSkillFormatter(BANDIT_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            if ("volley".equals(data.getString("action"))) {
                return Text.translatable("replay.skill.noellesroles.bandit.volley", actorText, data.getInt("count"));
            }
            return null;
        });

        Identifier riotForkId = Registries.ITEM.getId(ModItems.RIOT_FORK);
        ReplayRegistry.registerItemUseFormatter(riotForkId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.riot_fork", actorText, targetText);
        });

        Identifier hunterTrapId = Registries.ITEM.getId(ModItems.HUNTER_TRAP);
        ReplayRegistry.registerItemUseFormatter(hunterTrapId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("dismantle".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.hunter_trap.dismantle", actorText);
            }
            if ("pickup".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.hunter_trap.pickup", actorText);
            }
            if ("poison".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.hunter_trap.poison", actorText);
            }
            return Text.translatable("replay.item_use.noellesroles.hunter_trap.place", actorText);
        });

        Identifier doubleBarrelShotgunId = Registries.ITEM.getId(ModItems.DOUBLE_BARREL_SHOTGUN);
        ReplayRegistry.registerItemUseFormatter(doubleBarrelShotgunId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("reload".equals(action)) {
                int loadedShells = data.getInt("loaded_shells");
                return Text.translatable("replay.item_use.noellesroles.double_barrel_shotgun.reload", actorText, loadedShells);
            }

            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (targetUuid == null) {
                // 未命中改由 gun_miss 事件展示
                return null;
            }
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.double_barrel_shotgun.fire_hit", actorText, targetText);
        });

        ReplayRegistry.registerFormatter(org.agmas.noellesroles.entity.HunterTrapEntity.EVENT_TRIGGERED, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (targetUuid == null) return null;

            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            boolean poisoned = "true".equals(data.getString("poisoned"));
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) {
                return Text.translatable(
                    poisoned ? "replay.global.noellesroles.hunter_trap_triggered.poisoned_no_owner" : "replay.global.noellesroles.hunter_trap_triggered.no_owner",
                    targetText
                );
            }

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            if (poisoned && data.containsUuid("poisoner")) {
                Text poisonerText = ReplayGenerator.formatPlayerName(data.getUuid("poisoner"), playerInfoCache);
                return Text.translatable("replay.global.noellesroles.hunter_trap_triggered.poisoned", actorText, poisonerText, targetText);
            }
            return Text.translatable(
                poisoned ? "replay.global.noellesroles.hunter_trap_triggered.owner_poisoned" : "replay.global.noellesroles.hunter_trap_triggered",
                actorText,
                targetText
            );
        });

        // ===== 基酒/鸡尾酒格式化器 =====

        Identifier baseSpiritId = Registries.ITEM.getId(ModItems.BASE_SPIRIT);

        // 喝下 / 放置到餐盘（通过 recordItemUse）
        ReplayRegistry.registerItemUseFormatter(baseSpiritId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text drinkName = noellesroles$getDrinkName(data);
            String action = data.getString("action");
            if ("place".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.base_spirit.place", actorText, drinkName);
            }
            return Text.translatable("replay.item_use.noellesroles.base_spirit.drink", actorText, drinkName);
        });

        // 从餐盘拿取（通过 recordPlatterTake）
        ReplayRegistry.registerPlatterTakeFormatter(baseSpiritId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text drinkName = noellesroles$getDrinkName(data);
            return Text.translatable("replay.platter_take.noellesroles.base_spirit", actorText, drinkName);
        });

        // 调酒事件（prev/after 子 compound 中各含 ingredients NbtList，复用 getDrinkName 推导名称）
        ReplayRegistry.registerFormatter("ingredient_mixed", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String ingredient = data.getString("ingredient");
            Text ingredientName = Text.translatable("item.noellesroles." + ingredient);

            // 调制前饮品名（从 prev compound 的 ingredients NbtList 推导）
            NbtCompound prevData = data.contains("prev") ? data.getCompound("prev") : new NbtCompound();
            Text targetName = noellesroles$getDrinkName(prevData);

            // 调制后饮品名（从 after compound 的 ingredients NbtList 推导）
            NbtCompound afterData = data.contains("after") ? data.getCompound("after") : new NbtCompound();
            Text resultName = noellesroles$getDrinkName(afterData);

            // 调制后匹配到鸡尾酒名时显示 "调制出了xxx"
            if (afterData.contains("ingredients")) {
                java.util.List<String> afterList = new java.util.ArrayList<>();
                net.minecraft.nbt.NbtList afterNbt = afterData.getList("ingredients", net.minecraft.nbt.NbtString.STRING_TYPE);
                for (int i = 0; i < afterNbt.size(); i++) afterList.add(afterNbt.getString(i));
                if (CocktailRegistry.getCocktailKey(afterList) != null) {
                    return Text.translatable("replay.ingredient_mixed.result", actorText, ingredientName, targetName, resultName);
                }
            }
            return Text.translatable("replay.ingredient_mixed", actorText, ingredientName, targetName);
        });

        ReplayRegistry.registerSkillFormatter(CRIMINAL_REASONER_ID, (event, match, world) -> {
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            String action = data.getString("action");
            UUID victimUuid = data.containsUuid("victim_uuid") ? data.getUuid("victim_uuid") : null;
            UUID suspectUuid = data.containsUuid("suspect_uuid") ? data.getUuid("suspect_uuid") : null;
            boolean success = data.getBoolean("success");

            if (data.contains("extra")) {
                NbtCompound extra = data.getCompound("extra");
                if (action.isEmpty()) action = extra.getString("action");
                if (victimUuid == null && extra.containsUuid("victim_uuid")) victimUuid = extra.getUuid("victim_uuid");
                if (suspectUuid == null && extra.containsUuid("suspect_uuid")) suspectUuid = extra.getUuid("suspect_uuid");
                if (extra.contains("success")) success = extra.getBoolean("success");
            }

            if ("invisible".equals(action)) {
                if (actorUuid == null) return null;
                return Text.translatable(
                        "replay.skill.noellesroles.criminal_reasoner.invisible",
                        ReplayGenerator.formatPlayerName(actorUuid, ReplayGenerator.getPlayerInfoCache(match))
                );
            }

            if (!"reason".equals(action)) return null;
            if (actorUuid == null || victimUuid == null || suspectUuid == null) return null;

            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text victimText = ReplayGenerator.formatPlayerName(victimUuid, playerInfoCache);
            Text suspectText = ReplayGenerator.formatPlayerName(suspectUuid, playerInfoCache);

            return Text.translatable(
                    success
                            ? "replay.skill.noellesroles.criminal_reasoner.success"
                            : "replay.skill.noellesroles.criminal_reasoner.fail",
                    actorText,
                    victimText,
                    suspectText
            );
        });

        ReplayRegistry.registerSkillFormatter(ORTHOPEDIST_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            String action = data.getString("action");
            if ("heal_fracture".equals(action)) {
                return Text.translatable("replay.skill.noellesroles.orthopedist.heal_fracture", actorText, targetText);
            }
            if ("bone_setting".equals(action)) {
                return Text.translatable("replay.skill.noellesroles.orthopedist.bone_setting", actorText, targetText);
            }
            return null;
        });

        ReplayRegistry.registerSkillFormatter(FERRYMAN_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("reaction".equals(action)) {
                UUID attackerUuid = data.containsUuid("attacker") ? data.getUuid("attacker") : null;
                if (data.getBoolean("blessing") && attackerUuid != null) {
                    Text attackerText = ReplayGenerator.formatPlayerName(attackerUuid, playerInfoCache);
                    return Text.translatable("replay.skill.noellesroles.ferryman.reaction_blessed", actorText, attackerText);
                }
                return Text.translatable("replay.skill.noellesroles.ferryman.reaction", actorText);
            }

            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if ("ferry".equals(action) && targetUuid != null) {
                Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                return Text.translatable("replay.skill.noellesroles.ferryman.target", actorText, targetText);
            }
            return null;
        });

        ReplayRegistry.registerSkillFormatter(COMMANDER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.skill.noellesroles.commander.target", actorText, targetText);
        });

        ReplayRegistry.registerFormatter("saint_karma", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            String action = data.getString("action");
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;

            if ("marked".equals(action)) {
                if (actorUuid == null || targetUuid == null) return null;
                Text saintText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
                Text killerText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                return Text.translatable("replay.global.noellesroles.saint_karma.marked", saintText, killerText);
            }

            if ("triggered".equals(action) && actorUuid != null) {
                Text killerText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
                int duration = data.getInt("duration");
                return Text.translatable("replay.global.noellesroles.saint_karma.triggered", killerText, Math.max(1, duration / 20));
            }

            return null;
        });

    }

    /**
     * 从事件数据的 ingredients NbtList 中获取饮品显示名称。
     * 如果匹配到鸡尾酒则返回带颜色的鸡尾酒名，否则返回"基酒"。
     */
    private static Text noellesroles$getDrinkName(NbtCompound data) {
        if (!data.contains("ingredients")) {
            return Text.translatable("item.noellesroles.base_spirit");
        }
        net.minecraft.nbt.NbtList list = data.getList("ingredients", net.minecraft.nbt.NbtString.STRING_TYPE);
        if (list == null || list.isEmpty()) {
            return Text.translatable("item.noellesroles.base_spirit");
        }
        java.util.List<String> ingredients = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ingredients.add(list.getString(i));
        }
        net.minecraft.text.MutableText cocktailName = CocktailRegistry.getCocktailName(ingredients);
        if (cocktailName != null) return cocktailName;
        return Text.translatable("item.noellesroles.base_spirit");
    }

    private static void removeHunterShotgunDrops(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.DOUBLE_BARREL_SHOTGUN) || stack.isOf(ModItems.DOUBLE_BARREL_SHELL)) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static void removeLooseEndDrops(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.MASTER_KEY) || stack.isOf(WatheItems.REVOLVER)) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

}

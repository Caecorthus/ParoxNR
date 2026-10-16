package org.agmas.noellesroles.client.configscreen;

import dev.doctor4t.wathe.WatheConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.screen.NoellesRolesConfigScreen;

import java.util.List;

public final class NoellesRolesConfigScreenFactory {
    private NoellesRolesConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        return new NoellesRolesConfigScreen(parent, buildCategories());
    }

    public static List<ConfigCategoryDefinition> buildCategories() {
        return List.of(
                new ConfigCategoryDefinition(
                        "client",
                        Text.translatable("config_screen.category.client"),
                        Text.translatable("config_screen.category.client.description"),
                        List.of(
                                ConfigOptionDefinition.enumeration(
                                        "wathe_instinct_mode",
                                        Text.translatable("config_screen.option.wathe_instinct_mode"),
                                        Text.translatable("config_screen.option.wathe_instinct_mode.description"),
                                        List.of(WatheConfig.InstinctModeConfig.values()),
                                        NoellesRolesConfigScreenFactory::getInstinctModeText,
                                        ConfigScreenState::instinctMode,
                                        ConfigScreenState::instinctMode,
                                        (state, value) -> state.instinctMode(value)
                                ),
                                ConfigOptionDefinition.number(
                                        "wathe_chat_history_limit",
                                        Text.translatable("config_screen.option.wathe_chat_history_limit"),
                                        Text.translatable("config_screen.option.wathe_chat_history_limit.description"),
                                        WatheConfig.MIN_CHAT_HISTORY_LIMIT,
                                        WatheConfig.MAX_CHAT_HISTORY_LIMIT,
                                        ConfigScreenState::chatHistoryLimit,
                                        ConfigScreenState::chatHistoryLimit,
                                        (state, value) -> state.chatHistoryLimit(value)
                                ),
                                ConfigOptionDefinition.toggle(
                                        "collapse_top_right_hints",
                                        Text.translatable("config_screen.option.collapse_top_right_hints"),
                                        Text.translatable("config_screen.option.collapse_top_right_hints.description"),
                                        state -> state.noellesRolesConfig().collapseTopRightHints,
                                        state -> state.noellesRolesConfig().collapseTopRightHints,
                                        (state, value) -> state.noellesRolesConfig().collapseTopRightHints = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "bottom_right_hint_outline",
                                        Text.translatable("config_screen.option.bottom_right_hint_outline"),
                                        Text.translatable("config_screen.option.bottom_right_hint_outline.description"),
                                        state -> state.noellesRolesConfig().bottomRightHintOutline,
                                        state -> state.noellesRolesConfig().bottomRightHintOutline,
                                        (state, value) -> state.noellesRolesConfig().bottomRightHintOutline = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "legacy_gui_style",
                                        Text.translatable("config_screen.option.legacy_gui_style"),
                                        Text.translatable("config_screen.option.legacy_gui_style.description"),
                                        state -> state.noellesRolesConfig().legacyGuiStyle,
                                        state -> state.noellesRolesConfig().legacyGuiStyle,
                                        (state, value) -> state.noellesRolesConfig().legacyGuiStyle = value
                                )
                        )
                ),
                new ConfigCategoryDefinition(
                        "gameplay",
                        Text.translatable("config_screen.category.gameplay"),
                        Text.translatable("config_screen.category.gameplay.description"),
                        List.of(
                                ConfigOptionDefinition.toggle(
                                        "insane_players_see_morphs",
                                        Text.translatable("config_screen.option.insane_players_see_morphs"),
                                        Text.translatable("config_screen.option.insane_players_see_morphs.description"),
                                        state -> state.noellesRolesConfig().insanePlayersSeeMorphs,
                                        state -> state.noellesRolesConfig().insanePlayersSeeMorphs,
                                        (state, value) -> state.noellesRolesConfig().insanePlayersSeeMorphs = value
                                ),
                                ConfigOptionDefinition.number(
                                        "general_cooldown_ticks",
                                        Text.translatable("config_screen.option.general_cooldown_ticks"),
                                        Text.translatable("config_screen.option.general_cooldown_ticks.description"),
                                        0,
                                        20 * 60 * 10,
                                        state -> state.noellesRolesConfig().generalCooldownTicks,
                                        state -> state.noellesRolesConfig().generalCooldownTicks,
                                        (state, value) -> state.noellesRolesConfig().generalCooldownTicks = value
                                ),
                                ConfigOptionDefinition.numberInput(
                                        "match_length_seconds",
                                        Text.translatable("config_screen.option.match_length_seconds"),
                                        Text.translatable("config_screen.option.match_length_seconds.description"),
                                        6,
                                        state -> state.noellesRolesConfig().matchLengthSeconds,
                                        state -> state.noellesRolesConfig().matchLengthSeconds,
                                        (state, value) -> state.noellesRolesConfig().matchLengthSeconds = value
                                ),
                                ConfigOptionDefinition.number(
                                        "white_knight_chance_percent",
                                        Text.translatable("config_screen.option.white_knight_chance_percent"),
                                        Text.translatable("config_screen.option.white_knight_chance_percent.description"),
                                        0,
                                        100,
                                        state -> state.noellesRolesConfig().whiteKnightChancePercent,
                                        state -> state.noellesRolesConfig().whiteKnightChancePercent,
                                        (state, value) -> state.noellesRolesConfig().whiteKnightChancePercent = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "mis_kill_gun_cooldown",
                                        Text.translatable("config_screen.option.mis_kill_gun_cooldown"),
                                        Text.translatable("config_screen.option.mis_kill_gun_cooldown.description"),
                                        state -> state.noellesRolesConfig().misKillGunCooldown,
                                        state -> state.noellesRolesConfig().misKillGunCooldown,
                                        (state, value) -> state.noellesRolesConfig().misKillGunCooldown = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "voodoo_non_killer_deaths",
                                        Text.translatable("config_screen.option.voodoo_non_killer_deaths"),
                                        Text.translatable("config_screen.option.voodoo_non_killer_deaths.description"),
                                        state -> state.noellesRolesConfig().voodooNonKillerDeaths,
                                        state -> state.noellesRolesConfig().voodooNonKillerDeaths,
                                        (state, value) -> state.noellesRolesConfig().voodooNonKillerDeaths = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "show_fog_radius_hud",
                                        Text.translatable("config_screen.option.show_fog_radius_hud"),
                                        Text.translatable("config_screen.option.show_fog_radius_hud.description"),
                                        state -> state.noellesRolesConfig().showFogRadiusHud,
                                        state -> state.noellesRolesConfig().showFogRadiusHud,
                                        (state, value) -> state.noellesRolesConfig().showFogRadiusHud = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "show_hallucination_hud",
                                        Text.translatable("config_screen.option.show_hallucination_hud"),
                                        Text.translatable("config_screen.option.show_hallucination_hud.description"),
                                        state -> state.noellesRolesConfig().showHallucinationHud,
                                        state -> state.noellesRolesConfig().showHallucinationHud,
                                        (state, value) -> state.noellesRolesConfig().showHallucinationHud = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "skip_mode_voting",
                                        Text.translatable("config_screen.option.skip_mode_voting"),
                                        Text.translatable("config_screen.option.skip_mode_voting.description"),
                                        state -> state.noellesRolesConfig().skipModeVoting,
                                        state -> state.noellesRolesConfig().skipModeVoting,
                                        (state, value) -> state.noellesRolesConfig().skipModeVoting = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "skip_map_voting",
                                        Text.translatable("config_screen.option.skip_map_voting"),
                                        Text.translatable("config_screen.option.skip_map_voting.description"),
                                        state -> state.noellesRolesConfig().skipMapVoting,
                                        state -> state.noellesRolesConfig().skipMapVoting,
                                        (state, value) -> state.noellesRolesConfig().skipMapVoting = value
                                )
                        )
                ),
                new ConfigCategoryDefinition(
                        "compatibility",
                        Text.translatable("config_screen.category.compatibility"),
                        Text.translatable("config_screen.category.compatibility.description"),
                        List.of(
                                ConfigOptionDefinition.toggle(
                                        "lock_sound_physics_remastered_config",
                                        Text.translatable("config_screen.option.lock_sound_physics_remastered_config"),
                                        Text.translatable("config_screen.option.lock_sound_physics_remastered_config.description"),
                                        state -> state.noellesRolesConfig().lockSoundPhysicsRemasteredConfig,
                                        state -> state.noellesRolesConfig().lockSoundPhysicsRemasteredConfig,
                                        (state, value) -> state.noellesRolesConfig().lockSoundPhysicsRemasteredConfig = value
                                ),
                                ConfigOptionDefinition.toggle(
                                        "lock_talk_bubbles_config",
                                        Text.translatable("config_screen.option.lock_talk_bubbles_config"),
                                        Text.translatable("config_screen.option.lock_talk_bubbles_config.description"),
                                        state -> state.noellesRolesConfig().lockTalkBubblesConfig,
                                        state -> state.noellesRolesConfig().lockTalkBubblesConfig,
                                        (state, value) -> state.noellesRolesConfig().lockTalkBubblesConfig = value
                                )
                        )
                )
        );
    }

    private static Text getInstinctModeText(WatheConfig.InstinctModeConfig instinctMode) {
        return switch (instinctMode) {
            case HOLD -> Text.translatable("config_screen.enum.wathe_instinct_mode.hold");
            case TOGGLE -> Text.translatable("config_screen.enum.wathe_instinct_mode.toggle");
        };
    }
}

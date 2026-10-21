package org.agmas.noellesroles.client.roleinfo;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.agmas.noellesroles.client.NoellesrolesClient;

import java.util.*;

/**
 * Registry for role information.
 * Uses hardcoded in-mod registrations only.
 */
public class RoleInfoRegistry {
    public static final String CATEGORY_CLASSIC = "category:classic_murder";
    public static final String CATEGORY_MURDER_MAYHEM = "category:murder_mayhem";
    public static final String CATEGORY_LOOSE_ENDS = "category:loose_ends";
    public static final String CATEGORY_DEATH_ARENA = "category:death_arena";
    public static final String CATEGORY_CLASSIC_OVERVIEW = "category:classic_murder_overview";
    public static final String EVENT_MURDER_MAYHEM_FOG_OF_WAR = "event:murder_mayhem_fog_of_war";
    public static final String CATEGORY_LOOSE_ENDS_OVERVIEW = "category:loose_ends_overview";
    public static final String CATEGORY_DEATH_ARENA_OVERVIEW = "category:death_arena_overview";
    public static final String CATEGORY_PASSENGER = "category:passenger";
    public static final String CATEGORY_KILLER = "category:killer";
    public static final String CATEGORY_NEUTRAL = "category:neutral";
    public static final String ROLE_LOOSE_END = "wathe:loose_end";

    private static Map<String, RoleInfoData> roleInfoMap = new LinkedHashMap<>();
    private static Map<String, RoleInfoData> categoryInfoMap = new LinkedHashMap<>();

    /**
     * Load hardcoded role info. Call once during client initialization.
     */
    public static void load() {
        roleInfoMap = createDefaults();
        categoryInfoMap = createCategoryPages();
    }

    /**
     * Get role info by full identifier string (e.g. "noellesroles:morphling").
     */
    public static RoleInfoData get(String roleIdentifier) {
        return roleInfoMap.get(roleIdentifier);
    }

    public static RoleInfoData getPage(String pageIdentifier) {
        RoleInfoData categoryPage = categoryInfoMap.get(pageIdentifier);
        return categoryPage != null ? categoryPage : roleInfoMap.get(pageIdentifier);
    }

    public static Map<String, RoleInfoData> getAll() {
        return Collections.unmodifiableMap(roleInfoMap);
    }

    public static String getFactionCategoryId(RoleInfoData roleInfoData) {
        if (roleInfoData == null || roleInfoData.factionKey == null) {
            return CATEGORY_PASSENGER;
        }
        if (roleInfoData.factionKey.endsWith(".killer")) {
            return CATEGORY_KILLER;
        }
        if (roleInfoData.factionKey.endsWith(".neutral")) {
            return CATEGORY_NEUTRAL;
        }
        return CATEGORY_PASSENGER;
    }

    public static String getModeCategoryId(Identifier gameModeId) {
        // 迷乱谋杀模式已废弃：其分类节点不再出现在说明界面里，所以这里把它归到经典谋杀分类，
        // 避免任何路径（例如 skipModeVoting 沿用上一局模式）把界面导航到一个永远显示不出来的隐藏页。
        if (WatheGameModes.LOOSE_ENDS_ID.equals(gameModeId)) {
            return CATEGORY_LOOSE_ENDS;
        }
        return CATEGORY_CLASSIC;
    }

    public static String getModeOverviewPageId(Identifier gameModeId) {
        if (WatheGameModes.LOOSE_ENDS_ID.equals(gameModeId)) {
            return CATEGORY_LOOSE_ENDS_OVERVIEW;
        }
        return CATEGORY_CLASSIC_OVERVIEW;
    }

    /**
     * Resolve a keybind ID to the localized key name string.
     */
    public static String resolveKeybind(String keybindId) {
        if (keybindId == null || keybindId.isEmpty()) return "";
        MinecraftClient mc = MinecraftClient.getInstance();
        return switch (keybindId) {
            case "ability" -> NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            case "ability2" -> NoellesrolesClient.ability2Bind.getBoundKeyLocalizedText().getString();
            case "assist_interface" -> NoellesrolesClient.assistInterfaceBind.getBoundKeyLocalizedText().getString();
            case "role_info" -> NoellesrolesClient.roleInfoBind.getBoundKeyLocalizedText().getString();
            case "inventory" -> mc.options.inventoryKey.getBoundKeyLocalizedText().getString();
            case "use" -> mc.options.useKey.getBoundKeyLocalizedText().getString();
            case "attack" -> mc.options.attackKey.getBoundKeyLocalizedText().getString();
            case "sprint" -> mc.options.sprintKey.getBoundKeyLocalizedText().getString();
            case "instinct" -> WatheClient.instinctKeybind.getBoundKeyLocalizedText().getString();
            default -> keybindId;
        };
    }

    /**
     * Resolve a config string to Text.
     * Supports:
     * - "tr:<translation.key>" => translatable
     * - "translation.key"       => translatable if exists, otherwise humanized label
     * - plain text              => literal
     */
    public static Text resolveText(String raw) {
        if (raw == null || raw.isEmpty()) return Text.empty();

        if (raw.startsWith("tr:")) {
            String key = raw.substring(3);
            if (Language.getInstance().hasTranslation(key)) {
                return Text.translatable(key);
            }
            return Text.literal(humanizeKey(key));
        }

        if (raw.contains(".")) {
            if (Language.getInstance().hasTranslation(raw)) {
                return Text.translatable(raw);
            }
            return Text.literal(humanizeKey(raw));
        }

        return Text.literal(raw);
    }

    private static String humanizeKey(String key) {
        int i = key.lastIndexOf('.');
        String tail = i >= 0 ? key.substring(i + 1) : key;
        return tail.replace('_', ' ');
    }

    /**
     * Get the resolved trigger text for a skill.
     * If triggerKeybind is set, the key name is resolved and inserted when possible.
     */
    public static Text getTriggerText(RoleInfoData.SkillInfoData skill) {
        if (skill == null || skill.triggerKey == null) return Text.empty();

        String keyName = resolveKeybind(skill.triggerKeybind);
        boolean hasKeybind = skill.triggerKeybind != null && !skill.triggerKeybind.isEmpty();

        String key = skill.triggerKey.startsWith("tr:") ? skill.triggerKey.substring(3) : skill.triggerKey;
        if (skill.triggerKey.startsWith("tr:") || skill.triggerKey.contains(".")) {
            if (Language.getInstance().hasTranslation(key)) {
                return hasKeybind ? Text.translatable(key, keyName) : Text.translatable(key);
            }
            if (hasKeybind) {
                return Text.literal(humanizeKey(key) + " [" + keyName + "]");
            }
            return Text.literal(humanizeKey(key));
        }

        if (hasKeybind && skill.triggerKey.contains("%s")) {
            return Text.literal(skill.triggerKey.formatted(keyName));
        }

        return Text.literal(skill.triggerKey);
    }

    // ==================== Default Hardcoded Registry ====================

    private static RoleInfoData r(String id) {
        RoleInfoData info = new RoleInfoData(
                id,
                "tr:announcement.role." + id,
                "tr:" + factionLabelKeyForRole(id),
                "tr:announcement.goals." + id,
                "tr:" + winConditionKeyForRole(id)
        );
        info.summaryKey = "tr:roleinfo.summary." + id;
        return info;
    }

    private static String factionLabelKeyForRole(String roleId) {
        if ("loose_end".equals(roleId)) {
            return "faction.wathe.none";
        }
        return switch (inferFaction(roleId)) {
            case "killer" -> "shared.faction.killer";
            case "neutral" -> "shared.faction.neutral";
            default -> "shared.faction.passenger";
        };
    }

    private static String winConditionKeyForRole(String roleId) {
        if ("loose_end".equals(roleId)) {
            return "roleinfo.win_condition.loose_end";
        }
        String faction = inferFaction(roleId);
        if ("neutral".equals(faction)) {
            return neutralWinConditionKey(roleId);
        }
        return "roleinfo.win_condition.default." + faction;
    }

    private static String neutralWinConditionKey(String roleId) {
        return switch (roleId) {
            case "vulture" -> "roleinfo.win_condition.vulture";
            case "jester" -> "roleinfo.win_condition.jester";
            case "pathogen" -> "roleinfo.win_condition.pathogen";
            case "corrupt_cop" -> "roleinfo.win_condition.corrupt_cop";
            case "gangster_star" -> "roleinfo.win_condition.gangster_star";
            case "taotie" -> "roleinfo.win_condition.taotie";
            case "criminal_reasoner" -> "roleinfo.win_condition.criminal_reasoner";
            case "ferryman" -> "roleinfo.win_condition.ferryman";
            default -> "roleinfo.win_condition.default.neutral";
        };
    }

    private static String inferFaction(String roleId) {
        Set<String> killer = Set.of(
                "killer",
                "morphling", "phantom", "swapper", "the_insane_damned_paranoid_killer", "bomber", "assassin",
                "scavenger", "serial_killer", "silencer", "poisoner", "bandit", "hunter", "commander",
                "breacher", "phonism", "dream_eater", "puppeteer"
        );
        Set<String> neutral = Set.of("jester", "vulture", "corrupt_cop", "gangster_star", "pathogen", "taotie", "criminal_reasoner", "ferryman");
        if (killer.contains(roleId)) return "killer";
        if (neutral.contains(roleId)) return "neutral";
        return "innocent";
    }

    private static Map<String, RoleInfoData> createDefaults() {
        Map<String, RoleInfoData> m = new LinkedHashMap<>();

        // ===================== Wathe 原版职业 =====================

        // 杀手
        m.put("wathe:killer", r("killer")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能透视
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open")); // 商店

        // 平民
        m.put("wathe:civilian", r("civilian")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger") // 乘客基础能力
                .addPassiveSkill("survive")); // 存活目标

        // 义警
        m.put("wathe:vigilante", r("vigilante")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger") // 乘客基础能力
                .addSkillWithSharedNameAndTrigger("gun", null, "shared.name.revolver", "shared.trigger.initial_item") // 持有手枪
                .addPassiveSkill("trap_vision")); // 可见猎人捕兽夹

        // 亡命徒
        RoleInfoData looseEnd = new RoleInfoData(
                "loose_end",
                "tr:announcement.role.loose_end",
                null,
                "tr:announcement.loose_ends.goal",
                "tr:roleinfo.win_condition.loose_end"
        );
        looseEnd.summaryKey = "tr:roleinfo.summary.loose_end";
        m.put(ROLE_LOOSE_END, looseEnd);

        // ===================== 杀手阵营 =====================

        // 变形者
        m.put("noellesroles:morphling", r("morphling")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open") // 商店
                .addSkillWithSharedTrigger("morph", "ability2", "shared.trigger.active_menu") // 变形成目标
                .addActiveUseSkill("corpse_mode", "ability") // 尸体伪装
                .addShopSkill("myriad_changes") // 万千变化（商店购买）
                .addShopSkill("forged_potion")); // 赝造药水（商店购买）

        // 幽灵
        m.put("noellesroles:phantom", r("phantom")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open") // 商店
                .addActiveUseSkill("invisibility", "ability")); // 隐身

        // 交换者
        m.put("noellesroles:swapper", r("swapper")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open") // 商店
                .addSkillWithSharedTrigger("swap", "ability", "shared.trigger.active_menu")); // 交换两名玩家位置

        // 亡语杀手
        m.put("noellesroles:the_insane_damned_paranoid_killer", r("the_insane_damned_paranoid_killer")
                .addSharedNamedHoldUseSkill("instinct", "instinct", "shared.name.killer_instinct") // 本能
                .addSkillWithSharedNameAndEffect("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop") // 商店
                .addPassiveSkill("insanity")); // 听见死者哀嚎

        // 炸弹客
        m.put("noellesroles:bomber", r("bomber")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addInventoryOpenSkill("shop") // 商店
                .addItemEffectSkill("plant_bomb", "use") // 安装定时炸弹
                .addPassiveSkill("bomb_vision")); // 透视炸弹携带者

        // 刺客
        m.put("noellesroles:assassin", r("assassin")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open") // 商店
                .addActiveUseSkill("guess_identity", "ability")); // 猜测身份并刺杀

        // 清道夫
        m.put("noellesroles:scavenger", r("scavenger")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedTrigger("shop", "inventory", "shared.trigger.inventory_open") // 商店
                .addPassiveSkill("hidden_kill") // 尸体隐藏
                .addPassiveSkill("instant_knife")); // 刀杀无蓄力

        // 连环杀手
        m.put("noellesroles:serial_killer", r("serial_killer")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open") // 商店
                .addActiveUseSkill("psycho_rage", "ability") // 疯魔爆发
                .addPassiveSkill("nimble_knife")); // 快刀（冷却减半）

        // 静语者
        m.put("noellesroles:silencer", r("silencer")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedNameAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.trigger.inventory_open") // 商店
                .addActiveUseSkill("silence", "ability")); // 沉默目标

        // 毒师
        m.put("noellesroles:poisoner", r("poisoner")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedTrigger("shop", "inventory", "shared.trigger.inventory_open") // 商店
                .addItemEffectSkill("poison_needle") // 毒针
                .addItemEffectSkill("poison_gas_bomb") // 毒气弹
                .addItemEffectSkill("catalyst") // 催化剂
                .addPassiveSkill("immune_gas_bomb")); // 免疫毒气

        // 强盗
        m.put("noellesroles:bandit", r("bandit")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use") // 本能
                .addSkillWithSharedTrigger("shop", "inventory", "shared.trigger.inventory_open") // 商店
                .addItemEffectSkill("throwing_axe") // 飞斧
                .addActiveUseSkill("volley", "ability") // 掷出全部飞斧
                .addShopSkill("recover_axes")); // 回收飞斧（商店购买）

        // 指挥官
        m.put("noellesroles:commander", r("commander")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use")
                .addRoundStartSkill("killer_id")
                .addSkillWithSharedTrigger("shop", "inventory", "shared.trigger.inventory_open")
                .addActiveUseSkill("threat_mark", "ability")
                .addPassiveSkill("last_bullet"));

        m.put("noellesroles:breacher", r("breacher")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use")
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open")
                .addActiveUseSkill("create_break_point", "ability"));

        m.put("noellesroles:phonism", r("phonism")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use")
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open")
                .addActiveUseSkill("hallucinate", "ability"));

        // 噬梦者
        m.put("noellesroles:dream_eater", r("dream_eater")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use")
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open")
                .addActiveUseSkill("dream_mark", "ability")
                .addItemEffectSkill("sleeping_pill")
                .addPassiveSkill("sleep_vision"));

        // 傀儡师
        m.put("noellesroles:puppeteer", r("puppeteer")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use")
                .addSkillWithSharedNameEffectAndTrigger("shop", "inventory", "shared.name.killer_shop", "shared.effect.killer_shop", "shared.trigger.inventory_open")
                .addSkill("puppetry", "ability") // 傀儡术
                .addSkill("control", "ability") // 操纵
                .addSkill("recall", "ability2") // 回收
                .addPassiveSkill("fragility") // 提线易断
                .addPassiveSkill("body")); // 本体限制

        // ===================== 乘客阵营 =====================

        // 列车长
        m.put("noellesroles:conductor", r("conductor")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addInitialItemSkill("master_key")); // 使用万能钥匙开门

        // 超级宾格鲁斯
        m.put("noellesroles:awesome_binglus", r("awesome_binglus")); // 便签

        // 酒保
        m.put("noellesroles:bartender", r("bartender")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger") // 乘客基础能力
                .addSkillWithSharedTrigger("shop", "inventory", "shared.trigger.inventory_open") // 佳酿商店
                .addPassiveSkill("see_drinkers") // 观察喝过酒的玩家
                .addPassiveSkill("wine_tasting")); // 每完成两次喝点东西任务获得调酒材料

        // 大嗓门
        m.put("noellesroles:noisemaker", r("noisemaker")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addActiveUseSkill("broadcast", "ability")
                .addSkill("death_scream")); // 死亡尖叫播报

        // 巫毒师
        m.put("noellesroles:voodoo", r("voodoo")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addShopSkill("bind_curse")); // 购买巫毒娃娃并绑定诅咒

        // 验尸官
        m.put("noellesroles:coroner", r("coroner")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addPassiveSkill("examine_body")); // 验尸获取信息

        // 回溯者
        m.put("noellesroles:recaller", r("recaller")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addPassiveSkill("earn_money") // 赚钱
                .addActiveUseSkill("teleport", "ability")); // 传送回标记点

        // 计时员
        m.put("noellesroles:time_keeper", r("time_keeper")
                .addSharedNamedPassiveSkill("passenger", "shared.name.passenger")
                .addShopSkill("reduce_time")); // 花费金币减少时间

        // 卧底
        m.put("noellesroles:undercover", r("undercover")
                .addSharedNamedPassiveSkill("passenger", "shared.name.passenger")
                .addInitialItemSkill("walkie_talkie")
                .addPassiveSkill("disguise")); // 伪装混入杀手

        // 毒理学家
        m.put("noellesroles:toxicologist", r("toxicologist")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addPassiveSkill("see_poisoned") // 观察中毒玩家
                .addInitialItemSkill("antidote")); // 使用解毒剂

        // 教授
        m.put("noellesroles:professor", r("professor")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addInitialItemSkill("iron_man_vial")
                .addPassiveSkill("see_buffed")); // 识别护盾目标

        // 乘务员
        m.put("noellesroles:attendant", r("attendant")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addInitialItemSkill("manifest")); // 查阅乘客登记表

        // 记者
        m.put("noellesroles:reporter", r("reporter")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addShopSkill("shop")
                .addActiveUseSkill("mark_target", "ability")); // 标记并透视目标

        // 老兵
        m.put("wathe:veteran", r("veteran")
                .addSharedNamedPassiveSkill("passenger", "shared.name.passenger") // 乘客基础能力
                .addInitialItemSkill("knife") // 携带刀
                .addPassiveSkill("immune_blackout")); // 免疫停电

        // 保镖
        m.put("noellesroles:bodyguard", r("bodyguard")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addActiveUseSkill("defensive_stance", "ability")); // 空手按G进入潜行姿态,正面格挡刀刺/球棒

        // 生存大师
        m.put("noellesroles:survival_master", r("survival_master")
                .addSharedNamedPassiveSkill("passenger", "shared.name.passenger") // 乘客基础能力与隐匿特性
                .addSkill("survival_moment")); // 触发生存时刻

        m.put("noellesroles:engineer", r("engineer")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addInitialItemSkill("repair_tool")
                .addPassiveSkill("door_sense")
                .addSharedNamedPassiveSkillWithSharedEffect("trap_control", "shared.name.trap_control", "shared.effect.trap_control"));

        m.put("noellesroles:riot_patrol", r("riot_patrol")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addInitialItemSkill("riot_shield")
                .addInitialItemSkill("riot_fork")
                .addSharedNamedPassiveSkillWithSharedEffect("trap_control", "shared.name.trap_control", "shared.effect.trap_control"));

        m.put("noellesroles:hunter", r("hunter")
                .addSkillWithSharedNameEffectAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.effect.killer_instinct", "shared.trigger.hold_use")
                .addInventoryOpenSkill("shop")
                .addItemEffectSkill("trap")
                .addItemEffectSkill("shotgun")
                .addItemEffectSkill("shotgun_shell"));

        m.put("noellesroles:orthopedist", r("orthopedist")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addPassiveSkill("see_bone_setting")
                .addPassiveSkill("swift_stride")
                .addActiveUseSkill("ancient_healing", "ability"));

        // 圣徒
        m.put("noellesroles:saint", r("saint")
                .addSharedNamedPassiveSkill("passenger", "shared.name.passenger")
                .addPassiveSkill("divine_focus")
                .addActiveUseSkill("hellfire", "ability"));

        // 报丧
        m.put("noellesroles:baosang", r("baosang")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addPassiveSkill("death_sense"));

        // 拾荒者
        m.put("noellesroles:junker", r("junker")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addPassiveSkill("loot_vision")
                .addActiveUseSkill("search_corpse", "ability"));

        // 经济学家
        m.put("noellesroles:economist", r("economist")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addShopSkill("shop")
                .addItemEffectSkill("economic_report"));

        // 白骑士
        m.put("noellesroles:white_knight", r("white_knight")
                .addSharedNamedPassiveSkillWithSharedEffect("passenger", "shared.name.passenger", "shared.effect.passenger")
                .addInitialItemSkill("revolver")
                .addPassiveSkill("psycho_vision")
                .addPassiveSkill("bounty"));

        // ===================== 中立阵营 =====================

        // 小丑
        m.put("noellesroles:jester", r("jester")
                .addSharedNamedPassiveSkill("no_sanity", "shared.name.neutral") // 没有理智值
                .addSkill("psycho_mode")); // 进入疯魔模式

        // 秃鹫
        m.put("noellesroles:vulture", r("vulture")
                .addSharedNamedHoldUseSkill("no_sanity", "instinct", "shared.name.neutral") // 中立基础能力与本能感知
                .addSkillWithSharedNameEffectAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.effect.neutral_master_key", "shared.trigger.initial_item") // 中立万能钥匙
                .addActiveUseSkill("eat_body", "ability")); // 吃掉尸体并获得后续增益

        // 黑警
        m.put("noellesroles:corrupt_cop", r("corrupt_cop")
                .addSharedNamedPassiveSkill("neutral", "shared.name.neutral") // 中立基础能力
                .addSkillWithSharedNameAndTrigger("revolver", null, "shared.name.revolver", "shared.trigger.initial_item") // 初始左轮
                .addSkillWithSharedNameAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.trigger.initial_item") // 中立万能钥匙
                .addSharedPassiveSkill("block_victory") // 阻止其他阵营获胜
                .addSharedNamedPassiveSkillWithSharedEffect("trap_control", "shared.name.trap_control", "shared.effect.trap_control")
                .addSkill("moment")); // 触发黑警时刻

        // 病原体
        m.put("noellesroles:gangster_star", r("gangster_star")
                .addSharedNamedPassiveSkill("neutral", "shared.name.neutral")
                .addSkillWithSharedNameAndTrigger("instinct", "instinct", "shared.name.killer_instinct", "shared.trigger.hold_use")
                .addSkillWithSharedNameAndTrigger("revolver", null, "shared.name.revolver", "shared.trigger.initial_item")
                .addSkillWithSharedNameAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.trigger.initial_item")
                // 任务金币已写入中立效果说明，避免生成缺少名称的被动技能词条。
                .addShopSkill("bullet")
                .addShopSkill("gangster_spirit"));

        m.put("noellesroles:pathogen", r("pathogen")
                .addSharedNamedPassiveSkill("no_sanity", "shared.name.neutral") // 中立基础能力与感染者高亮
                .addSkillWithSharedNameEffectAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.effect.neutral_master_key", "shared.trigger.initial_item") // 中立万能钥匙
                .addActiveUseSkill("infect", "ability")); // 感染玩家

        // 饕餮
        m.put("noellesroles:taotie", r("taotie")
                .addSharedNamedPassiveSkill("no_sanity", "shared.name.neutral") // 没有理智值
                .addSkillWithSharedNameEffectAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.effect.neutral_master_key", "shared.trigger.initial_item") // 中立万能钥匙
                .addSharedPassiveSkill("block_victory") // 阻止其他阵营获胜
                .addPassiveSkill("gluttony") // 暴食：饥饿值机制
                .addActiveUseSkill("swallow_skill", "ability") // 吞噬玩家
                .addSkill("moment")); // 触发饕餮时刻

        m.put("noellesroles:criminal_reasoner", r("criminal_reasoner")
                .addSharedNamedPassiveSkill("neutral", "shared.name.neutral")
                .addSkillWithSharedNameEffectAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.effect.neutral_master_key", "shared.trigger.initial_item") // 中立万能钥匙
                .addActiveUseSkill("reason", "ability") // 推理命案
                .addSkillWithSharedTrigger("invisibility", "ability2", "shared.trigger.active_use")); // 推理成功奖励的隐身

        m.put("noellesroles:ferryman", r("ferryman")
                .addSharedNamedHoldUseSkill("neutral", "instinct", "shared.name.neutral")
                .addSkillWithSharedNameEffectAndTrigger("neutral_master_key", null, "shared.name.neutral_master_key", "shared.effect.neutral_master_key", "shared.trigger.initial_item")
                .addActiveUseSkill("ferry", "ability")
                .addSkill("netherwalker", "ability"));

        return m;
    }

    private static Map<String, RoleInfoData> createCategoryPages() {
        Map<String, RoleInfoData> m = new LinkedHashMap<>();
        m.put(CATEGORY_CLASSIC, new RoleInfoData(
                "classic_murder_category",
                "tr:roleinfo.category.classic.name",
                null,
                "tr:roleinfo.category.classic.description",
                null
        ));
        m.put(CATEGORY_CLASSIC_OVERVIEW, new RoleInfoData(
                "classic_murder_overview",
                "tr:roleinfo.category.classic.name",
                null,
                "tr:roleinfo.category.classic.description",
                null
        ));
        m.put(CATEGORY_MURDER_MAYHEM, new RoleInfoData(
                "murder_mayhem_category",
                "tr:roleinfo.category.murder_mayhem.name",
                null,
                "tr:roleinfo.category.murder_mayhem.description",
                null
        ));
        m.put(EVENT_MURDER_MAYHEM_FOG_OF_WAR, new RoleInfoData(
                "murder_mayhem_fog_of_war",
                "tr:event.noellesroles.murder_mayhem.fog_of_war",
                null,
                "tr:event_description.noellesroles.murder_mayhem.fog_of_war",
                null
        ).addEventEffect(
                "fog",
                "tr:roleinfo.event_effect.murder_mayhem_fog_of_war.fog.name",
                "tr:roleinfo.event_effect.murder_mayhem_fog_of_war.fog.effect"
        ).addEventEffect(
                "hallucination",
                "tr:roleinfo.event_effect.murder_mayhem_fog_of_war.hallucination.name",
                "tr:roleinfo.event_effect.murder_mayhem_fog_of_war.hallucination.effect"
        ));
        m.put(CATEGORY_LOOSE_ENDS, new RoleInfoData(
                "loose_ends_category",
                "tr:roleinfo.category.loose_ends.name",
                null,
                "tr:roleinfo.category.loose_ends.description",
                null
        ));
        m.put(CATEGORY_LOOSE_ENDS_OVERVIEW, new RoleInfoData(
                "loose_ends_overview",
                "tr:roleinfo.category.loose_ends.name",
                null,
                "tr:roleinfo.category.loose_ends.description",
                null
        ));
        m.put(CATEGORY_DEATH_ARENA, new RoleInfoData(
                "death_arena_category",
                "tr:roleinfo.category.death_arena.name",
                null,
                "tr:roleinfo.category.death_arena.description",
                null
        ));
        m.put(CATEGORY_DEATH_ARENA_OVERVIEW, new RoleInfoData(
                "death_arena_overview",
                "tr:roleinfo.category.death_arena.name",
                null,
                "tr:roleinfo.category.death_arena.description",
                null
        ));
        m.put(CATEGORY_PASSENGER, new RoleInfoData(
                "passenger_category",
                "tr:roleinfo.category.passenger.name",
                null,
                "tr:roleinfo.category.passenger.description",
                "tr:roleinfo.win_condition.default.innocent"
        ));
        m.put(CATEGORY_KILLER, new RoleInfoData(
                "killer_category",
                "tr:roleinfo.category.killer.name",
                null,
                "tr:roleinfo.category.killer.description",
                "tr:roleinfo.win_condition.default.killer"
        ));
        m.put(CATEGORY_NEUTRAL, new RoleInfoData(
                "neutral_category",
                "tr:roleinfo.category.neutral.name",
                null,
                "tr:roleinfo.category.neutral.description",
                "tr:roleinfo.win_condition.default.neutral"
        ));
        return m;
    }
}

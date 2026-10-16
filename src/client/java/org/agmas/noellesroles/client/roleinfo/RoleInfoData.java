package org.agmas.noellesroles.client.roleinfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data class for role information.
 * String values are translation keys resolved via Minecraft i18n.
 */
public class RoleInfoData {
    public String nameKey;
    public String factionKey;
    /** One-line plain-language summary of the role kit, shown on the inventory card. */
    public String summaryKey;
    public String descriptionKey;
    public String winConditionKey;
    private String roleId;
    public Map<String, SkillInfoData> skills;

    public RoleInfoData(String roleId, String nameKey, String factionKey, String descriptionKey, String winConditionKey) {
        this.roleId = roleId;
        this.nameKey = nameKey;
        this.factionKey = factionKey;
        this.descriptionKey = descriptionKey;
        this.winConditionKey = winConditionKey;
        this.skills = new LinkedHashMap<>();
    }

    /**
     * Fluent shortcut to add a role skill using the default translation key pattern.
     */
    public RoleInfoData addSkill(String skillId, String triggerKeybind) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, base + "name", base + "trigger", base + "effect");
    }

    public RoleInfoData addSkillWithSharedTrigger(String skillId, String triggerKeybind, String sharedTriggerKey) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, base + "name", "tr:" + sharedTriggerKey, base + "effect");
    }

    public RoleInfoData addSkillWithSharedNameAndTrigger(String skillId, String triggerKeybind, String sharedNameKey, String sharedTriggerKey) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, "tr:" + sharedNameKey, "tr:" + sharedTriggerKey, base + "effect");
    }

    public RoleInfoData addSkillWithSharedName(String skillId, String triggerKeybind, String sharedNameKey) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, "tr:" + sharedNameKey, base + "trigger", base + "effect");
    }

    public RoleInfoData addSkillWithSharedEffect(String skillId, String triggerKeybind, String sharedEffectKey) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, base + "name", base + "trigger", "tr:" + sharedEffectKey);
    }

    public RoleInfoData addSkillWithSharedNameAndEffect(String skillId, String triggerKeybind, String sharedNameKey, String sharedEffectKey) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, "tr:" + sharedNameKey, base + "trigger", "tr:" + sharedEffectKey);
    }

    public RoleInfoData addSkillWithSharedNameEffectAndTrigger(String skillId, String triggerKeybind, String sharedNameKey, String sharedEffectKey, String sharedTriggerKey) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        return addSkillWithKeys(skillId, triggerKeybind, "tr:" + sharedNameKey, "tr:" + sharedTriggerKey, "tr:" + sharedEffectKey);
    }

    public RoleInfoData addActiveUseSkill(String skillId, String triggerKeybind) {
        return addSkillWithSharedTrigger(skillId, triggerKeybind, "shared.trigger.active_use");
    }

    public RoleInfoData addHoldUseSkill(String skillId, String triggerKeybind) {
        return addSkillWithSharedTrigger(skillId, triggerKeybind, "shared.trigger.hold_use");
    }

    public RoleInfoData addInventoryOpenSkill(String skillId) {
        return addSkillWithSharedTrigger(skillId, "inventory", "shared.trigger.inventory_open");
    }

    public RoleInfoData addPassiveSkill(String skillId) {
        return addSkillWithSharedTrigger(skillId, null, "shared.trigger.passive");
    }

    public RoleInfoData addInitialItemSkill(String skillId) {
        return addSkillWithSharedTrigger(skillId, null, "shared.trigger.initial_item");
    }

    public RoleInfoData addItemEffectSkill(String skillId) {
        return addSkillWithSharedTrigger(skillId, null, "shared.trigger.item_effect");
    }

    public RoleInfoData addItemEffectSkill(String skillId, String triggerKeybind) {
        return addSkillWithSharedTrigger(skillId, triggerKeybind, "shared.trigger.item_effect");
    }

    public RoleInfoData addRoundStartSkill(String skillId) {
        return addSkillWithSharedTrigger(skillId, null, "shared.trigger.round_start");
    }

    public RoleInfoData addShopSkill(String skillId) {
        return addSkillWithSharedTrigger(skillId, null, "shared.trigger.shop");
    }

    public RoleInfoData addSharedNamedHoldUseSkill(String skillId, String triggerKeybind, String sharedNameKey) {
        return addSkillWithSharedNameAndTrigger(skillId, triggerKeybind, sharedNameKey, "shared.trigger.hold_use");
    }

    public RoleInfoData addSharedNamedPassiveSkill(String skillId, String sharedNameKey) {
        return addSkillWithSharedNameAndTrigger(skillId, null, sharedNameKey, "shared.trigger.passive");
    }

    public RoleInfoData addSharedNamedPassiveSkillWithSharedEffect(String skillId, String sharedNameKey, String sharedEffectKey) {
        return addSkillWithSharedNameEffectAndTrigger(skillId, null, sharedNameKey, sharedEffectKey, "shared.trigger.passive");
    }

    private RoleInfoData addSkillWithKeys(String skillId, String triggerKeybind, String nameKey, String triggerKey, String effectKey) {
        SkillInfoData s = new SkillInfoData();
        s.nameKey = nameKey;
        s.triggerKey = triggerKey;
        s.triggerKeybind = triggerKeybind;
        s.effectKey = effectKey;
        skills.put(skillId, s);
        return this;
    }

    public RoleInfoData addSkill(String skillId) {
        return addSkill(skillId, null);
    }

    /**
     * Fluent shortcut to add a shared skill using the shared translation key pattern.
     */
    public RoleInfoData addSharedSkill(String skillId, String triggerKeybind) {
        String base = "tr:roleinfo.skill.shared." + skillId + ".";
        return addSharedSkillWithKeys(skillId, triggerKeybind, base + "name", base + "trigger", base + "effect");
    }

    public RoleInfoData addSharedSkillWithSharedTrigger(String skillId, String triggerKeybind, String sharedTriggerKey) {
        String base = "tr:roleinfo.skill.shared." + skillId + ".";
        return addSharedSkillWithKeys(skillId, triggerKeybind, base + "name", "tr:" + sharedTriggerKey, base + "effect");
    }

    public RoleInfoData addSharedPassiveSkill(String skillId) {
        return addSharedSkillWithSharedTrigger(skillId, null, "shared.trigger.passive");
    }

    public RoleInfoData addEventEffect(String effectId, String nameKey, String effectKey) {
        SkillInfoData s = new SkillInfoData();
        s.nameKey = nameKey;
        s.triggerKey = null;
        s.triggerKeybind = null;
        s.effectKey = effectKey;
        skills.put(effectId, s);
        return this;
    }

    private RoleInfoData addSharedSkillWithKeys(String skillId, String triggerKeybind, String nameKey, String triggerKey, String effectKey) {
        SkillInfoData s = new SkillInfoData();
        s.nameKey = nameKey;
        s.triggerKey = triggerKey;
        s.triggerKeybind = triggerKeybind;
        s.effectKey = effectKey;
        skills.put(skillId, s);
        return this;
    }

    public RoleInfoData addSharedSkill(String skillId) {
        return addSharedSkill(skillId, null);
    }

    public static class SkillInfoData {
        /** Translation key for skill name */
        public String nameKey;
        /** Translation key for trigger description (may contain %s placeholder for resolved keybind name) */
        public String triggerKey;
        /**
         * Keybind ID to resolve and pass as %s to triggerKey.
         * Supported values: "ability", "inventory", "use", "attack", "sprint", or null/empty for passive (no substitution).
         */
        public String triggerKeybind;
        /** Translation key for skill effect description */
        public String effectKey;
    }
}

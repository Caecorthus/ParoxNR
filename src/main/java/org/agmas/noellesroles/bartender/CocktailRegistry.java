package org.agmas.noellesroles.bartender;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鸡尾酒命名注册表。
 * 根据基酒中的调剂组合（忽略冰块）查找鸡尾酒名称和颜色。
 */
public class CocktailRegistry {

    public record CocktailEntry(String translationKey, int color) {}

    private static final Map<Set<String>, CocktailEntry> COCKTAILS = new HashMap<>();

    static {
        // 一级酒类（1 种调剂）
        register("cocktail.noellesroles.cuba_libre",         0xD4A574, "rum");             // 淡琥珀
        register("cocktail.noellesroles.gin_tonic",          0x87CEEB, "gin");             // 天蓝
        register("cocktail.noellesroles.highball",           0xDAA520, "whiskey");          // 金棕
        register("cocktail.noellesroles.screwdriver",        0xFFA500, "vodka");            // 橙色
        register("cocktail.noellesroles.tequila_shot",       0xC8E600, "tequila");          // 黄绿

        // 二级酒类（2 种调剂）
        register("cocktail.noellesroles.between_the_sheets", 0xF5DEB3, "rum", "gin");      // 小麦色
        register("cocktail.noellesroles.el_presidente",      0xCD853F, "rum", "whiskey");   // 秘鲁棕
        register("cocktail.noellesroles.daiquiri",           0xFFB6C1, "rum", "vodka");     // 浅粉
        register("cocktail.noellesroles.canchancha",         0xBDB76B, "rum", "tequila");   // 暗卡其
        register("cocktail.noellesroles.negroni",            0xCC3333, "gin", "whiskey");   // 暗红
        register("cocktail.noellesroles.white_lady",         0xF0F0F0, "gin", "vodka");     // 银白
        register("cocktail.noellesroles.aviation",           0x9370DB, "gin", "tequila");   // 淡紫
        register("cocktail.noellesroles.mizuwari",           0xC9B37E, "whiskey", "vodka"); // 琥珀淡
        register("cocktail.noellesroles.silent_third",       0x8B6914, "whiskey", "tequila"); // 深金
        register("cocktail.noellesroles.moscow_mule",        0xB8860B, "vodka", "tequila"); // 暗金杆

        // 三级酒类（3 种调剂）
        register("cocktail.noellesroles.long_island",        0xD2691E, "rum", "gin", "vodka");       // 巧克力棕
        register("cocktail.noellesroles.godfather",          0x8B4513, "rum", "gin", "whiskey");     // 马鞍棕
        register("cocktail.noellesroles.margarita",          0x7FFF00, "rum", "gin", "tequila");     // 查特酒绿
        register("cocktail.noellesroles.manhattan",          0xB22222, "rum", "whiskey", "vodka");   // 火砖红
        register("cocktail.noellesroles.icebreaker",          0x87CEFA, "rum", "whiskey", "tequila"); // 浅天蓝
        register("cocktail.noellesroles.xyz",                0xFFD700, "rum", "vodka", "tequila");   // 金色
        register("cocktail.noellesroles.playboy",            0xFF69B4, "gin", "whiskey", "vodka");   // 热粉红
        register("cocktail.noellesroles.see_you_tomorrow",   0x4B0082, "gin", "whiskey", "tequila"); // 靛蓝
        register("cocktail.noellesroles.tequila_sunrise",    0xFF4500, "gin", "vodka", "tequila");   // 橙红
        register("cocktail.noellesroles.witchs_remedy",      0x556B2F, "whiskey", "vodka", "tequila"); // 暗橄榄绿

        // 白兰地系列（1 种调剂）
        register("cocktail.noellesroles.sidecar",            0xD9A441, "brandy");                  // 边车·琥珀金
        // 白兰地系列（2 种调剂）
        register("cocktail.noellesroles.coquette",           0xD98CA5, "brandy", "rum");           // 千娇百媚·娇粉
        register("cocktail.noellesroles.trantin",            0xC9A66B, "brandy", "gin");           // 特朗坦·浅棕
        register("cocktail.noellesroles.nikolas",            0xF0D9A6, "brandy", "vodka");         // 尼古拉斯·淡金
        register("cocktail.noellesroles.alexander",          0xEAD9C0, "brandy", "tequila");       // 亚历山大·奶油
        register("cocktail.noellesroles.one_more_road",      0x9C6B30, "brandy", "whiskey");       // 多一条路·深棕
        // 白兰地系列（3 种调剂）
        register("cocktail.noellesroles.nightcap",           0x6B4226, "brandy", "rum", "gin");     // 睡帽·暗褐
        register("cocktail.noellesroles.brandy_fix",         0xC98A45, "brandy", "rum", "vodka");   // 白兰地修复·琥珀
        register("cocktail.noellesroles.horses_neck",        0xD9B26B, "brandy", "rum", "tequila"); // 马脖·姜黄
        register("cocktail.noellesroles.witch_elizabeth",    0x7A4E8C, "brandy", "rum", "whiskey"); // 女巫伊丽莎白·巫紫
        register("cocktail.noellesroles.champs_elysees",     0xE3B34B, "brandy", "gin", "vodka");   // 香榭丽舍大街·金
        register("cocktail.noellesroles.honeymoon",          0xF2D57A, "brandy", "gin", "tequila"); // 蜜月·蜜金
        register("cocktail.noellesroles.olympic",            0xD99A3C, "brandy", "gin", "whiskey"); // 奥林匹克·铜金
        register("cocktail.noellesroles.dream",              0xB8CDE0, "brandy", "vodka", "tequila"); // 梦·梦蓝
        register("cocktail.noellesroles.baltimore_trailblazer", 0xA85F2E, "brandy", "vodka", "whiskey"); // 巴尔的摩开拓者·开拓棕
        register("cocktail.noellesroles.via_veneto",         0xB5651D, "brandy", "tequila", "whiskey"); // 威尼托大街·威尼托琥珀
    }

    private static void register(String translationKey, int color, String... ingredientIds) {
        COCKTAILS.put(Set.of(ingredientIds), new CocktailEntry(translationKey, color));
    }

    /**
     * 通过调剂列表查找鸡尾酒条目（忽略冰块）。
     */
    public static @Nullable CocktailEntry getEntry(List<String> ingredients) {
        Set<String> set = ingredients.stream()
                .filter(id -> !"ice_cube".equals(id))
                .filter(id -> !"liqueur".equals(id))
                .filter(id -> !"special_spice".equals(id))
                .collect(Collectors.toSet());
        if (set.isEmpty()) return null;
        return COCKTAILS.get(set);
    }

    /**
     * 通过调剂列表查找鸡尾酒翻译 key（忽略冰块）。
     */
    public static @Nullable String getCocktailKey(List<String> ingredients) {
        CocktailEntry entry = getEntry(ingredients);
        return entry != null ? entry.translationKey() : null;
    }

    /**
     * 通过翻译 key 查找鸡尾酒颜色。
     */
    public static int getColorByKey(String translationKey) {
        for (CocktailEntry entry : COCKTAILS.values()) {
            if (entry.translationKey().equals(translationKey)) return entry.color();
        }
        return 0xFFFFFF;
    }

    /**
     * 获取鸡尾酒带颜色的显示名称（含加冰后缀）。
     * 用于物品名和回放格式化。
     */
    public static @Nullable MutableText getCocktailName(List<String> ingredients) {
        CocktailEntry entry = getEntry(ingredients);
        if (entry == null) return null;
        MutableText name = Text.translatable(entry.translationKey())
                .setStyle(Style.EMPTY.withColor(entry.color()));
        if (ingredients.contains("ice_cube")) {
            name.append(Text.translatable("cocktail.noellesroles.iced_suffix")
                    .setStyle(Style.EMPTY.withColor(entry.color())));
        }
        return name;
    }

    /**
     * 通过翻译 key 获取带颜色的鸡尾酒名称文本。
     * 用于回放格式化器从存储的 key 还原显示名。
     */
    public static MutableText getColoredName(String translationKey) {
        int color = getColorByKey(translationKey);
        return Text.translatable(translationKey).setStyle(Style.EMPTY.withColor(color));
    }
}

package org.agmas.noellesroles;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import org.agmas.noellesroles.item.AntidoteItem;
import org.agmas.noellesroles.item.AdrenalineItem;
import org.agmas.noellesroles.item.AssassinBookItem;
import org.agmas.noellesroles.item.AttendantRegisterItem;
import org.agmas.noellesroles.item.FineDrinkItem;
import org.agmas.noellesroles.item.ForgedPotionItem;
import org.agmas.noellesroles.item.TimedBombItem;
import org.agmas.noellesroles.item.IronManVialItem;
import org.agmas.noellesroles.item.PoisonNeedleItem;
import org.agmas.noellesroles.item.PoisonGasBombItem;
import org.agmas.noellesroles.item.CatalystItem;
import org.agmas.noellesroles.item.HallucinationMedicineItem;
import org.agmas.noellesroles.item.PortableRadarItem;
import org.agmas.noellesroles.item.RepairToolItem;
import org.agmas.noellesroles.item.RiotForkItem;
import org.agmas.noellesroles.item.RiotShieldItem;
import org.agmas.noellesroles.item.ShopIconItem;
import org.agmas.noellesroles.item.SilenceDeviceItem;
import org.agmas.noellesroles.item.SleepingPillItem;
import org.agmas.noellesroles.item.SoundCollectorItem;
import org.agmas.noellesroles.item.ThrowingAxeItem;
import org.agmas.noellesroles.item.HunterTrapItem;
import org.agmas.noellesroles.item.DoubleBarrelShotgunItem;
import org.agmas.noellesroles.item.DoubleBarrelShellItem;
import org.agmas.noellesroles.item.GangsterSpiritItem;
import org.agmas.noellesroles.item.GangsterStarBulletItem;
import org.agmas.noellesroles.item.MedicalSplintItem;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.agmas.noellesroles.item.IngredientItem;
import org.agmas.noellesroles.item.VoodooDollItem;
import org.agmas.noellesroles.item.RecallerAnchorItem;
import org.agmas.noellesroles.item.RewindShardItem;
import org.agmas.noellesroles.item.ingredient.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    private static final List<Item> MOD_ITEM_GROUP_ENTRIES = new ArrayList<>();
    public static final RegistryKey<ItemGroup> NOELLESROLES_ITEMS_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(Noellesroles.MOD_ID, "items")
    );

    public static void init() {
        Registry.register(
                Registries.ITEM_GROUP,
                NOELLESROLES_ITEMS_GROUP_KEY,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(FINE_DRINK))
                        .displayName(Text.translatable("itemGroup.noellesroles.items"))
                        .entries((displayContext, entries) -> MOD_ITEM_GROUP_ENTRIES.forEach(entries::add))
                        .build()
        );

        // 注册调剂到 IngredientItem 静态注册表
        IngredientItem.register((IngredientItem) RUM);
        IngredientItem.register((IngredientItem) GIN);
        IngredientItem.register((IngredientItem) VODKA);
        IngredientItem.register((IngredientItem) TEQUILA);
        IngredientItem.register((IngredientItem) WHISKEY);
        IngredientItem.register((IngredientItem) BRANDY);
        IngredientItem.register((IngredientItem) ICE_CUBE);
        IngredientItem.register((IngredientItem) LIQUEUR);
        IngredientItem.register((IngredientItem) SPECIAL_SPICE);
    }

    public static final Item MASTER_KEY = register(
            new Item(new Item.Settings().maxCount(1)),
            "master_key"
    );
    public static final Item NEUTRAL_MASTER_KEY = register(
            new Item(new Item.Settings().maxCount(1)),
            "neutral_master_key"
    );
    public static final Item ROLE_MINE = registerHidden(
            new Item(new Item.Settings().maxCount(1)),
            "role_mine"
    );
    public static final Item TIMED_BOMB = register(
            new TimedBombItem(new Item.Settings().maxCount(1)),
            "timed_bomb"
    );
    public static final Item FINE_DRINK = registerHidden(
            new FineDrinkItem(new Item.Settings().maxCount(1)),
            "fine_drink"
    );
    public static final Item IRON_MAN_VIAL = register(
            new IronManVialItem(new Item.Settings().maxCount(1)),
            "iron_man_vial"
    );
    public static final Item ANTIDOTE = register(
            new AntidoteItem(new Item.Settings().maxCount(1)),
            "antidote"
    );
    public static final Item HALLUCINATION_MEDICINE = register(
            new HallucinationMedicineItem(new Item.Settings().maxCount(1)),
            "hallucination_medicine"
    );
    public static final Item ADRENALINE = register(
            new AdrenalineItem(new Item.Settings().maxCount(1)),
            "adrenaline"
    );
    public static final Item POISON_NEEDLE = register(
            new PoisonNeedleItem(new Item.Settings().maxCount(1)),
            "poison_needle"
    );
    public static final Item POISON_GAS_BOMB = register(
            new PoisonGasBombItem(new Item.Settings().maxCount(16)),
            "poison_gas_bomb"
    );
    public static final Item THROWING_AXE = register(
            new ThrowingAxeItem(new Item.Settings().maxCount(1)),
            "throwing_axe"
    );
    // 强盗「回收飞斧」：购买即生效，不进入背包（商店图标物品，隐藏于创造模式物品栏）
    public static final Item RECOVER_AXE = registerHidden(
            new ShopIconItem(new Item.Settings().maxCount(1), "item.noellesroles.recover_axe.tooltip"),
            "recover_axe"
    );
    public static final Item CATALYST = register(
            new CatalystItem(new Item.Settings().maxCount(1)),
            "catalyst"
    );
    public static final Item RIOT_SHIELD = register(
            new RiotShieldItem(new Item.Settings().maxCount(1)),
            "riot_shield"
    );
    public static final Item RIOT_FORK = register(
            new RiotForkItem(new Item.Settings().maxCount(1)),
            "riot_fork"
    );
    public static final Item PORTABLE_RADAR = register(
            new PortableRadarItem(new Item.Settings().maxCount(1)),
            "portable_radar"
    );
    public static final Item MEDICAL_SPLINT = register(
            new MedicalSplintItem(new Item.Settings().maxCount(1)),
            "medical_splint"
    );
    public static final Item HUNTER_TRAP = register(
            new HunterTrapItem(new Item.Settings().maxCount(16)),
            "hunter_trap"
    );
    public static final Item DOUBLE_BARREL_SHOTGUN = register(
            new DoubleBarrelShotgunItem(new Item.Settings().maxCount(1)),
            "double_barrel_shotgun"
    );
    public static final Item DOUBLE_BARREL_SHELL = register(
            new DoubleBarrelShellItem(new Item.Settings().maxCount(16)),
            "double_barrel_shell"
    );
    public static final Item GANGSTER_STAR_BULLET = registerHidden(
            new GangsterStarBulletItem(new Item.Settings().maxCount(16)),
            "gangster_star_bullet"
    );
    public static final Item GANGSTER_SPIRIT = registerHidden(
            new GangsterSpiritItem(new Item.Settings().maxCount(1)),
            "gangster_spirit"
    );
    // 经济学家商店商品：购买即生效，不进入背包
    public static final Item ECONOMIC_REPORT = registerHidden(
            new Item(new Item.Settings().maxCount(1)),
            "economic_report"
    );

    // ---- 噬梦者系统 ----
    public static final Item SLEEPING_PILL = register(
            new SleepingPillItem(new Item.Settings().maxCount(1)),
            "sleeping_pill"
    );
    // 3x蝎子与降帷：购买即生效，不进入背包（商店图标物品，隐藏于创造模式物品栏）
    public static final Item SCORPION_X3 = registerHidden(
            new ShopIconItem(new Item.Settings().maxCount(1), "item.noellesroles.scorpion_x3.tooltip"),
            "scorpion_x3"
    );
    public static final Item CURTAIN = registerHidden(
            new ShopIconItem(new Item.Settings().maxCount(1), "item.noellesroles.curtain.tooltip"),
            "curtain"
    );

    // ---- 巫毒师系统 ----
    // 商店购买道具，隐藏于创造模式物品栏
    public static final Item VOODOO_DOLL = registerHidden(
            new VoodooDollItem(new Item.Settings().maxCount(1)),
            "voodoo_doll"
    );

    // ---- 刺客系统 ----
    // 刺客商店购买道具，隐藏于创造模式物品栏
    public static final Item ASSASSIN_BOOK = registerHidden(
            new AssassinBookItem(new Item.Settings().maxCount(1)),
            "assassin_book"
    );

    // ---- 静语者系统 ----
    // 静语者商店购买道具，隐藏于创造模式物品栏
    public static final Item SILENCE_DEVICE = registerHidden(
            new SilenceDeviceItem(new Item.Settings().maxCount(1)),
            "silence_device"
    );

    // ---- 回溯者系统 ----
    // 商店购买道具，隐藏于创造模式物品栏
    public static final Item RECALLER_ANCHOR = registerHidden(
            new RecallerAnchorItem(new Item.Settings().maxCount(1)),
            "recaller_anchor"
    );
    public static final Item REWIND_SHARD = registerHidden(
            new RewindShardItem(new Item.Settings().maxCount(16)),
            "rewind_shard"
    );

    // ---- 谎言音效师系统 ----
    // 音效师商店购买道具，隐藏于创造模式物品栏
    public static final Item SOUND_COLLECTOR = registerHidden(
            new SoundCollectorItem(new Item.Settings().maxCount(1)),
            "sound_collector"
    );

    // ---- 乘务员系统 ----
    // 开局发放道具，隐藏于创造模式物品栏
    public static final Item ATTENDANT_REGISTER = registerHidden(
            new AttendantRegisterItem(new Item.Settings().maxCount(1)),
            "attendant_register"
    );

    // ---- 变形者系统 ----
    // 「万千变化」：购买即生效，不进入背包（商店图标物品，隐藏于创造模式物品栏）
    public static final Item MYRIAD_CHANGES = registerHidden(
            new ShopIconItem(new Item.Settings().maxCount(1), "item.noellesroles.myriad_changes.tooltip"),
            "myriad_changes"
    );
    // 「赝造药水」：对尸体使用，把尸体皮肤伪装成指定玩家
    public static final Item FORGED_POTION = registerHidden(
            new ForgedPotionItem(new Item.Settings().maxCount(1)),
            "forged_potion"
    );

    // ---- 工程师系统 ----
    public static final Item REPAIR_TOOL = register(
            new RepairToolItem(new Item.Settings().maxCount(1)),
            "repair_tool"
    );

    // ---- 调酒师系统 ----
    public static final Item BASE_SPIRIT = register(
            new BaseSpiritItem(new Item.Settings().maxCount(1)),
            "base_spirit"
    );
    public static final Item RUM = register(
            new RumItem(new Item.Settings().maxCount(1)),
            "rum"
    );
    public static final Item GIN = register(
            new GinItem(new Item.Settings().maxCount(1)),
            "gin"
    );
    public static final Item VODKA = register(
            new VodkaItem(new Item.Settings().maxCount(1)),
            "vodka"
    );
    public static final Item TEQUILA = register(
            new TequilaItem(new Item.Settings().maxCount(1)),
            "tequila"
    );
    public static final Item WHISKEY = register(
            new WhiskeyItem(new Item.Settings().maxCount(1)),
            "whiskey"
    );
    public static final Item BRANDY = register(
            new BrandyItem(new Item.Settings().maxCount(1)),
            "brandy"
    );
    public static final Item ICE_CUBE = register(
            new IceCubeItem(new Item.Settings().maxCount(1)),
            "ice_cube"
    );
    public static final Item LIQUEUR = register(
            new LiqueurItem(new Item.Settings().maxCount(1)),
            "liqueur"
    );
    public static final Item SPECIAL_SPICE = register(
            new SpecialSpiceItem(new Item.Settings().maxCount(1)),
            "special_spice"
    );

    public static Item register(Item item, String id) {
        // Create the identifier for the item.
        Identifier itemID = Identifier.of(Noellesroles.MOD_ID, id);

        // Register the item.
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);
        // 在这个工作区里，只要创建新物品时继续走这里注册，就会自动加入 NoellesRoles 道具物品栏。
        MOD_ITEM_GROUP_ENTRIES.add(registeredItem);

        // Return the registered item!
        return registeredItem;
    }

    /** 注册但不加入创造模式道具栏。 */
    public static Item registerHidden(Item item, String id) {
        Identifier itemID = Identifier.of(Noellesroles.MOD_ID, id);
        return Registry.register(Registries.ITEM, itemID, item);
    }

}

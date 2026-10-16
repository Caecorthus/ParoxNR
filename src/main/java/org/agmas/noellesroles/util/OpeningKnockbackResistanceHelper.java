package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * 开局刀冷却期间全员击退抗性 +1（抗性上限即 1.0，等同于免疫击退）：
 * 直接给玩家属性挂临时修饰符，不拦截攻击行为。
 * <p>
 * 抗性按玩家各自的开局刀冷却存续：tick 中发现冷却已结束就摘掉修饰符，
 * 因此双角色选择期间冷却被冻结时抗性也随之保留。
 */
public final class OpeningKnockbackResistanceHelper {
    /** 开局刀冷却时长（45 秒），与 Wathe 原版开局冷却一致。 */
    public static final int OPENING_KNIFE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);

    private static final Identifier RESISTANCE_MODIFIER_ID = Identifier.of(Noellesroles.MOD_ID, "opening_knockback_resistance");
    private static final double RESISTANCE_BONUS = 1.0D;

    private static final Set<UUID> openingCooldownPlayers = new HashSet<>();

    private OpeningKnockbackResistanceHelper() {}

    /** 对局开始时调用：玩家开局刀冷却期间保持击退抗性 +1。 */
    public static void startOpeningWindow(ServerPlayerEntity player) {
        openingCooldownPlayers.add(player.getUuid());
        applyResistance(player);
    }

    /** 每 tick 检查开局冷却是否结束；结束后移除抗性，死亡重生等丢失修饰符时补回。 */
    public static void tick(MinecraftServer server) {
        if (openingCooldownPlayers.isEmpty()) {
            return;
        }
        Iterator<UUID> iterator = openingCooldownPlayers.iterator();
        while (iterator.hasNext()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (!player.getItemCooldownManager().isCoolingDown(WatheItems.KNIFE)) {
                removeResistance(player);
                iterator.remove();
                continue;
            }
            applyResistance(player);
        }
    }

    /** 对局结束/新对局开始时调用：清空记录并摘掉所有在线玩家的抗性。 */
    public static void reset(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            removeResistance(player);
        }
        openingCooldownPlayers.clear();
    }

    private static void applyResistance(PlayerEntity player) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null || attribute.hasModifier(RESISTANCE_MODIFIER_ID)) {
            return;
        }
        attribute.addTemporaryModifier(new EntityAttributeModifier(
                RESISTANCE_MODIFIER_ID,
                RESISTANCE_BONUS,
                EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void removeResistance(PlayerEntity player) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null && attribute.hasModifier(RESISTANCE_MODIFIER_ID)) {
            attribute.removeModifier(RESISTANCE_MODIFIER_ID);
        }
    }
}

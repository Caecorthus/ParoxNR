package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.agmas.noellesroles.bandit.BanditPlayerComponent;
import org.agmas.noellesroles.baosang.BaosangPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.breacher.BreacherPlayerComponent;
import org.agmas.noellesroles.commander.CommanderPlayerComponent;
import org.agmas.noellesroles.economist.EconomistPlayerComponent;
import org.agmas.noellesroles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.junker.JunkerPlayerComponent;
import org.agmas.noellesroles.mengpo.MengPoPlayerComponent;
import org.agmas.noellesroles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownEntryAccessor;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownManagerAccessor;
import org.agmas.noellesroles.mixin.wathe.ScoreboardRoleSelectorComponentAccessor;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingScramblePlayerComponent;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.silencer.SilencerPlayerComponent;
import org.agmas.noellesroles.survivalmaster.SurvivalMasterPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对局中途把玩家转为另一个职业，并把他重置到「新职业开局时」的状态。
 * <p>
 * 开局初始化分散在 Wathe 与本模组多处，这里按开局顺序串起来：
 * 清旧职业自带状态 → 清技能/物品冷却 → 清物品栏（保留房门钥匙）→ 设置新职业 →
 * 重置商店 → 开局左轮/刀冷却 → 触发 RoleAssigned 发放新职业装备与开局冷却 → 同步、回放、欢迎标题。
 * <p>
 * 心情、中毒、血量、状态效果以及别人施加在该玩家身上的状态（感染、炸弹、护盾、骨折等）一律保留。
 */
public final class RoleTransformHelper {
    private RoleTransformHelper() {}

    public static void transform(ServerPlayerEntity player, Role newRole) {
        ServerWorld world = player.getServerWorld();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        Role oldRole = gameWorld.getRole(player);

        // 开局窗口内转职（如渡己）沿用剩余的开局冷却，避免把开局击退抗性延长到别人之后
        boolean openingWindow = OpeningKnockbackResistanceHelper.isInOpeningWindow(player);
        int revolverCooldown = openingWindow ? getRemainingCooldownTicks(player, WatheItems.REVOLVER) : GameConstants.getInTicks(1, 0);
        int knifeCooldown = openingWindow ? getRemainingCooldownTicks(player, WatheItems.KNIFE) : OpeningKnockbackResistanceHelper.OPENING_KNIFE_COOLDOWN_TICKS;

        clearOwnRoleState(player, oldRole);
        AbilityPlayerComponent.KEY.get(player).reset();
        clearAllItemCooldowns(player);
        resetInventory(player, oldRole);

        gameWorld.addRole(player, newRole);

        // initializeShop 按当前职业构建条目，必须在设置新职业之后；杀手开局金币由 RoleAssigned 写入
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        shop.reset();
        shop.initializeShop(ShopUtils.getShopEntriesForPlayer(player));

        if (revolverCooldown > 0) {
            player.getItemCooldownManager().set(WatheItems.REVOLVER, revolverCooldown);
        }
        if (knifeCooldown > 0) {
            player.getItemCooldownManager().set(WatheItems.KNIFE, knifeCooldown);
        }

        RoleAssigned.EVENT.invoker().assignRole(player, newRole);
        gameWorld.sync();
        player.currentScreenHandler.sendContentUpdates();

        recordRoleAssigned(world, player, newRole);

        int totalPlayers = gameWorld.getAllPlayers().size();
        int killerCount = gameWorld.getAllKillerTeamPlayers().size();
        ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                newRole.identifier().toString(), killerCount, totalPlayers - killerCount));
    }

    /**
     * 从玩家所在阵营中随机抽一个可转入的职业：遵守本局禁用开关与出场条件，
     * 排除孟婆、带枪/义警槽位职业、列车长，以及玩家当前的职业（池里只剩它时除外）。
     * 可与场上其他玩家的职业重复。
     */
    @Nullable
    public static Role pickRandomRole(ServerPlayerEntity player, Faction faction) {
        ServerWorld world = player.getServerWorld();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        Role currentRole = gameWorld.getRole(player);
        RoleSelectionContext context = createSelectionContext(world, gameWorld);

        List<Role> pool = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() != faction
                    || WatheRoles.SPECIAL_ROLES.contains(role)
                    || isExcludedTransformTarget(role)
                    || !gameWorld.isRoleEnabled(role)
                    || !role.shouldAppear(context)) {
                continue;
            }
            pool.add(role);
        }
        if (pool.size() > 1) {
            pool.remove(currentRole);
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(world.getRandom().nextInt(pool.size()));
    }

    /** 孟婆汤/渡己都不能转入的职业：孟婆自身、带枪或义警槽位的警类职业、保底的列车长。 */
    public static boolean isExcludedTransformTarget(Role role) {
        return role == Noellesroles.MENGPO
                || role == Noellesroles.CONDUCTOR
                || Noellesroles.isVigilanteSlotRole(role);
    }

    private static RoleSelectionContext createSelectionContext(ServerWorld world, GameWorldComponent gameWorld) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (UUID uuid : gameWorld.getAllPlayers()) {
            ServerPlayerEntity participant = world.getServer().getPlayerManager().getPlayer(uuid);
            if (participant != null) {
                players.add(participant);
            }
        }
        ScoreboardRoleSelectorComponent selector = ScoreboardRoleSelectorComponent.KEY.get(world.getScoreboard());
        return ((ScoreboardRoleSelectorComponentAccessor) selector)
                .noellesroles$invokeCreateSelectionContext(world, gameWorld, players);
    }

    /**
     * 只清旧职业「自带」的组件状态。同一组件里若混有别人施加的状态（如猎人骨折、噬梦者标记、炸弹），
     * 则不动该组件；接骨师的被动加成由其组件按当前职业自行撤销。
     */
    private static void clearOwnRoleState(ServerPlayerEntity player, @Nullable Role oldRole) {
        if (oldRole == null) {
            return;
        }
        if (oldRole.equals(Noellesroles.BARTENDER)) {
            // 发光是喝下鸡尾酒者身上的状态，只清酒保自己的品酒进度
            BartenderPlayerComponent bartender = BartenderPlayerComponent.KEY.get(player);
            bartender.drinkTasksCompleted = 0;
            bartender.sync();
        } else if (oldRole.equals(Noellesroles.NOISEMAKER)) {
            NoisemakerPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.VOODOO)) {
            VoodooPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.RECALLER)) {
            RecallerPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.REPORTER)) {
            ReporterPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.ATTENDANT)) {
            AttendantPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.BODYGUARD)) {
            BodyguardPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.SURVIVAL_MASTER)) {
            SurvivalMasterPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.ENGINEER)) {
            EngineerPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.SAINT)) {
            SaintPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.BAOSANG)) {
            BaosangPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.JUNKER)) {
            JunkerPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.ECONOMIST)) {
            EconomistPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.MENGPO)) {
            MengPoPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.MORPHLING)) {
            MorphlingPlayerComponent.KEY.get(player).reset();
            MorphlingScramblePlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.ASSASSIN)) {
            AssassinPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.SERIAL_KILLER)) {
            SerialKillerPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.SILENCER)) {
            SilencerPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.COMMANDER)) {
            CommanderPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.BREACHER)) {
            BreacherPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.BANDIT)) {
            BanditPlayerComponent.KEY.get(player).reset();
        } else if (oldRole.equals(Noellesroles.HUNTER)) {
            // 猎人组件里的骨折是别人施加的状态，只收回自己放下的捕兽夹
            HunterPlayerComponent hunter = HunterPlayerComponent.KEY.get(player);
            for (UUID trapUuid : hunter.takeOwnedTraps()) {
                Entity trap = player.getServerWorld().getEntity(trapUuid);
                if (trap != null) {
                    trap.discard();
                }
            }
            hunter.sync();
        }

        // 杀手阵营的疯魔是自己买来的状态；连环杀手的技能疯魔已在上面的 reset 中归还
        if (oldRole.getFaction() == Faction.KILLER) {
            PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
            if (psycho.getPsychoTicks() > 0) {
                psycho.stopPsycho(true);
            }
        }
    }

    private static int getRemainingCooldownTicks(ServerPlayerEntity player, Item item) {
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
        Object entry = accessor.getEntries().get(item);
        if (entry == null) {
            return 0;
        }
        return Math.max(0, ((ItemCooldownEntryAccessor) entry).getEndTick() - accessor.getTick());
    }

    private static void clearAllItemCooldowns(ServerPlayerEntity player) {
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
        for (var item : new HashSet<>(accessor.getEntries().keySet())) {
            player.getItemCooldownManager().remove(item);
        }
    }

    /**
     * 清空物品栏，房门钥匙放回原槽位。乘客身上的枪不随物品栏消失，而是掉在脚下且不记丢弃者，
     * 能捡枪的人（包括他自己，若新职业允许）都能捡起；杀手阵营的枪随物品栏一起清除。
     */
    private static void resetInventory(ServerPlayerEntity player, @Nullable Role oldRole) {
        boolean dropGuns = oldRole != null && oldRole.getFaction() == Faction.CIVILIAN;
        PlayerInventory inventory = player.getInventory();
        Map<Integer, ItemStack> keys = new HashMap<>();
        List<ItemStack> guns = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.isOf(WatheItems.KEY)) {
                keys.put(slot, stack.copy());
            } else if (dropGuns && stack.isIn(WatheItemTags.GUNS)) {
                guns.add(stack.copy());
            }
        }
        ItemStack cursor = player.currentScreenHandler.getCursorStack();
        if (!cursor.isEmpty()) {
            if (dropGuns && cursor.isIn(WatheItemTags.GUNS)) {
                guns.add(cursor.copy());
            }
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        }

        inventory.clear();
        keys.forEach(inventory::setStack);
        for (ItemStack gun : guns) {
            player.dropItem(gun, false, false);
        }
        inventory.markDirty();
    }

    private static void recordRoleAssigned(ServerWorld world, ServerPlayerEntity player, Role role) {
        if (!GameRecordManager.hasActiveMatch()) {
            return;
        }
        NbtCompound playerInfo = new NbtCompound();
        playerInfo.putUuid("uuid", player.getUuid());
        playerInfo.putString("name", player.getName().getString());
        playerInfo.putString("role", role.identifier().toString());
        GameRecordManager.event(GameRecordTypes.ROLE_ASSIGNED)
                .world(world)
                .actor(player)
                .putNbt("player", playerInfo)
                .record();
    }
}

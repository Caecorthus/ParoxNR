package org.agmas.noellesroles.puppeteer;

import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 傀儡师的全部服务端状态迁移。所有进入/退出操纵的路径都经过这里，保证物品栏、位置与伪装不会错乱：
 * <ul>
 *   <li>傀儡术：把未化白骨的尸体收为库存傀儡</li>
 *   <li>操纵：傀儡师本人移动到傀儡位置并换上傀儡物品栏，原地留下本体替身</li>
 *   <li>切回 / 回收：傀儡留在原地静止，或收回库存；傀儡师回到替身位置</li>
 *   <li>傀儡倒地：变回尸体（不可再转化），扣金币，满足条件时傀儡师连带死亡</li>
 * </ul>
 */
public final class PuppeteerControl {
    public static final int PUPPETRY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int SWITCH_COOLDOWN_TICKS = GameConstants.getInTicks(0, 3);
    /** 尸体存在满 6000 tick（与摆渡人、乘务员口径一致）即视为化为白骨 */
    public static final int SKELETON_AGE = GameConstants.TIME_TO_DECOMPOSITION + GameConstants.DECOMPOSING_TIME;

    /** 打在傀儡形态上即视为「受击」的死因：傀儡倒地，傀儡师本人不受影响（炸弹按安放形态另行结算） */
    private static final Set<Identifier> PHYSICAL_DEATH_REASONS = Set.of(
            GameConstants.DeathReasons.KNIFE,
            GameConstants.DeathReasons.GUN,
            GameConstants.DeathReasons.BAT,
            GameConstants.DeathReasons.GRENADE,
            GameConstants.DeathReasons.FELL_OUT_OF_TRAIN,
            Noellesroles.DEATH_REASON_THROWING_AXE
    );

    private PuppeteerControl() {}

    // ===================== 查询 =====================

    public static boolean isPuppeteer(@Nullable PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.PUPPETEER);
    }

    private static boolean canAct(ServerPlayerEntity player) {
        return isPuppeteer(player)
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player);
    }

    /**
     * 炸弹倒计时（物品已发到手上）或狂暴期间不能切换形态：这两者只会从「当前」物品栏收回道具，
     * 切换会把炸弹 / 球棒藏进另一份物品栏里。两者都会提示持有者，锁定不泄露信息。
     */
    private static boolean isFormLocked(ServerPlayerEntity player) {
        BomberPlayerComponent bomber = BomberPlayerComponent.KEY.get(player);
        return (bomber.hasBomb() && bomber.isBeeping())
                || PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0;
    }

    /** 服务端判定：尸体可否被傀儡术转化（未化白骨、不是傀儡倒下后留下的尸体）。 */
    public static boolean isConvertibleBody(ServerWorld world, PlayerBodyEntity body) {
        return body.age < SKELETON_AGE && !PuppetWorldComponent.get(world).isSpentBody(body.getUuid());
    }

    @Nullable
    public static PuppetEntity findStandIn(ServerWorld world, @Nullable UUID entityUuid) {
        if (entityUuid == null) {
            return null;
        }
        return world.getEntity(entityUuid) instanceof PuppetEntity puppet && !puppet.isRemoved() ? puppet : null;
    }

    /**
     * 「身体位置」代表：正在操纵傀儡的傀儡师返回其本体替身，其余返回玩家本身。
     * 供病原体感染距离等「作用于本体」的判定使用。
     */
    public static Entity getBodyAnchor(PlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld world) {
            PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(player);
            if (comp.isControlling()) {
                PuppetEntity shell = findStandIn(world, comp.getShellUuid());
                if (shell != null) {
                    return shell;
                }
            }
        }
        return player;
    }

    /**
     * 把技能面板里选中的 UUID 解析为场内的「位置代表」实体：
     * 傀儡（生前玩家 UUID）→ 闲置傀儡实体 / 正在操纵它的傀儡师；
     * 正在操纵傀儡的傀儡师 → 其本体替身；其他玩家 → 玩家本身。
     */
    @Nullable
    public static Entity resolvePositionalTarget(ServerWorld world, UUID uuid) {
        PuppetRecord record = PuppetWorldComponent.get(world).get(uuid);
        if (record != null) {
            return switch (record.getState()) {
                case IDLE -> findStandIn(world, record.getEntityUuid());
                case CONTROLLED -> world.getPlayerByUuid(record.getOwnerUuid());
                case STORED -> null;
            };
        }
        PlayerEntity player = world.getPlayerByUuid(uuid);
        return player != null ? getBodyAnchor(player) : null;
    }

    // ===================== 傀儡术 =====================

    public static void tryConvert(ServerPlayerEntity player, UUID bodyUuid) {
        if (!canAct(player)) {
            return;
        }
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(player);
        if (comp.isControlling() || comp.getPuppetryCooldown() > 0) {
            return;
        }
        if (!comp.hasQuotaLeft()) {
            player.sendMessage(Text.translatable("tip.puppeteer.no_quota"), true);
            return;
        }
        ServerWorld world = player.getServerWorld();
        EntityHitResult hit = BodyTargetHelper.raycastBody(player, BodyTargetHelper.SKILL_RANGE,
                body -> body.getUuid().equals(bodyUuid));
        if (hit == null || !(hit.getEntity() instanceof PlayerBodyEntity body)) {
            return;
        }
        PuppetWorldComponent puppets = PuppetWorldComponent.get(world);
        if (!isConvertibleBody(world, body)) {
            player.sendMessage(Text.translatable("tip.puppeteer.body_unusable"), true);
            return;
        }
        UUID source = body.getPlayerUuid();
        if (source == null || source.equals(player.getUuid()) || puppets.get(source) != null) {
            player.sendMessage(Text.translatable("tip.puppeteer.body_unusable"), true);
            return;
        }

        PuppetRecord record = new PuppetRecord(source, player.getUuid(), body.getDeathReason(), body.getDeathGameTime());
        puppets.add(record);
        Vec3d bodyPos = body.getPos();
        UUID bodyEntityUuid = body.getUuid();
        body.discard();

        comp.incrementMadeCount();
        comp.setPuppetryCooldown(PUPPETRY_COOLDOWN_TICKS);
        comp.sync();
        puppets.sync();

        world.spawnParticles(ParticleTypes.SOUL, bodyPos.x, bodyPos.y + 0.4, bodyPos.z, 16, 0.3, 0.2, 0.3, 0.02);
        world.playSound(null, BlockPos.ofFloored(bodyPos), SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM,
                SoundCategory.PLAYERS, 0.6F, 0.5F);
        player.sendMessage(Text.translatable("tip.puppeteer.converted",
                comp.getMadeCount(), comp.getMaxPuppets()), true);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "convert");
        extra.putUuid("body_uuid", bodyEntityUuid);
        extra.putUuid("target", source);
        GameRecordManager.putPos(extra, "body_pos", bodyPos);
        GameRecordManager.recordSkillUse(player, Noellesroles.PUPPETEER_ID, null, extra);
    }

    // ===================== 操纵 =====================

    /**
     * 从本体接管一具傀儡。source 为空时取第一具可用傀儡。
     */
    public static void tryControl(ServerPlayerEntity player, @Nullable UUID source) {
        if (!canAct(player)) {
            return;
        }
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(player);
        if (comp.isControlling() || comp.getSwitchCooldown() > 0) {
            return;
        }
        if (isFormLocked(player)) {
            player.sendMessage(Text.translatable("tip.puppeteer.form_locked"), true);
            return;
        }
        ServerWorld world = player.getServerWorld();
        PuppetWorldComponent puppets = PuppetWorldComponent.get(world);
        PuppetRecord record = source != null ? puppets.get(source) : firstAvailable(puppets, player.getUuid());
        if (record == null || !record.getOwnerUuid().equals(player.getUuid())
                || record.getState() == PuppetRecord.State.CONTROLLED) {
            return;
        }

        PuppetEntity idleEntity = null;
        if (record.getState() == PuppetRecord.State.IDLE) {
            idleEntity = findStandIn(world, record.getEntityUuid());
            if (idleEntity == null) {
                // 实体已不在世界里（被清理等）：视作收回了库存
                record.setState(PuppetRecord.State.STORED);
                record.setEntityUuid(null);
            }
        }

        Vec3d targetPos;
        float targetYaw;
        float targetPitch;
        if (idleEntity != null) {
            targetPos = idleEntity.getPos();
            targetYaw = idleEntity.getHeadYaw();
            targetPitch = 0.0F;
        } else {
            targetPos = findDeploySpot(world, player);
            if (targetPos == null) {
                player.sendMessage(Text.translatable("tip.puppeteer.no_space"), true);
                return;
            }
            targetYaw = player.getYaw();
            targetPitch = player.getPitch();
        }

        prepareForMove(player);
        Vec3d bodyPos = player.getPos();
        PuppetEntity shell = spawnStandIn(world, PuppetEntity.Kind.SHELL, player.getUuid(), player.getUuid(),
                bodyPos, player.getYaw(), player.getHeadYaw());
        if (shell == null) {
            return;
        }

        comp.beginControl(record.getSourceUuid(), shell.getUuid(), bodyPos, player.getYaw(), player.getPitch());
        record.giveInventoryTo(player.getInventory());
        record.clearInventory();
        if (idleEntity != null) {
            idleEntity.discard();
        }
        record.setState(PuppetRecord.State.CONTROLLED);
        record.setEntityUuid(null);

        teleport(player, targetPos, targetYaw, targetPitch);
        syncInventory(player);
        comp.setSwitchCooldown(SWITCH_COOLDOWN_TICKS);
        comp.sync();
        puppets.sync();

        world.spawnParticles(ParticleTypes.SOUL, targetPos.x, targetPos.y + 1.0, targetPos.z, 10, 0.25, 0.5, 0.25, 0.01);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "control");
        extra.putUuid("target", record.getSourceUuid());
        GameRecordManager.recordSkillUse(player, Noellesroles.PUPPETEER_ID, null, extra);
    }

    /** 玩家主动切回本体（傀儡留在原地静止）或回收（傀儡收回库存）。 */
    public static void tryReturn(ServerPlayerEntity player, boolean recall) {
        if (!canAct(player)) {
            return;
        }
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(player);
        if (!comp.isControlling() || comp.getSwitchCooldown() > 0) {
            return;
        }
        if (isFormLocked(player)) {
            player.sendMessage(Text.translatable("tip.puppeteer.form_locked"), true);
            return;
        }
        UUID source = comp.getControlledSource();
        if (!returnToBody(player, recall)) {
            return;
        }
        comp.setSwitchCooldown(SWITCH_COOLDOWN_TICKS);
        comp.sync();

        NbtCompound extra = new NbtCompound();
        extra.putString("action", recall ? "recall" : "release");
        if (source != null) {
            extra.putUuid("target", source);
        }
        GameRecordManager.recordSkillUse(player, Noellesroles.PUPPETEER_ID, null, extra);
    }

    /**
     * 结束操纵并回到本体，所有退出路径（主动、被迫）共用。
     * @param recall true 时傀儡收回库存，否则静止留在当前位置
     * @return 调用前是否处于操纵中
     */
    public static boolean returnToBody(ServerPlayerEntity player, boolean recall) {
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(player);
        if (!comp.isControlling()) {
            return false;
        }
        ServerWorld world = player.getServerWorld();
        PuppetWorldComponent puppets = PuppetWorldComponent.get(world);
        PuppetRecord record = puppets.get(comp.getControlledSource());
        Vec3d puppetPos = player.getPos();
        float puppetYaw = player.getYaw();
        float puppetHeadYaw = player.getHeadYaw();

        prepareForMove(player);
        detachFormBoundItems(player);
        if (record != null) {
            record.takeInventoryFrom(player.getInventory());
            PuppetEntity idle = recall ? null : spawnStandIn(world, PuppetEntity.Kind.PUPPET, player.getUuid(),
                    record.getSourceUuid(), puppetPos, puppetYaw, puppetHeadYaw);
            record.setState(idle != null ? PuppetRecord.State.IDLE : PuppetRecord.State.STORED);
            record.setEntityUuid(idle != null ? idle.getUuid() : null);
        } else {
            player.getInventory().clear();
        }
        restoreBody(player, comp);
        puppets.sync();
        comp.sync();
        return true;
    }

    /** 把正在操纵的玩家放回本体位置并换回本体物品栏（玩家物品栏须已清空或已存走）。 */
    private static void restoreBody(ServerPlayerEntity player, PuppeteerPlayerComponent comp) {
        ServerWorld world = player.getServerWorld();
        PuppetEntity shell = findStandIn(world, comp.getShellUuid());
        Vec3d bodyPos = shell != null ? shell.getPos() : comp.getBodyPos();
        float bodyYaw = comp.getBodyYaw();
        float bodyPitch = comp.getBodyPitch();
        if (shell != null) {
            shell.discard();
        }
        detachFormBoundItems(player);
        player.getInventory().clear();
        comp.endControl();
        // 被迫回到本体时炸弹仍在倒计时（安放在本体或别的傀儡上）：炸弹物品跟着人走
        BomberPlayerComponent bomber = BomberPlayerComponent.KEY.get(player);
        if (bomber.hasBomb() && bomber.isBeeping() && player.getInventory().count(ModItems.TIMED_BOMB) == 0) {
            player.giveItemStack(ModItems.TIMED_BOMB.getDefaultStack());
        }
        teleport(player, bodyPos, bodyYaw, bodyPitch);
        syncInventory(player);
    }

    /**
     * 离开傀儡形态前收走只能从「当前」物品栏回收的道具：结束狂暴（球棒随之收回；本体无法购买，
     * 狂暴必然来自傀儡形态），取出倒计时炸弹物品（之后由 restoreBody 在本体上补发）。
     */
    private static void detachFormBoundItems(ServerPlayerEntity player) {
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        if (psycho.getPsychoTicks() > 0) {
            psycho.stopPsycho();
            psycho.sync();
        }
        BomberPlayerComponent bomber = BomberPlayerComponent.KEY.get(player);
        if (bomber.hasBomb()) {
            bomber.removeBombItemOnly();
        }
    }

    /** 操纵期间每 tick 自检：替身或傀儡记录丢失时强制回到本体。 */
    static void tickController(ServerPlayerEntity player, PuppeteerPlayerComponent comp) {
        ServerWorld world = player.getServerWorld();
        PuppetRecord record = PuppetWorldComponent.get(world).get(comp.getControlledSource());
        if (record == null || record.getState() != PuppetRecord.State.CONTROLLED) {
            prepareForMove(player);
            player.getInventory().clear();
            restoreBody(player, comp);
            comp.sync();
            return;
        }
        if (findStandIn(world, comp.getShellUuid()) == null) {
            returnToBody(player, false);
        }
    }

    // ===================== 倒地与死亡 =====================

    /**
     * 傀儡倒地：变回尸体（保留生前死因、不可再转化）、扣金币；
     * 巫毒导致的倒地，或名额已用完且再无存活傀儡时，傀儡师以相同死因连带死亡。
     */
    public static void collapsePuppet(ServerWorld world, PuppetRecord record, @Nullable ServerPlayerEntity attacker,
                                      Identifier reason, boolean spawnBody, boolean byVoodoo) {
        PuppetWorldComponent puppets = PuppetWorldComponent.get(world);
        if (puppets.get(record.getSourceUuid()) != record) {
            return;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(record.getOwnerUuid());
        if (owner != null && record.getSourceUuid().equals(PuppeteerPlayerComponent.get(owner).getBombedSource())) {
            // 炸弹安放在这具傀儡上：随傀儡倒地一起失效（须在 restoreBody 之前，免得补发炸弹物品）
            PuppeteerPlayerComponent.get(owner).clearBombedSource();
            BomberPlayerComponent.KEY.get(owner).reset();
        }
        Vec3d pos = null;
        float yaw = 0.0F;
        switch (record.getState()) {
            case CONTROLLED -> {
                if (owner != null) {
                    pos = owner.getPos();
                    yaw = owner.getHeadYaw();
                    PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(owner);
                    if (record.getSourceUuid().equals(comp.getControlledSource())) {
                        // 傀儡身上的物品随傀儡倒地一起消失
                        prepareForMove(owner);
                        owner.getInventory().clear();
                        restoreBody(owner, comp);
                        comp.setSwitchCooldown(SWITCH_COOLDOWN_TICKS);
                    }
                }
            }
            case IDLE -> {
                PuppetEntity entity = findStandIn(world, record.getEntityUuid());
                if (entity != null) {
                    pos = entity.getPos();
                    yaw = entity.getHeadYaw();
                    entity.discard();
                }
            }
            case STORED -> {
                if (owner != null) {
                    pos = owner.getPos();
                    yaw = owner.getHeadYaw();
                }
            }
        }
        puppets.remove(record.getSourceUuid());
        if (spawnBody && pos != null) {
            PlayerBodyEntity body = spawnCorpse(world, record, pos, yaw);
            if (body != null) {
                puppets.markSpentBody(body.getUuid());
            }
        }
        puppets.sync();

        if (owner != null) {
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(owner);
            int penalty = PuppeteerRules.coinPenalty(shop.getBalance());
            if (penalty > 0) {
                shop.addToBalance(-penalty);
            }
            owner.sendMessage(Text.translatable("tip.puppeteer.puppet_destroyed"), true);
            PuppeteerPlayerComponent.get(owner).sync();

            NbtCompound extra = new NbtCompound();
            extra.putString("action", "destroyed");
            extra.putUuid("target", record.getSourceUuid());
            extra.putString("death_reason", reason.toString());
            if (attacker != null) {
                extra.putUuid("attacker", attacker.getUuid());
            }
            GameRecordManager.recordSkillUse(owner, Noellesroles.PUPPETEER_ID, null, extra);
        }

        if (owner == null || !GameFunctions.isPlayerPlayingAndAlive(owner)) {
            return;
        }
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(owner);
        if (!PuppeteerRules.shouldChainDie(byVoodoo, comp.getMadeCount(), comp.getMaxPuppets(),
                puppets.getOwnedBy(owner.getUuid()).size())) {
            return;
        }
        // 下一 tick 再结算连带死亡，避免在另一次 killPlayer 的 BEFORE 回调里重入
        ServerPlayerEntity chainOwner = owner;
        Scheduler.schedule(() -> {
            if (!GameFunctions.isPlayerPlayingAndAlive(chainOwner)) {
                return;
            }
            NbtCompound chainExtra = new NbtCompound();
            chainExtra.putString("action", "chain_death");
            chainExtra.putString("death_reason", reason.toString());
            GameRecordManager.recordSkillUse(chainOwner, Noellesroles.PUPPETEER_ID, attacker, chainExtra);
            GameFunctions.killPlayer(chainOwner, true, attacker, reason, true);
        }, 1);
    }

    /** 傀儡师死亡（任何原因）后：所有剩余傀儡原地倒地，不再扣金币也不再连带。 */
    public static void collapseAllOwnedBy(ServerWorld world, ServerPlayerEntity owner) {
        PuppetWorldComponent puppets = PuppetWorldComponent.get(world);
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(owner);
        if (comp.isControlling()) {
            returnToBody(owner, false);
        }
        List<PuppetRecord> owned = new ArrayList<>(puppets.getOwnedBy(owner.getUuid()));
        if (owned.isEmpty()) {
            return;
        }
        for (PuppetRecord record : owned) {
            Vec3d pos = owner.getPos();
            float yaw = owner.getHeadYaw();
            if (record.getState() == PuppetRecord.State.IDLE) {
                PuppetEntity entity = findStandIn(world, record.getEntityUuid());
                if (entity != null) {
                    pos = entity.getPos();
                    yaw = entity.getHeadYaw();
                    entity.discard();
                }
            }
            puppets.remove(record.getSourceUuid());
            PlayerBodyEntity body = spawnCorpse(world, record, pos, yaw);
            if (body != null) {
                puppets.markSpentBody(body.getUuid());
            }
        }
        puppets.sync();
    }

    /**
     * KillPlayer.BEFORE：正在操纵傀儡的玩家被杀。
     * 打在傀儡身上的物理伤害 → 傀儡倒地，取消这次击杀；
     * 针对傀儡师本人的死亡（刺客、巫毒、精神崩溃等）→ 先回到本体，再按正常流程结算。
     */
    @Nullable
    public static KillPlayer.KillResult handleBeforeKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier reason) {
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(victim);
        ServerWorld world = victim.getServerWorld();
        if (Noellesroles.DEATH_REASON_BOMB.equals(reason) && (comp.getBombedSource() != null || comp.isControlling())) {
            return handleBombKill(world, victim, killer, comp);
        }
        if (!comp.isControlling()) {
            return null;
        }
        PuppetRecord record = PuppetWorldComponent.get(world).get(comp.getControlledSource());
        if (record != null && PHYSICAL_DEATH_REASONS.contains(reason)) {
            collapsePuppet(world, record, killer, reason,
                    !GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(reason), false);
            return KillPlayer.KillResult.cancel();
        }
        returnToBody(victim, false);
        return null;
    }

    /**
     * 炸弹按安放时的形态结算，与爆炸时傀儡师在哪个形态无关：
     * 安放在某具傀儡上 → 只炸倒那具傀儡；安放在本体上 → 傀儡师回到本体后被炸死。
     */
    private static KillPlayer.KillResult handleBombKill(ServerWorld world, ServerPlayerEntity victim,
                                                       @Nullable ServerPlayerEntity killer, PuppeteerPlayerComponent comp) {
        UUID bombedSource = comp.getBombedSource();
        comp.clearBombedSource();
        // 先清掉炸弹状态：否则下面回到本体时 restoreBody 会把炸弹物品补发给本体
        BomberPlayerComponent.KEY.get(victim).reset();
        if (bombedSource == null) {
            returnToBody(victim, false);
            return null;
        }
        PuppetRecord bombed = PuppetWorldComponent.get(world).get(bombedSource);
        if (bombed != null && bombed.getOwnerUuid().equals(victim.getUuid())) {
            collapsePuppet(world, bombed, killer, Noellesroles.DEATH_REASON_BOMB, true, false);
        }
        return KillPlayer.KillResult.cancel();
    }

    /** 傀儡形态被下毒：傀儡立即倒地，毒不会留给傀儡师。返回 true 表示已拦截。 */
    public static boolean handlePoisoned(ServerPlayerEntity target, @Nullable UUID poisonerUuid) {
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.get(target);
        if (!comp.isControlling()) {
            return false;
        }
        ServerWorld world = target.getServerWorld();
        PuppetRecord record = PuppetWorldComponent.get(world).get(comp.getControlledSource());
        if (record == null) {
            return false;
        }
        ServerPlayerEntity poisoner = poisonerUuid != null ? world.getServer().getPlayerManager().getPlayer(poisonerUuid) : null;
        collapsePuppet(world, record, poisoner, GameConstants.DeathReasons.POISON, true, false);
        return true;
    }

    /**
     * 武器命中傀儡实体：闲置傀儡倒地；本体替身被命中则傀儡师被迫回到本体并按该死因被击杀。
     */
    public static void onEntityHit(PuppetEntity entity, @Nullable ServerPlayerEntity attacker, Identifier reason) {
        if (!(entity.getWorld() instanceof ServerWorld world) || entity.isRemoved()) {
            return;
        }
        if (attacker != null && SaintPlayerComponent.KEY.get(attacker).isKarmaLocked()) {
            return;
        }
        // 傀儡师打不到自己的傀儡和本体替身
        if (attacker != null && attacker.getUuid().equals(entity.getOwnerUuid())) {
            return;
        }
        if (entity.isShell()) {
            ServerPlayerEntity owner = returnShellOwnerToBody(entity);
            if (owner != null) {
                GameFunctions.killPlayer(owner, true, attacker, reason);
            }
            return;
        }
        PuppetRecord record = PuppetWorldComponent.get(world).findByEntity(entity.getUuid());
        if (record == null) {
            entity.discard();
            return;
        }
        collapsePuppet(world, record, attacker, reason, true, false);
    }

    /**
     * 本体替身受到作用（被击中、被扎针、吸入毒气）：傀儡师被迫回到本体，由调用方再对其本人结算。
     * @return 回到本体后的傀儡师；替身已失去归属时返回 null（替身随即被移除）
     */
    @Nullable
    public static ServerPlayerEntity returnShellOwnerToBody(PuppetEntity shell) {
        if (!(shell.getWorld() instanceof ServerWorld world) || !shell.isShell() || shell.isRemoved()) {
            return null;
        }
        ServerPlayerEntity owner = shell.getOwnerUuid() != null
                ? world.getServer().getPlayerManager().getPlayer(shell.getOwnerUuid()) : null;
        if (owner == null || !shell.getUuid().equals(PuppeteerPlayerComponent.get(owner).getShellUuid())) {
            shell.discard();
            return null;
        }
        returnToBody(owner, false);
        return owner;
    }

    /** 巫毒连带：巫毒师死亡 10 秒后，被绑定的傀儡倒地，傀儡师随之死亡。 */
    public static void applyVoodooCurse(ServerWorld world, UUID boundSource, ServerPlayerEntity voodooMaster) {
        PuppetRecord record = PuppetWorldComponent.get(world).get(boundSource);
        if (record == null) {
            return;
        }
        collapsePuppet(world, record, voodooMaster, Noellesroles.DEATH_REASON_VOODOO, true, true);
    }

    // ===================== 实体维护 =====================

    /** 每秒自检一次：失去归属的实体直接移除；闲置傀儡掉出列车区域即倒地（不留尸体）。 */
    public static void tickEntity(ServerWorld world, PuppetEntity entity) {
        if (entity.age % 20 != 0) {
            return;
        }
        if (!GameWorldComponent.KEY.get(world).isRunning()) {
            entity.discard();
            return;
        }
        // 保持所在区块常驻：替身/闲置傀儡附近没有玩家时也不会被卸载
        world.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(entity.getBlockPos()), 2, entity.getBlockPos());
        if (entity.isShell()) {
            ServerPlayerEntity owner = entity.getOwnerUuid() != null
                    ? world.getServer().getPlayerManager().getPlayer(entity.getOwnerUuid()) : null;
            if (owner == null || !entity.getUuid().equals(PuppeteerPlayerComponent.get(owner).getShellUuid())) {
                entity.discard();
            }
            return;
        }
        PuppetRecord record = PuppetWorldComponent.get(world).findByEntity(entity.getUuid());
        if (record == null || record.getState() != PuppetRecord.State.IDLE) {
            entity.discard();
            return;
        }
        Box playArea = MapVariablesWorldComponent.KEY.get(world).getPlayArea();
        if (playArea != null && entity.getY() < playArea.minY) {
            collapsePuppet(world, record, null, GameConstants.DeathReasons.FELL_OUT_OF_TRAIN, false, false);
        }
    }

    /** 对局结束：清掉场上所有傀儡实体与替身。 */
    public static void discardAllEntities(ServerWorld world) {
        for (PuppetEntity entity : world.getEntitiesByType(NoellesRolesEntities.PUPPET_ENTITY, e -> true)) {
            entity.discard();
        }
    }

    // ===================== 工具 =====================

    @Nullable
    private static PuppetRecord firstAvailable(PuppetWorldComponent puppets, UUID owner) {
        for (PuppetRecord record : puppets.getOwnedBy(owner)) {
            if (record.getState() != PuppetRecord.State.CONTROLLED) {
                return record;
            }
        }
        return null;
    }

    @Nullable
    private static PuppetEntity spawnStandIn(ServerWorld world, PuppetEntity.Kind kind, UUID owner, UUID skin,
                                             Vec3d pos, float yaw, float headYaw) {
        PuppetEntity entity = NoellesRolesEntities.PUPPET_ENTITY.create(world);
        if (entity == null) {
            return null;
        }
        entity.setKind(kind);
        entity.setOwnerUuid(owner);
        entity.setSkinUuid(skin);
        entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0F);
        entity.setHeadYaw(headYaw);
        entity.setBodyYaw(yaw);
        world.spawnEntity(entity);
        return entity;
    }

    @Nullable
    private static PlayerBodyEntity spawnCorpse(ServerWorld world, PuppetRecord record, Vec3d pos, float yaw) {
        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(world);
        if (body == null) {
            return null;
        }
        body.setPlayerUuid(record.getSourceUuid());
        body.setDeathReason(record.getDeathReason());
        body.setDeathGameTime(record.getDeathGameTime());
        body.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0F);
        body.setYaw(yaw);
        body.setHeadYaw(yaw);
        world.spawnEntity(body);
        world.playSound(null, BlockPos.ofFloored(pos), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.8F, 0.6F);
        return body;
    }

    /** 召唤库存傀儡的位置：本体前/右/左/后方 1 格，要求无遮挡、脚下有地面。 */
    @Nullable
    private static Vec3d findDeploySpot(ServerWorld world, ServerPlayerEntity player) {
        float yawRad = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        Vec3d forward = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        Vec3d[] offsets = {forward, right, right.multiply(-1.0), forward.multiply(-1.0)};
        Vec3d origin = player.getPos();
        Vec3d eye = player.getEyePos();
        for (Vec3d offset : offsets) {
            Vec3d candidate = origin.add(offset);
            Box box = player.getBoundingBox().offset(offset);
            if (!world.isSpaceEmpty(player, box)) {
                continue;
            }
            Box ground = new Box(box.minX, box.minY - 0.6, box.minZ, box.maxX, box.minY, box.maxZ);
            if (world.isSpaceEmpty(player, ground)) {
                continue;
            }
            Vec3d candidateEye = candidate.add(0.0, player.getStandingEyeHeight(), 0.0);
            HitResult blockHit = world.raycast(new RaycastContext(eye, candidateEye,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (blockHit.getType() != HitResult.Type.MISS) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static void prepareForMove(ServerPlayerEntity player) {
        player.closeHandledScreen();
        if (player.isSleeping()) {
            player.wakeUp();
        }
        if (player.hasVehicle()) {
            player.stopRiding();
        }
    }

    private static void teleport(ServerPlayerEntity player, Vec3d pos, float yaw, float pitch) {
        player.teleport(player.getServerWorld(), pos.x, pos.y, pos.z, EnumSet.noneOf(PositionFlag.class), yaw, pitch);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
    }

    private static void syncInventory(ServerPlayerEntity player) {
        player.playerScreenHandler.sendContentUpdates();
        player.currentScreenHandler.sendContentUpdates();
        player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));
    }
}

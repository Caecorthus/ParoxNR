package org.agmas.noellesroles.client.puppeteer;

import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.renderer.PuppetEntityRenderer;
import org.agmas.noellesroles.client.screen.RoleScreenHelper;
import org.agmas.noellesroles.client.screen.RoleTargetMenuScreen;
import org.agmas.noellesroles.client.murdermayhem.FogOfWarClientHelper;
import org.agmas.noellesroles.entity.PuppetEntity;
import org.agmas.noellesroles.packet.PuppeteerActionC2SPacket;
import org.agmas.noellesroles.puppeteer.PuppetRecord;
import org.agmas.noellesroles.puppeteer.PuppetWorldComponent;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import org.agmas.noellesroles.puppeteer.PuppeteerPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 傀儡师的客户端逻辑：实体渲染、皮肤解析、按键、瞄准傀儡、本能透视与本体疾跑锁。
 */
public final class PuppeteerClient {
    /** 傀儡师看自己闲置傀儡的特殊透视色 */
    public static final int OWN_PUPPET_COLOR = 0x7FFFD4;
    /** 傀儡形态下看自己本体替身的特殊透视色 */
    public static final int OWN_SHELL_COLOR = 0xFFFFFF;
    /** 本体形态下透视可转化（未化白骨）尸体的颜色 */
    public static final int CONVERTIBLE_BODY_COLOR = 0xC8C8C8;
    private static final int KILLER_RED = MathHelper.hsvToRgb(0F, 1.0F, 0.6F);
    private static final Text UNKNOWN_NAME = Text.translatable("screen.role_target.unknown_player");

    private PuppeteerClient() {}

    public static void init() {
        EntityRendererRegistry.register(NoellesRolesEntities.PUPPET_ENTITY, PuppetEntityRenderer::new);
        registerInstinctHighlights();
        ClientTickEvents.START_CLIENT_TICK.register(PuppeteerClient::tickSprintLock);
    }

    // ===================== 皮肤与名字 =====================

    /** 解析任意玩家（含已死亡、已离线）的皮肤。 */
    public static SkinTextures getSkinTextures(@Nullable UUID uuid) {
        if (uuid == null) {
            return DefaultSkinHelper.getSkinTextures(new UUID(0L, 0L));
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.world.getPlayerByUuid(uuid) instanceof AbstractClientPlayerEntity clientPlayer) {
            return clientPlayer.getSkinTextures();
        }
        PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (cachedEntry != null) {
            return cachedEntry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(uuid);
    }

    /** 正以傀儡形态行动的玩家所伪装的死者 UUID；不在操纵中返回 null。 */
    @Nullable
    public static UUID getPuppetDisguise(PlayerEntity player) {
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRunning()) {
            return null;
        }
        return PuppeteerPlayerComponent.KEY.get(player).getControlledSource();
    }

    public static Text getName(@Nullable UUID uuid) {
        return uuid == null ? UNKNOWN_NAME : RoleScreenHelper.getPlayerName(uuid, UNKNOWN_NAME);
    }

    // ===================== 状态 =====================

    public static boolean isPuppeteer(@Nullable PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.PUPPETEER);
    }

    /** 可以被接管的傀儡（库存中或闲置在场上）。 */
    public static List<PuppetRecord> getAvailablePuppets(PlayerEntity owner) {
        List<PuppetRecord> result = new ArrayList<>();
        for (PuppetRecord record : PuppetWorldComponent.get(owner.getWorld()).getOwnedBy(owner.getUuid())) {
            if (record.getState() != PuppetRecord.State.CONTROLLED) {
                result.add(record);
            }
        }
        return result;
    }

    public static boolean hasPuppets(PlayerEntity owner) {
        return !PuppetWorldComponent.get(owner.getWorld()).getOwnedBy(owner.getUuid()).isEmpty();
    }

    /**
     * 客户端估算尸体能否被傀儡术转化。尸体 age 不同步，按死亡时刻推算（摆渡人推进过的 age 也会计入）。
     */
    public static boolean isConvertibleBody(PlayerBodyEntity body) {
        if (PuppetWorldComponent.get(body.getWorld()).isSpentBody(body.getUuid())) {
            return false;
        }
        long sinceDeath = body.getWorld().getTime() - body.getDeathGameTime();
        long age = Math.max(body.age, sinceDeath);
        return age < PuppeteerControl.SKELETON_AGE;
    }

    // ===================== 按键 =====================

    /**
     * 技能键：操纵中 → 切回本体；准星对准可转化尸体且傀儡术就绪 → 傀儡术；
     * 否则接管傀儡（只有一具时直接接管，多具时打开选择面板）。
     */
    public static void onAbilityKey(MinecraftClient client, ClientPlayerEntity player, @Nullable PlayerBodyEntity targetBody) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player) || SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return;
        }
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.KEY.get(player);
        if (comp.isControlling()) {
            if (comp.getSwitchCooldown() <= 0) {
                ClientPlayNetworking.send(new PuppeteerActionC2SPacket(PuppeteerActionC2SPacket.Action.RETURN, null));
            }
            return;
        }
        if (targetBody != null && comp.hasQuotaLeft() && comp.getPuppetryCooldown() <= 0 && isConvertibleBody(targetBody)) {
            ClientPlayNetworking.send(new PuppeteerActionC2SPacket(PuppeteerActionC2SPacket.Action.CONVERT, targetBody.getUuid()));
            return;
        }
        if (comp.getSwitchCooldown() > 0) {
            return;
        }
        List<PuppetRecord> available = getAvailablePuppets(player);
        if (available.size() == 1) {
            ClientPlayNetworking.send(new PuppeteerActionC2SPacket(PuppeteerActionC2SPacket.Action.CONTROL,
                    available.getFirst().getSourceUuid()));
        } else if (available.size() > 1) {
            client.setScreen(new RoleTargetMenuScreen(player, RoleTargetMenuScreen.MenuType.PUPPETEER));
        }
    }

    /** 技能键2：操纵中回收傀儡（收回库存并回到本体）。 */
    public static void onAbility2Key(ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player) || SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return;
        }
        PuppeteerPlayerComponent comp = PuppeteerPlayerComponent.KEY.get(player);
        if (comp.isControlling() && comp.getSwitchCooldown() <= 0) {
            ClientPlayNetworking.send(new PuppeteerActionC2SPacket(PuppeteerActionC2SPacket.Action.RECALL, null));
        }
    }

    // ===================== 瞄准 =====================

    /**
     * 刀 / 枪的客户端瞄准：若射线上有比原目标更近的傀儡或本体替身（不含自己的），改为瞄准它。
     */
    public static HitResult preferPuppetTarget(PlayerEntity user, HitResult original, double range) {
        HitResult puppetHit = ProjectileUtil.getCollision(user,
                entity -> entity instanceof PuppetEntity puppet && !user.getUuid().equals(puppet.getOwnerUuid()),
                range);
        if (!(puppetHit instanceof EntityHitResult)) {
            return original;
        }
        Vec3d eyePos = user.getEyePos();
        if (original instanceof EntityHitResult originalEntityHit
                && eyePos.squaredDistanceTo(originalEntityHit.getPos()) <= eyePos.squaredDistanceTo(puppetHit.getPos())) {
            return original;
        }
        return puppetHit;
    }

    /** 准星下的傀儡 / 本体替身（用于显示名字，范围与 Wathe 名牌一致）。 */
    @Nullable
    public static PuppetEntity findCrosshairPuppet(ClientPlayerEntity player) {
        double range = WatheClient.canSeeSpectatorInformation() ? 8.0 : 2.0;
        HitResult hit = ProjectileUtil.getCollision(player,
                entity -> entity instanceof PuppetEntity
                        || (entity instanceof PlayerEntity target && GameFunctions.isPlayerPlayingAndAlive(target)),
                range);
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PuppetEntity puppet ? puppet : null;
    }

    // ===================== 透视 =====================

    private static void registerInstinctHighlights() {
        // 傀儡 / 本体替身：主人看自己的为特殊色，队友杀手看为红色
        GetInstinctHighlight.EVENT.register(entity -> {
            if (!(entity instanceof PuppetEntity puppet)) {
                return null;
            }
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer == null || !WatheClient.isPlayerPlayingAndAlive()
                    || SwallowedPlayerComponent.isPlayerSwallowed(localPlayer)) {
                return null;
            }
            if (localPlayer.getUuid().equals(puppet.getOwnerUuid())) {
                return GetInstinctHighlight.HighlightResult.withKeybind(
                        puppet.isShell() ? OWN_SHELL_COLOR : OWN_PUPPET_COLOR,
                        GetInstinctHighlight.HighlightResult.PRIORITY_HIGH);
            }
            if (GameWorldComponent.KEY.get(localPlayer.getWorld()).canUseKillerFeatures(localPlayer)) {
                return GetInstinctHighlight.HighlightResult.withKeybind(KILLER_RED, GetInstinctHighlight.HighlightResult.PRIORITY_HIGH);
            }
            return null;
        });

        // 傀儡师本体形态：透视不分敌我（统一角色色），并可透视未化白骨的尸体；傀儡形态则沿用原版杀手本能
        GetInstinctHighlight.EVENT.register(entity -> {
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer == null || entity == localPlayer || !WatheClient.isPlayerPlayingAndAlive()
                    || SwallowedPlayerComponent.isPlayerSwallowed(localPlayer)
                    || !isPuppeteer(localPlayer)
                    || PuppeteerPlayerComponent.KEY.get(localPlayer).isControlling()) {
                return null;
            }
            if (entity instanceof PlayerBodyEntity body) {
                return isConvertibleBody(body)
                        ? GetInstinctHighlight.HighlightResult.withKeybind(CONVERTIBLE_BODY_COLOR, GetInstinctHighlight.HighlightResult.PRIORITY_HIGH)
                        : null;
            }
            if (!(entity instanceof PlayerEntity player) || SpectatorStateHelper.isSpectatorLike(player) || player.isInvisible()) {
                return null;
            }
            if (FogOfWarClientHelper.shouldClampKillerInstinct(localPlayer, player)
                    && !FogOfWarClientHelper.isWithinKillerInstinctLimit(localPlayer, player)) {
                return GetInstinctHighlight.HighlightResult.skip();
            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(localPlayer.getWorld());
            if (gameWorldComponent.isRole(player, Noellesroles.SURVIVAL_MASTER) && !localPlayer.canSee(player)) {
                return GetInstinctHighlight.HighlightResult.skip();
            }
            return GetInstinctHighlight.HighlightResult.withKeybind(Noellesroles.PUPPETEER.color(),
                    GetInstinctHighlight.HighlightResult.PRIORITY_HIGH + 1);
        });
    }

    // ===================== 疾跑锁 =====================

    /** 拥有傀儡时本体无法疾跑（傀儡形态不受限）。 */
    private static void tickSprintLock(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !isPuppeteer(player) || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        if (PuppeteerPlayerComponent.KEY.get(player).isControlling() || !hasPuppets(player)) {
            return;
        }
        client.options.sprintKey.setPressed(false);
        player.setSprinting(false);
    }
}

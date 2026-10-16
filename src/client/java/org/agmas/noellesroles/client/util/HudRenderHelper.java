package org.agmas.noellesroles.client.util;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.client.gui.HallucinationHudRenderer;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.util.rolehud.AssassinHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.BanditHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.BaosangHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.BodyguardHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.BreacherHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.CommanderHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.CorruptCopHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.CriminalReasonerHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.DreamEaterHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.FerrymanHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.GangsterStarHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.JunkerHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.MorphlingHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.NoisemakerHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.OrthopedistHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.PathogenHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.PhantomHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.PhonismHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.RecallerHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.ReporterHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.SerialKillerHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.RoleHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.SilencerHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.SwapperHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.TaotieHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.VoodooHudRenderer;
import org.agmas.noellesroles.client.util.rolehud.VultureHudRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudRenderHelper {
    private static final int UNLIMITED_WIDTH = Integer.MAX_VALUE;
    private static final float ABOVE_VOICE_CHAT_HUD_Z = 200.0F;
    private static final int TEXT_OUTLINE_COLOR = 0xFFFFFFFF;
    private static final int TEXT_OUTLINE_INSET = 1;
    public static final int LINE_GAP = 2;
    public static final int ASSASSIN_BOTTOM_PADDING = 5;

    private static final Map<Role, RoleHudRenderer> ROLE_HUD_REGISTRY = new LinkedHashMap<>();

    static {
        ROLE_HUD_REGISTRY.put(Noellesroles.ASSASSIN, new AssassinHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.BANDIT, new BanditHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.BAOSANG, new BaosangHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.BODYGUARD, new BodyguardHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.BREACHER, new BreacherHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.COMMANDER, new CommanderHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.CORRUPT_COP, new CorruptCopHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.CRIMINAL_REASONER, new CriminalReasonerHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.DREAM_EATER, new DreamEaterHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.FERRYMAN, new FerrymanHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.GANGSTER_STAR, new GangsterStarHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.JUNKER, new JunkerHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.MORPHLING, new MorphlingHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.NOISEMAKER, new NoisemakerHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.ORTHOPEDIST, new OrthopedistHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.PHANTOM, new PhantomHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.PATHOGEN, new PathogenHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.PHONISM, new PhonismHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.RECALLER, new RecallerHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.REPORTER, new ReporterHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.SERIAL_KILLER, new SerialKillerHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.SILENCER, new SilencerHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.SWAPPER, new SwapperHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.TAOTIE, new TaotieHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.VOODOO, new VoodooHudRenderer());
        ROLE_HUD_REGISTRY.put(Noellesroles.VULTURE, new VultureHudRenderer());
    }

    private HudRenderHelper() {}

    /**
     * Returns the local player if they are in-game and alive, otherwise null.
     */
    @Nullable
    public static ClientPlayerEntity getActivePlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;
        if (!GameFunctions.isPlayerPlayingAndAlive(client.player)) return null;
        return client.player;
    }

    /**
     * Draws a text line aligned to the bottom-right corner of the screen.
     * Returns the new drawY (above the drawn line) for stacking multiple lines.
     */
    public static int drawBottomRight(DrawContext context, TextRenderer renderer, Text text, int drawY, int color) {
        if (shouldHideBottomRightSkillHud()) {
            return drawY;
        }
        drawY -= measure(renderer, text);
        drawRightAligned(context, renderer, text, 0, drawY, color);
        return drawY;
    }

    /**
     * 与 Wathe 黑屏 alpha = min(fade / FADE_TIME, 1) 互补的可见度系数（1=完全可见，0=被全黑屏幕完全盖住）。
     * 供 HUD 元素随开局黑屏渐亮同步渐入；同帧读取，保证与黑屏曲线严格同步。
     */
    public static float getOpeningFadeVisibility() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return 1f;
        }
        int fade = GameWorldComponent.KEY.get(player.getWorld()).getFade();
        return 1f - Math.min(fade / (float) GameConstants.FADE_TIME, 1f);
    }

    /**
     * 按可见度系数缩放颜色 alpha 通道。
     */
    public static int applyVisibility(int color, float visibility) {
        if (visibility >= 1f) {
            return color;
        }
        int alpha = Math.round(((color >>> 24) & 0xFF) * visibility);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Draws a line right-aligned to the screen edge, outlined when the client config enables it
     * (inset by the outline width so it is not clipped by the screen edge). The outline goes
     * through TextRenderer#drawWithOutline, which font-replacing mods (e.g. ModernUI) also implement.
     * 黑屏渐亮期间整行连同描边同步渐入；全黑时整体跳过绘制。
     */
    public static void drawRightAligned(DrawContext context, TextRenderer renderer, Text text, int rightPadding, int y, int color) {
        float visibility = getOpeningFadeVisibility();
        if (visibility <= 0f) {
            return;
        }
        int x = context.getScaledWindowWidth() - renderer.getWidth(text) - TEXT_OUTLINE_INSET - rightPadding;
        if (!NoellesRolesConfig.HANDLER.instance().bottomRightHintOutline) {
            context.drawTextWithShadow(renderer, text, x, y, applyVisibility(color, visibility));
            return;
        }
        renderer.drawWithOutline(text.asOrderedText(), x, y, applyVisibility(color, visibility),
                applyVisibility(TEXT_OUTLINE_COLOR, visibility),
                context.getMatrices().peek().getPositionMatrix(), context.getVertexConsumers(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    /**
     * Returns the top Y of the active role's bottom-right ability HUD stack.
     * If the role has no bottom-right ability HUD this frame, the screen height is returned.
     */
    public static int getBottomRightSkillHudTopY(DrawContext context, TextRenderer renderer, ClientPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        int bottom = context.getScaledWindowHeight();
        for (Map.Entry<Role, RoleHudRenderer> entry : ROLE_HUD_REGISTRY.entrySet()) {
            if (gameWorld.isRole(player, entry.getKey())) {
                return entry.getValue().getTopY(renderer, player, bottom);
            }
        }
        return bottom;
    }

    public static int getBottomRightSkillHudRightPadding(ClientPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.ASSASSIN) ? ASSASSIN_BOTTOM_PADDING : 0;
    }

    public static int measure(TextRenderer renderer, Text text) {
        return renderer.getWrappedLinesHeight(text, UNLIMITED_WIDTH);
    }

    /**
     * Pushes a GUI depth layer that renders above Simple Voice Chat group avatars.
     * Use this for custom bottom-corner overlays that would otherwise be covered.
     */
    public static void pushAboveVoiceChatHudLayer(DrawContext context) {
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, ABOVE_VOICE_CHAT_HUD_Z);
    }

    public static void popAboveVoiceChatHudLayer(DrawContext context) {
        context.getMatrices().pop();
    }

    /**
     * Moves drawY up by one stacked text line (plus an extra gap above it).
     */
    public static int stackLine(int drawY, TextRenderer renderer, Text text, int gap) {
        return drawY - measure(renderer, text) - gap;
    }

    public static String getAbilityKeyName() {
        return NoellesrolesClient.abilityBind == null
                ? ""
                : NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
    }

    public static Text getAbilityKeyText() {
        return NoellesrolesClient.abilityBind == null
                ? Text.empty()
                : NoellesrolesClient.abilityBind.getBoundKeyLocalizedText();
    }

    public static String getAbility2KeyName() {
        return NoellesrolesClient.ability2Bind == null
                ? ""
                : NoellesrolesClient.ability2Bind.getBoundKeyLocalizedText().getString();
    }

    public static Text getAbility2KeyText() {
        return NoellesrolesClient.ability2Bind == null
                ? Text.empty()
                : NoellesrolesClient.ability2Bind.getBoundKeyLocalizedText();
    }

    private static boolean shouldHideBottomRightSkillHud() {
        return HallucinationHudRenderer.shouldSuppressSkillHintHud();
    }
}

package org.agmas.noellesroles.client.gui;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.KnifeItem;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/**
 * 蓄力时顶替 Wathe 匕首条的蓄力条，位置与它一致（准星下方 5 像素），造型也照搬 hud/knife_* 贴图：木质握把 + 随蓄力生长的刀刃。
 * 蓄力中刀刃为绿色，蓄力完成转黄，瞄准可刺杀目标时整把刀换成 Wathe attack 贴图的红粉配色。
 * 匕首下方的细条是第二进度条，蓄力完成后从左向右填满、整条一起由蓝转红，读满即到达强制取消时长。
 */
public final class KnifeChargeHudRenderer {
    /** 与 Wathe 匕首条同尺寸：贴图 1 像素 = 1 像素 */
    private static final int UNIT = 1;
    private static final int WIDTH = UNIT * 10;
    private static final int HANDLE_WIDTH = UNIT * 4;
    private static final int BLADE_WIDTH = WIDTH - HANDLE_WIDTH;
    /** 刀身占 Wathe 贴图第 2、3 行，护手列在上下各多出 1 像素 */
    private static final int CORE_TOP = UNIT;
    private static final int CORE_HEIGHT = UNIT * 2;
    private static final int GUARD_HEIGHT = UNIT * 4;
    private static final int HOLD_BAR_HEIGHT = UNIT;
    /** Wathe 的匕首条画在中心点 (-5, 5) 处，我们占满同一条 10×4 区域，蓝色细条落在其下方的空行里 */
    private static final int BAR_OFFSET_Y = 5;

    /** KnifeItem.onStoppedUsing 在 remainingUseTicks >= maxUseTime - 10 时不刺杀，即按住要超过 10 tick 才能刺杀 */
    private static final int CHARGE_TICKS = 10;
    private static final float COMPLETE_TICKS = CHARGE_TICKS + 1;

    private static final int COLOR_EMPTY = 0xFF3A3B3C;
    private static final int COLOR_TIP = 0xFFFFFFFF;
    /** 第二进度条随时间从蓝变红的色相范围（0.62 为蓝，0.99 为红） */
    private static final float HOLD_HUE_START = 0.62F;
    private static final float HOLD_HUE_END = 0.99F;
    private static final int[] HANDLE_STRIPES = {0xFF826D6C, 0xFF6D5957, 0xFF826D6C, 0xFF6D5957};
    private static final int HANDLE_GUARD = 0xFF5B4A49;
    /** "松手即可刺杀"状态：沿用 Wathe hud/knife_attack 的红粉结构，但饱和度拉高以保证醒目 */
    private static final int[] TARGET_STRIPES = {0xFFD93A3A, 0xFFAD1E1E, 0xFFD93A3A, 0xFFAD1E1E};
    private static final int TARGET_GUARD = 0xFF7E1212;
    private static final int COLOR_CHARGING_SPINE = 0xFF3FBF3F;
    private static final int COLOR_CHARGING_EDGE = 0xFF74DA74;
    private static final int COLOR_READY_SPINE = 0xFFE0B32E;
    private static final int COLOR_READY_EDGE = 0xFFF5DA72;
    private static final int COLOR_TARGET_SPINE = 0xFFE51A1A;
    private static final int COLOR_TARGET_EDGE = 0xFFFF5A5A;

    private KnifeChargeHudRenderer() {}

    /** 玩家是否正在举刀蓄力 */
    public static boolean isCharging(ClientPlayerEntity player) {
        return player != null && player.isUsingItem() && player.getActiveItem().isOf(WatheItems.KNIFE);
    }

    /** 蓄力完成且瞄准可刺杀目标，即整把匕首变红的状态 */
    public static boolean isChargingAtTarget(ClientPlayerEntity player) {
        return isCharging(player) && player.getItemUseTime() > CHARGE_TICKS && isStabReady(player);
    }

    public static void render(ClientPlayerEntity player, DrawContext context) {
        if (!isCharging(player)) {
            return;
        }

        int maxUseTicks = player.getActiveItem().getMaxUseTime(player);
        int useTime = Math.min(player.getItemUseTime(), maxUseTicks);
        boolean complete = useTime > CHARGE_TICKS;
        float chargeProgress = useTime / COMPLETE_TICKS;
        float holdProgress = MathHelper.clamp((useTime - COMPLETE_TICKS) / (maxUseTicks - COMPLETE_TICKS), 0.0F, 1.0F);
        boolean canStab = complete && isStabReady(player);

        int left = context.getScaledWindowWidth() / 2 - WIDTH / 2;
        int top = context.getScaledWindowHeight() / 2 + BAR_OFFSET_Y;
        int coreTop = top + CORE_TOP;
        int coreBottom = coreTop + CORE_HEIGHT;

        context.fill(left, coreTop, left + WIDTH, coreBottom, COLOR_EMPTY);

        int[] stripes = canStab ? TARGET_STRIPES : HANDLE_STRIPES;
        for (int i = 0; i < stripes.length; i++) {
            context.fill(left + i * UNIT, coreTop, left + (i + 1) * UNIT, coreBottom, stripes[i]);
        }
        int guardColor = canStab ? TARGET_GUARD : HANDLE_GUARD;
        int guardLeft = left + HANDLE_WIDTH - UNIT;
        context.fill(guardLeft, top, left + HANDLE_WIDTH, coreTop, guardColor);
        context.fill(guardLeft, coreBottom, left + HANDLE_WIDTH, top + GUARD_HEIGHT, guardColor);

        int bladeLength = complete ? BLADE_WIDTH : Math.round(chargeProgress * BLADE_WIDTH);
        if (bladeLength > 0) {
            int spineColor = canStab ? COLOR_TARGET_SPINE : complete ? COLOR_READY_SPINE : COLOR_CHARGING_SPINE;
            int edgeColor = canStab ? COLOR_TARGET_EDGE : complete ? COLOR_READY_EDGE : COLOR_CHARGING_EDGE;
            int bladeLeft = left + HANDLE_WIDTH;
            int bladeEnd = bladeLeft + bladeLength;
            // 刃尖前 2 像素只留刀背，最后 1 像素为白色尖头
            int frontStart = Math.max(bladeLeft, bladeEnd - UNIT * 2);
            int tipStart = Math.max(bladeLeft, bladeEnd - UNIT);

            if (frontStart > bladeLeft) {
                context.fill(bladeLeft, coreTop, frontStart, coreTop + UNIT, spineColor);
                context.fill(bladeLeft, coreTop + UNIT, frontStart, coreBottom, edgeColor);
            }
            if (tipStart > frontStart) {
                context.fill(frontStart, coreTop, tipStart, coreTop + UNIT, spineColor);
            }
            context.fill(tipStart, coreTop, bladeEnd, coreTop + UNIT, COLOR_TIP);
        }

        int holdTop = top + GUARD_HEIGHT;
        context.fill(left, holdTop, left + WIDTH, holdTop + HOLD_BAR_HEIGHT, COLOR_EMPTY);
        int holdEnd = left + (int) (holdProgress * WIDTH);
        if (holdEnd > left) {
            context.fill(left, holdTop, holdEnd, holdTop + HOLD_BAR_HEIGHT, holdColor(holdProgress));
        }
    }

    /** 整条一起随时间变色：蓝（刚开始）→ 紫 → 红（即将被强制取消） */
    private static int holdColor(float progress) {
        return 0xFF000000 | MathHelper.hsvToRgb(MathHelper.lerp(progress, HOLD_HUE_START, HOLD_HUE_END), 1.0F, 1.0F);
    }

    /** 与 KnifeItem.onStoppedUsing 一致：射线命中存活玩家，或命中床方块且床上有睡觉的玩家 */
    private static boolean isStabReady(ClientPlayerEntity player) {
        HitResult hit = KnifeItem.getKnifeTarget(player);
        if (hit instanceof EntityHitResult) {
            return true;
        }
        if (hit instanceof BlockHitResult blockHitResult) {
            return RevolverItem.findSleepingPlayerOnBed(player.getWorld(), blockHitResult).isPresent();
        }
        return false;
    }
}

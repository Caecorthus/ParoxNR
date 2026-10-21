package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.phonism.PhonismPlayerComponent;
import org.agmas.noellesroles.phonism.SoundCollectorHelper;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.CrosshairTargetHelper;

import java.util.List;
import java.util.UUID;

/**
 * 声音收集器：谎言音效师商店道具（每局限购一次，50 金币）。
 * 右键安装到视野 5 格内的玩家（纯组件状态，目标身上不产生任何实体物品，对所有人不可见）；
 * 物品保留在音效师物品栏作为控制器，再右键开关监听中继——目标听到的语音与世界音效
 * 会重放到音效师耳边；空手右键持有者或其尸体回收，回收后可换装他人。
 */
public class SoundCollectorItem extends Item {
    /** 安装距离：视野 5 格内（与对准度阈值同 CrosshairTargetHelper 惯例） */
    public static final double INSTALL_RANGE = 5.0;
    public static final double INSTALL_MIN_ALIGNMENT = 0.85;

    public SoundCollectorItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand != Hand.MAIN_HAND || user.isSneaking()) {
            return TypedActionResult.pass(stack); // 潜行留给门/方块交互习惯
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(user, Noellesroles.PHONISM)) {
            return TypedActionResult.pass(stack);
        }
        if (world.isClient) {
            return TypedActionResult.success(stack); // 实际逻辑由服务端执行
        }
        if (!(user instanceof ServerPlayerEntity serverUser)
                || !GameFunctions.isPlayerPlayingAndAlive(serverUser)
                || SwallowedPlayerComponent.isPlayerSwallowed(serverUser)) {
            return TypedActionResult.pass(stack);
        }

        PhonismPlayerComponent component = PhonismPlayerComponent.KEY.get(serverUser);
        if (component.getCollectorTarget() == null) {
            install(serverUser, gameWorld);
        } else {
            toggleRelay(serverUser, component);
        }
        return TypedActionResult.success(stack);
    }

    private void install(ServerPlayerEntity user, GameWorldComponent gameWorld) {
        PlayerEntity target = CrosshairTargetHelper.findCrosshairTarget(user, INSTALL_RANGE, INSTALL_MIN_ALIGNMENT);
        boolean valid = target != null
                && !target.equals(user)
                && GameFunctions.isPlayerPlayingAndAlive(target)
                && gameWorld.getAllPlayers().contains(target.getUuid())
                && !SwallowedPlayerComponent.isPlayerSwallowed(target)
                && !SoundCollectorHelper.isTargetTaken(user.getWorld(), target.getUuid(), user.getUuid());
        if (!valid) {
            user.sendMessage(Text.translatable("tip.phonism.collector_no_target"), true);
            return;
        }

        SoundCollectorHelper.install(target.getUuid(), user.getUuid());
        PhonismPlayerComponent.KEY.get(user).installCollector(target.getUuid());
        user.sendMessage(Text.translatable("tip.phonism.collector_installed", target.getDisplayName()), true);

        NbtCompound extra = new NbtCompound();
        extra.putString("action", "collector_install");
        GameRecordManager.recordSkillUse(user, Noellesroles.PHONISM_ID,
                target instanceof ServerPlayerEntity serverTarget ? serverTarget : null, extra);
    }

    private void toggleRelay(ServerPlayerEntity user, PhonismPlayerComponent component) {
        boolean turnOn = !component.isRelayActive();
        component.setRelayActive(turnOn);

        UUID targetUuid = component.getCollectorTarget();
        PlayerEntity target = targetUuid == null ? null : user.getWorld().getPlayerByUuid(targetUuid);
        if (turnOn) {
            Text targetName = target != null
                    ? target.getDisplayName()
                    : Text.translatable("screen.role_target.unknown_player");
            user.sendMessage(Text.translatable("tip.phonism.relay_on", targetName), true);
        } else {
            user.sendMessage(Text.translatable("tip.phonism.relay_off"), true);
        }

        NbtCompound extra = new NbtCompound();
        extra.putString("action", turnOn ? "relay_on" : "relay_off");
        if (targetUuid != null) {
            extra.putUuid("target", targetUuid);
        }
        GameRecordManager.recordSkillUse(user, Noellesroles.PHONISM_ID,
                target instanceof ServerPlayerEntity serverTarget ? serverTarget : null, extra);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // 按 \n 手动分行并统一灰色渲染（tooltip 不会自动处理换行符）
        String full = Text.translatable("item.noellesroles.sound_collector.tooltip").getString();
        for (String line : full.split("\n")) {
            tooltip.add(Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}

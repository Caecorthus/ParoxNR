package org.agmas.noellesroles.serialkiller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 连环杀手玩家组件
 * 管理疯魔爆发技能与死亡倒计时：
 * 释放技能进入30秒无护盾疯魔模式；首次疯魔结束后开启180秒死亡倒计时；
 * 死亡倒计时期间再次释放技能，则本次疯魔结束时立即死亡。
 */
public class SerialKillerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<SerialKillerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "serial_killer"), SerialKillerPlayerComponent.class);

    // 每次使用技能后的冷却（100秒，开局3分钟在角色分配时单独设置）
    public static final int SKILL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 40);
    // 首次疯魔结束后到死亡的时间（180秒）
    public static final int DOOM_COUNTDOWN_TICKS = GameConstants.getInTicks(3, 0);

    private final PlayerEntity player;

    // 死亡倒计时剩余tick（0 = 未开启），同步给客户端HUD
    private int doomTicks = 0;
    // 技能触发的疯魔进行中（疯魔结束瞬间开启倒计时，期间每tick重断言无护盾）
    private boolean skillPsychoActive = false;
    private boolean firstSkillUsed = false;
    private boolean secondSkillUsed = false;

    public SerialKillerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        // 技能疯魔占用了世界级疯魔计数，回合结束/开局清理时须归还。
        // 疯魔中死亡时 Wathe 的 killPlayer 已自行 stopPsycho，故仅当疯魔仍活跃时归还，避免重复扣减。
        if (this.skillPsychoActive && PlayerPsychoComponent.KEY.get(this.player).psychoTicks > 0) {
            PlayerPsychoComponent.KEY.get(this.player).stopPsycho(true);
        }
        this.doomTicks = 0;
        this.skillPsychoActive = false;
        this.firstSkillUsed = false;
        this.secondSkillUsed = false;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.doomTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.doomTicks = buf.readInt();
    }

    /**
     * 尝试释放技能。由技能键的C2S接收器调用（冷却已在接收器处拦下）。
     *
     * @return 是否成功释放
     */
    public boolean tryUseSkill() {
        if (!(player.getWorld() instanceof ServerWorld)) return false;
        if (this.secondSkillUsed) return false;

        PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);
        // 已处于疯魔状态（技能或商店疯魔）时不可再次释放
        if (psychoComponent.psychoTicks > 0) return false;
        // 第二次释放必须在死亡倒计时期间
        if (this.firstSkillUsed && this.doomTicks <= 0) return false;

        // 使用 Wathe 原生世界疯魔计数，客户端 isPsychoActive() 会触发疯魔BGM
        if (!psychoComponent.startPsycho(true)) {
            return false; // 背包没有空位放不下球棒
        }
        // 技能疯魔不提供任何护盾
        psychoComponent.setArmour(0);

        this.skillPsychoActive = true;
        if (!this.firstSkillUsed) {
            this.firstSkillUsed = true;
        } else {
            this.secondSkillUsed = true;
            // 第二次疯魔结束时立即死亡：把倒计时压到本段疯魔的剩余时长
            if (this.doomTicks > 0) {
                this.doomTicks = Math.min(this.doomTicks, GameConstants.PSYCHO_TIMER);
            }
        }

        AbilityPlayerComponent.KEY.get(this.player).setCooldown(SKILL_COOLDOWN_TICKS);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            extra.putString("action", this.secondSkillUsed ? "psycho_again" : "psycho_start");
            GameRecordManager.recordSkillUse(serverPlayer, Noellesroles.SERIAL_KILLER_ID, null, extra);
        }
        this.sync();
        return true;
    }

    public int getDoomTicks() {
        return this.doomTicks;
    }

    @Override
    public void serverTick() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);
        if (!gameWorldComponent.isRole(player, Noellesroles.SERIAL_KILLER)) return;

        PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);

        if (this.skillPsychoActive) {
            if (psychoComponent.psychoTicks <= 0) {
                // 技能疯魔自然结束
                this.skillPsychoActive = false;
                if (!this.secondSkillUsed && this.doomTicks <= 0) {
                    // 首次疯魔结束：开启死亡倒计时
                    this.doomTicks = DOOM_COUNTDOWN_TICKS;
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.sendMessage(Text.translatable("tip.noellesroles.serial_killer.doom_started", DOOM_COUNTDOWN_TICKS / 20)
                                .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
                    }
                    this.sync();
                }
            } else {
                // 每tick重断言，确保技能疯魔全程无护盾
                psychoComponent.setArmour(0);
            }
        }

        if (this.doomTicks > 0) {
            this.doomTicks--;
            if (this.doomTicks <= 0) {
                this.sync();
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // force=true：无视KillPlayer.BEFORE取消，与黑帮巨星/小丑超时死亡一致
                    GameFunctions.killPlayer(serverPlayer, true, null, Noellesroles.DEATH_REASON_SERIAL_KILLER_DOOM, true);
                }
            } else if (this.doomTicks % 20 == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void clientTick() {
        if (this.doomTicks > 0) {
            this.doomTicks--;
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("doomTicks", this.doomTicks);
        tag.putBoolean("firstSkillUsed", this.firstSkillUsed);
        tag.putBoolean("secondSkillUsed", this.secondSkillUsed);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.doomTicks = tag.contains("doomTicks") ? tag.getInt("doomTicks") : 0;
        this.firstSkillUsed = tag.getBoolean("firstSkillUsed");
        this.secondSkillUsed = tag.getBoolean("secondSkillUsed");
    }
}

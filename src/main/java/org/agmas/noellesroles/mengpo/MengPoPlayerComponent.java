package org.agmas.noellesroles.mengpo;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 孟婆开局的「渡人 / 渡己」选择：倒计时中客户端自动弹出选择界面，超时默认渡人。
 * 选择渡己后玩家立即转为其他职业，本组件随之重置；只有渡人会保留 {@link Mode#DUREN}。
 */
public class MengPoPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<MengPoPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "mengpo"), MengPoPlayerComponent.class);

    public static final int CHOICE_TICKS = GameConstants.getInTicks(0, 20);

    public enum Mode { NONE, DUREN }

    private final PlayerEntity player;
    private int choiceTicks = 0;
    private Mode mode = Mode.NONE;

    public MengPoPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.choiceTicks = 0;
        this.mode = Mode.NONE;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void startChoice() {
        this.choiceTicks = CHOICE_TICKS;
        this.mode = Mode.NONE;
        this.sync();
    }

    public void finishChoice(Mode mode) {
        this.choiceTicks = 0;
        this.mode = mode;
        this.sync();
    }

    public boolean isChoosing() {
        return this.choiceTicks > 0;
    }

    public int getChoiceTicks() {
        return this.choiceTicks;
    }

    public boolean isDuren() {
        return this.mode == Mode.DUREN;
    }

    @Override
    public void serverTick() {
        if (this.choiceTicks <= 0) {
            return;
        }
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)
                || !GameWorldComponent.KEY.get(serverPlayer.getWorld()).isRole(serverPlayer, Noellesroles.MENGPO)) {
            this.choiceTicks = 0;
            this.sync();
            return;
        }
        this.choiceTicks--;
        if (this.choiceTicks <= 0) {
            // 超时未选：默认渡人
            MengPoHelper.applyChoice(serverPlayer, false);
            return;
        }
        if (this.choiceTicks % 20 == 0) {
            this.sync();
        }
    }

    @Override
    public void clientTick() {
        // 本地只倒数到 1，由服务端的最终同步决定何时结束，避免服务端卡顿时界面先关再被下一次同步重新弹出
        if (this.choiceTicks > 1) {
            this.choiceTicks--;
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.choiceTicks);
        buf.writeEnumConstant(this.mode);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.choiceTicks = buf.readInt();
        this.mode = buf.readEnumConstant(Mode.class);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("choiceTicks", this.choiceTicks);
        tag.putString("mode", this.mode.name());
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.choiceTicks = tag.contains("choiceTicks") ? tag.getInt("choiceTicks") : 0;
        Mode loaded = Mode.NONE;
        if (tag.contains("mode")) {
            try {
                loaded = Mode.valueOf(tag.getString("mode"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.mode = loaded;
    }
}

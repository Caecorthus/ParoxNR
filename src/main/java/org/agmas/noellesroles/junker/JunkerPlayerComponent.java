package org.agmas.noellesroles.junker;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.BodyTargetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 拾荒者状态：当前正在搜刮的尸体与注视进度。
 * 进度由服务端计算，每几 tick 同步一次给客户端用于 HUD。
 */
public class JunkerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<JunkerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "junker"), JunkerPlayerComponent.class);

    public static final int SEARCH_TICKS_REQUIRED = 100;
    private static final int SYNC_INTERVAL_TICKS = 4;

    private final PlayerEntity player;
    @Nullable
    private UUID searchBodyUuid;
    private int searchProgressTicks;
    private boolean searching;

    public JunkerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isSearching() {
        return this.searching || this.searchBodyUuid != null;
    }

    public int getSearchProgressTicks() {
        return this.searchProgressTicks;
    }

    public float getSearchProgress() {
        return Math.min(1.0F, (float) this.searchProgressTicks / SEARCH_TICKS_REQUIRED);
    }

    public void startSearch(UUID bodyEntityUuid) {
        this.searchBodyUuid = bodyEntityUuid;
        this.searchProgressTicks = 0;
        this.searching = true;
        this.sync();
    }

    public void cancelSearch() {
        if (!isSearching() && this.searchProgressTicks == 0) {
            return;
        }
        this.searchBodyUuid = null;
        this.searchProgressTicks = 0;
        this.searching = false;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.searchBodyUuid == null) {
            return;
        }
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerPlayingAndAlive(this.player)
                || SwallowedPlayerComponent.isPlayerSwallowed(this.player)) {
            cancelSearch();
            return;
        }

        UUID bodyUuid = this.searchBodyUuid;
        boolean stillWatching = BodyTargetHelper.raycastBody(this.player, JunkerSearchHelper.SEARCH_RANGE,
                body -> body.getUuid().equals(bodyUuid)) != null;
        if (!stillWatching) {
            cancelSearch();
            return;
        }

        this.searchProgressTicks++;
        if (this.searchProgressTicks >= SEARCH_TICKS_REQUIRED) {
            this.searchBodyUuid = null;
            this.searchProgressTicks = 0;
            this.searching = false;
            this.sync();
            JunkerSearchHelper.completeSearch(serverPlayer, bodyUuid);
            return;
        }
        if (this.searchProgressTicks % SYNC_INTERVAL_TICKS == 0) {
            this.sync();
        }
    }

    public void reset() {
        this.searchBodyUuid = null;
        this.searchProgressTicks = 0;
        this.searching = false;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.searchBodyUuid != null);
        buf.writeInt(this.searchProgressTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.searching = buf.readBoolean();
        this.searchProgressTicks = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("searchProgressTicks", this.searchProgressTicks);
        if (this.searchBodyUuid != null) {
            tag.putUuid("searchBodyUuid", this.searchBodyUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.searchProgressTicks = tag.contains("searchProgressTicks") ? tag.getInt("searchProgressTicks") : 0;
        this.searchBodyUuid = tag.contains("searchBodyUuid") ? tag.getUuid("searchBodyUuid") : null;
        this.searching = this.searchBodyUuid != null;
    }
}

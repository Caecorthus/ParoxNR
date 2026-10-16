package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.cca.MapVotingComponent;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.deatharena.DeathArenaStateHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MapVotingComponent.class)
public abstract class MapVotingComponentMixin {
    @Shadow
    public abstract void sync();

    @Inject(method = "startMapVotingForMode", at = @At("RETURN"))
    private void noellesroles$filterDeathArenaFromMapVote(Identifier gameModeId, int playerCount, CallbackInfo ci) {
        Identifier arenaMapId = DeathArenaStateHelper.getConfiguredMapId();
        Identifier arenaDimensionId = DeathArenaStateHelper.getArenaDimensionId();
        if (arenaMapId == null && arenaDimensionId == null) {
            return;
        }

        MapVotingComponentAccessor accessor = (MapVotingComponentAccessor) this;
        boolean removedAvailable = accessor.noellesroles$getAvailableMaps().removeIf(entry ->
                (arenaMapId != null && arenaMapId.equals(entry.mapId()))
                        || (arenaDimensionId != null && arenaDimensionId.equals(entry.dimensionId())));
        boolean removedUnavailable = accessor.noellesroles$getUnavailableMaps().removeIf(entry ->
                arenaDimensionId != null && arenaDimensionId.equals(entry.dimensionId()));

        if (!removedAvailable && !removedUnavailable) {
            return;
        }

        accessor.noellesroles$setVoteCounts(new int[accessor.noellesroles$getAvailableMaps().size()]);
        if (accessor.noellesroles$getSelectedMapIndex() >= accessor.noellesroles$getAvailableMaps().size()) {
            accessor.noellesroles$setSelectedMapIndex(-1);
        }
        sync();
    }

    @Inject(method = "buildModeChoices", at = @At("RETURN"))
    private void noellesroles$hideDeprecatedMurderMayhemMode(CallbackInfo ci) {
        MapVotingComponentAccessor accessor = (MapVotingComponentAccessor) this;
        List<MapVotingComponent.VotingModeEntry> availableModes = accessor.noellesroles$getAvailableModes();

        // 迷乱谋杀模式已废弃：从可选模式里移除，玩家在模式投票中看不到它。
        // 必须在 buildModeChoices 的 RETURN（wathe 插入「随机模式」选项之前）移除，这样
        // 随机模式选项的插入条件与 selectRandomModeFromCurrentChoices 的抽样池都不会再包含它。
        // 注：以前这里还会把迷乱谋杀项重排到经典谋杀后面，那套重排逻辑随本次隐藏一并删除。
        availableModes.removeIf(entry -> Noellesroles.MURDER_MAYHEM_ID.equals(entry.gameModeId()));
    }

    /** 跳过模式投票：不进入模式投票，直接按当前/上一局模式开始地图投票。 */
    @Inject(method = "startModeVoting(Z)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipModeVoting(boolean forceModeVoting, CallbackInfo ci) {
        if (!NoellesRolesConfig.HANDLER.instance().skipModeVoting) {
            return;
        }
        ci.cancel();
        ((MapVotingComponent) (Object) this).startVoting();
    }

    /** 跳过地图投票：地图投票一开始就按上一局地图直接结算（含选图轮盘）。 */
    @Inject(method = "startMapVotingForMode", at = @At("RETURN"))
    private void noellesroles$skipMapVotingOnStart(Identifier gameModeId, int playerCount, CallbackInfo ci) {
        if (!NoellesRolesConfig.HANDLER.instance().skipMapVoting) {
            return;
        }
        noellesroles$skipMapVoting();
    }

    /** 兜底：上一局地图不属于当前模式时走随机结算，轮盘一出现即结束。 */
    @Inject(method = "serverTick", at = @At("TAIL"))
    private void noellesroles$skipRoulette(CallbackInfo ci) {
        MapVotingComponentAccessor accessor = (MapVotingComponentAccessor) this;
        if (accessor.noellesroles$isVotingActive()
                && accessor.noellesroles$isRoulettePhase()
                && NoellesRolesConfig.HANDLER.instance().skipMapVoting) {
            accessor.noellesroles$invokeFinishSelection();
        }
    }

    private void noellesroles$skipMapVoting() {
        MapVotingComponentAccessor accessor = (MapVotingComponentAccessor) this;
        if (!accessor.noellesroles$isVotingActive()) {
            return;
        }
        Identifier previousDimension = ((MapVotingComponent) (Object) this).getLastSelectedDimension();
        List<MapVotingComponent.VotingMapEntry> maps = accessor.noellesroles$getAvailableMaps();
        if (previousDimension != null) {
            for (int i = 0; i < maps.size(); i++) {
                if (previousDimension.equals(maps.get(i).dimensionId())) {
                    accessor.noellesroles$setSelectedMapIndex(i);
                    accessor.noellesroles$invokeFinishSelection();
                    return;
                }
            }
        }
        accessor.noellesroles$invokeEndVoting(true);
    }
}

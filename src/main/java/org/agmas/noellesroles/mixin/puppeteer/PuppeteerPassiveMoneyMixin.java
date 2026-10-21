package org.agmas.noellesroles.mixin.puppeteer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.puppeteer.PuppeteerRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 傀儡师金币自然上涨上限为 300（原版杀手为 200）：Wathe 在余额 < 200 时发放被动收入，
 * 这里对傀儡师把参与比较的余额整体下移，等效于把上限抬到 300。击杀奖励等其他收入不受影响。
 */
@Mixin(MurderGameMode.class)
public abstract class PuppeteerPassiveMoneyMixin {
    @ModifyExpressionValue(
            method = "tickServerGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerShopComponent;getBalance()I",
                    ordinal = 0
            )
    )
    // 作用域内有两个 ServerPlayerEntity 局部变量（neutralWinner、循环里的 player），取第二个
    private int noellesroles$raisePuppeteerPassiveCap(int balance, @Local(ordinal = 1) ServerPlayerEntity player,
                                                      @Local(argsOnly = true) GameWorldComponent gameWorldComponent) {
        if (gameWorldComponent.isRole(player, Noellesroles.PUPPETEER)) {
            return PuppeteerRules.passiveIncomeComparableBalance(balance, GameConstants.KILLER_PASSIVE_MONEY_CAP);
        }
        return balance;
    }
}

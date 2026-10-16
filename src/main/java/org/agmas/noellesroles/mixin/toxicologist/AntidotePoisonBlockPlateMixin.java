package org.agmas.noellesroles.mixin.toxicologist;

import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.toxicologist.AntidotePlateShieldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 毒瓶对带解毒剂预防护的餐盘/饮品托盘下毒时：护盾被消耗、下毒不生效。
 * 只重定向 onUse 中真正下毒的那次 setPoisoner 调用（ordinal 0）；
 * wathe 的扣毒瓶/音效/记录照常执行，因此投毒方消耗与下毒成功时一致。
 */
@Mixin(FoodPlatterBlock.class)
public abstract class AntidotePoisonBlockPlateMixin {

    @Redirect(
            method = "onUse",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/block_entity/BeveragePlateBlockEntity;setPoisoner(Ljava/lang/String;)V", ordinal = 0)
    )
    private void noellesroles$blockPoisonOnShieldedPlate(BeveragePlateBlockEntity plate, String poisonerUuid) {
        World world = plate.getWorld();
        if (world != null && !world.isClient) {
            AntidotePlateShieldComponent shieldComp = AntidotePlateShieldComponent.KEY.get(world);
            if (shieldComp.consumeShield(plate.getPos())) {
                AntidotePlateShieldComponent.KEY.sync(world);
                return;
            }
        }
        plate.setPoisoner(poisonerUuid);
    }
}

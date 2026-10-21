package org.agmas.noellesroles.mixin.phonism;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.phonism.SoundCollectorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 声音收集器：世界音效中继。
 * 收集目标（存活玩家）收到的 PlaySound / PlaySoundFromEntity 包，若该目标正被某个
 * 音效师监听，就在音效师耳边原样重放一份（同音效/类别/音量/音调/种子，坐标改为音效师位置）。
 * 私密中继：复制包只发给音效师本人。不做防双音距离判定（用户决定）——
 * 音效师贴近声源时自然声与重放可能叠着听，换取"目标听到的都送得到"。
 * 注：sendPacket 在 1.21.1 声明于父类 ServerCommonNetworkHandler（ServerPlayNetworkHandler 未覆写），
 * mixin 必须挂在声明类上，再用 instanceof 过滤对局处理器。
 */
@Mixin(ServerCommonNetworkHandler.class)
public abstract class SoundCollectorWorldSoundRelayMixin {
    /** 防环：本钩子在复制包发给音效师时会再次触发（音效师同时是别人的收集目标、
     *  或双音效师互听成环），同线程重入直接跳过。 */
    private static final ThreadLocal<Boolean> noellesroles$RELAYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void noellesroles$relayCollectorWorldSound(Packet<?> packet, CallbackInfo ci) {
        // 守卫开销近零：没有活跃中继时立即返回
        if (!SoundCollectorHelper.hasActiveTaps()) {
            return;
        }
        if (Boolean.TRUE.equals(noellesroles$RELAYING.get())) {
            return;
        }
        if (!((Object) this instanceof ServerPlayNetworkHandler handler)) {
            return;
        }
        ServerPlayerEntity listener = handler.player;
        if (listener == null) {
            return;
        }
        ServerPlayerEntity phonism = SoundCollectorHelper.getPhonismForTarget(listener.getUuid());
        if (phonism == null || phonism == listener) {
            return;
        }

        PlaySoundS2CPacket copy;
        if (packet instanceof PlaySoundS2CPacket play) {
            copy = new PlaySoundS2CPacket(play.getSound(), play.getCategory(),
                    phonism.getX(), phonism.getY(), phonism.getZ(),
                    play.getVolume(), play.getPitch(), play.getSeed());
        } else if (packet instanceof PlaySoundFromEntityS2CPacket fromEntity) {
            copy = new PlaySoundS2CPacket(fromEntity.getSound(), fromEntity.getCategory(),
                    phonism.getX(), phonism.getY(), phonism.getZ(),
                    fromEntity.getVolume(), fromEntity.getPitch(), fromEntity.getSeed());
        } else {
            return;
        }

        noellesroles$RELAYING.set(Boolean.TRUE);
        try {
            phonism.networkHandler.sendPacket(copy);
        } finally {
            noellesroles$RELAYING.set(Boolean.FALSE);
        }
    }
}

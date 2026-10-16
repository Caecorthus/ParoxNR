package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.packets.Packet;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.dualrole.DualRoleSelectionWorldComponent;
import org.agmas.noellesroles.hallucination.HallucinationHelper;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoellesrolesVoiceChatPlugin implements VoicechatPlugin {
    // Static reference to the server API for use by other classes
    private static VoicechatServerApi serverApi = null;

    // Map of Taotie UUID to their stomach group
    private static final Map<UUID, Group> taotieStomachGroups = new HashMap<>();

    public static VoicechatServerApi getServerApi() {
        return serverApi;
    }

    /**
     * Get or create a stomach group for a Taotie player
     * @param taotie The Taotie player
     * @return The stomach group for this Taotie
     */
    public static Group getOrCreateStomachGroup(ServerPlayerEntity taotie) {
        if (serverApi == null) return null;

        UUID taotieUuid = taotie.getUuid();

        // Return existing group if valid
        Group existing = taotieStomachGroups.get(taotieUuid);
        if (existing != null) {
            return existing;
        }

        // Create new group with player name (max 16 chars, perfect for Minecraft usernames)
        String groupName = "tts-" + taotie.getName().getString();
        Group newGroup = serverApi.groupBuilder()
                .setPersistent(false)
                .setName(groupName)
                .setType(Group.Type.NORMAL)
                .setHidden(true)
                .build();

        taotieStomachGroups.put(taotieUuid, newGroup);
        return newGroup;
    }

    /**
     * Add a swallowed player to a Taotie's stomach group
     * @param taotie The Taotie player
     * @param swallowedPlayer The player being swallowed
     */
    public static void addToStomachGroup(ServerPlayerEntity taotie, ServerPlayerEntity swallowedPlayer) {
        if (serverApi == null) return;

        Group stomachGroup = getOrCreateStomachGroup(taotie);
        if (stomachGroup == null) return;

        VoicechatConnection connection = serverApi.getConnectionOf(swallowedPlayer.getUuid());
        if (connection != null) {
            connection.setGroup(stomachGroup);
        }
    }

    /**
     * Remove a player from voice chat groups (set to null)
     * @param playerUuid The UUID of the player to remove
     */
    public static void removeFromVoiceChat(UUID playerUuid) {
        if (serverApi == null) return;

        VoicechatConnection connection = serverApi.getConnectionOf(playerUuid);
        if (connection != null) {
            connection.setGroup(null);
        }
    }

    /**
     * Clear a Taotie's stomach group (called when all players are released)
     * @param taotieUuid The UUID of the Taotie
     */
    public static void clearStomachGroup(UUID taotieUuid) {
        taotieStomachGroups.remove(taotieUuid);
    }

    @Override
    public String getPluginId() {
        return Noellesroles.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApiInstance) {
            serverApi = serverApiInstance;
        }
        VoicechatPlugin.super.initialize(api);
    }

    public void paranoidEvent(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity spectator = ((ServerPlayerEntity)event.getSenderConnection().getPlayer().getPlayer());
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(spectator.getWorld());
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(spectator);
        if (SpectatorStateHelper.isRealSpectator(spectator)) {
            spectator.getWorld().getPlayers().forEach((p) -> {
                if (gameWorldComponent.isRole(p, Noellesroles.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES) && GameFunctions.isPlayerPlayingAndAlive(p)) {
                    if (spectator.distanceTo(p) <= api.getVoiceChatDistance()) {
                        VoicechatConnection con = api.getConnectionOf(p.getUuid());
                        api.sendLocationalSoundPacketTo(con, event.getPacket().locationalSoundPacketBuilder()
                                        .position(api.createPosition(p.getX(), p.getY(), p.getZ()))
                                        .distance((float)api.getVoiceChatDistance())
                                        .build());
                    }
                }
            });
        }
    }

    /**
     * Block voice packets to swallowed players from:
     * 1. Real spectators (not swallowed)
     * 2. Players swallowed by other Taotie
     *
     * Swallowed players should only hear:
     * - Players swallowed by the same Taotie
     * - The Taotie who swallowed them
     */
    public <T extends Packet> void blockVoiceToSwallowedPlayers(SoundPacketEvent<T> event) {
        // Get recipient (the player who would receive this sound packet)
        if (event.getReceiverConnection() == null || event.getSenderConnection() == null){
            return;
        }
        ServerPlayerEntity recipient = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
        SwallowedPlayerComponent recipientSwallowed = SwallowedPlayerComponent.KEY.get(recipient);

        // Only process if recipient is swallowed
        if (!recipientSwallowed.isSwallowed()) {
            return;
        }

        // Get speaker
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        SwallowedPlayerComponent speakerSwallowed = SwallowedPlayerComponent.KEY.get(speaker);

        boolean shouldBlock = isShouldBlockVoiceToSwallowed(speaker, speakerSwallowed, recipientSwallowed);

        // Cancel the packet if it should be blocked
        if (shouldBlock) {
            event.cancel();
        }
    }

    private static boolean isShouldBlockVoiceToSwallowed(ServerPlayerEntity speaker, SwallowedPlayerComponent speakerSwallowed, SwallowedPlayerComponent recipientSwallowed) {
        boolean shouldBlock = false;

        // Case 1: Speaker is a real spectator (not swallowed) - block
        if (SpectatorStateHelper.isRealSpectator(speaker)) {
            shouldBlock = true;
        }

        // Case 2: Speaker is swallowed by a different Taotie - block
        if (speakerSwallowed.isSwallowed()) {
            UUID recipientTaotie = recipientSwallowed.getSwallowedBy();
            UUID speakerTaotie = speakerSwallowed.getSwallowedBy();
            if (recipientTaotie != null && speakerTaotie != null && !recipientTaotie.equals(speakerTaotie)) {
                shouldBlock = true;
            }
        }
        return shouldBlock;
    }

    /**
     * Taotie voice system:
     * - Swallowed players are in an ISOLATED Group, can talk to each other, outsiders can't hear
     * - Forward swallowed player voice to Taotie
     * - Forward Taotie voice to swallowed players
     */
    public void taotieVoiceEvent(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        //Swallowed player speaks -> forward to Taotie
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(speaker);
        if (swallowedComp.isSwallowed()) {
            UUID taotieUuid = swallowedComp.getSwallowedBy();
            if (taotieUuid != null) {
                PlayerEntity taotie = speaker.getWorld().getPlayerByUuid(taotieUuid);
                if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    VoicechatConnection taotieCon = api.getConnectionOf(taotieUuid);
                    if (taotieCon != null) {
                        // Forward voice to Taotie at Taotie's position
                        api.sendLocationalSoundPacketTo(taotieCon, event.getPacket()
                                .locationalSoundPacketBuilder()
                                .position(api.createPosition(taotie.getX(), taotie.getY(), taotie.getZ()))
                                .distance((float) api.getVoiceChatDistance())
                                .build());
                    }
                }
            }
        }
    }

    /**
     * Handle VoiceChat group removal events
     * When a Taotie stomach group is auto-deleted (no members), clear the reference
     */
    public void onGroupRemoved(RemoveGroupEvent event) {
        Group removedGroup = event.getGroup();
        if (removedGroup == null) return;

        // Remove from our map if it exists
        taotieStomachGroups.entrySet().removeIf(entry -> entry.getValue().equals(removedGroup));
    }

    /**
     * 对局内禁止玩家自行创建语音组，避免绕过当前语音限制。
     * 注意：模组内部通过服务端 API 创建胃部组时同样会触发本事件
     * （setGroup 对未知组会走 addGroup -> CreateGroupEvent），因此需要放行。
     */
    public void onGroupCreated(CreateGroupEvent event) {
        if (taotieStomachGroups.containsValue(event.getGroup())) {
            return;
        }
        if (shouldBlockManualGroupCreation(event.getConnection())) {
            event.cancel();
        }
    }

    /**
     * 对局内禁止存活玩家加入非模组维护的语音组。
     * 死后系统自动加入死人语音组需要保留，因此不拦真实旁观者。
     */
    public void onGroupJoined(JoinGroupEvent event) {
        if (shouldBlockManualGroupJoin(event.getConnection(), event.getGroup())) {
            event.cancel();
        }
    }

    private static boolean shouldBlockManualGroupCreation(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null || connection.getPlayer().getPlayer() == null) {
            return false;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) connection.getPlayer().getPlayer();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return false;
        }

        return GameFunctions.isPlayerPlayingAndAlive(player)
                || SpectatorStateHelper.isInGameRealSpectator(player, gameWorld);
    }

    private static boolean shouldBlockManualGroupJoin(VoicechatConnection connection, Group group) {
        if (connection == null || connection.getPlayer() == null || connection.getPlayer().getPlayer() == null) {
            return false;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) connection.getPlayer().getPlayer();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return false;
        }

        if (taotieStomachGroups.containsValue(group)) {
            return false;
        }

        return GameFunctions.isPlayerPlayingAndAlive(player);
    }

    /**
     * 大嗓门广播：广播期间将语音发送给所有玩家
     * 被沉默时不生效；死亡后不生效；被吞噬时不生效
     */
    public void noisemakerBroadcastEvent(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        if (event.getSenderConnection().getPlayer() == null) return;
        if (event.getSenderConnection().getPlayer().getPlayer() == null) return;

        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();

        // 被沉默时广播不生效
        SilencedPlayerComponent silencedComp = SilencedPlayerComponent.KEY.get(speaker);
        if (silencedComp.isSilenced()) return;

        // 被吞噬时广播不生效
        if (SwallowedPlayerComponent.isPlayerSwallowed(speaker)) return;

        // 死亡后广播不生效
        if (!GameFunctions.isPlayerPlayingAndAlive(speaker)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(speaker.getWorld());
        if (!gameWorldComponent.isRole(speaker, Noellesroles.NOISEMAKER)) return;

        NoisemakerPlayerComponent noisemakerComp = NoisemakerPlayerComponent.KEY.get(speaker);
        if (!noisemakerComp.isBroadcasting()) return;

        // 广播语音给所有玩家（在接收者位置播放，相当于直接在耳边听到）
        for (ServerPlayerEntity receiver : speaker.getServer().getPlayerManager().getPlayerList()) {
            if (receiver.equals(speaker)) continue;
            VoicechatConnection receiverCon = api.getConnectionOf(receiver.getUuid());
            if (receiverCon != null) {
                api.sendLocationalSoundPacketTo(receiverCon, event.getPacket().locationalSoundPacketBuilder()
                        .position(api.createPosition(receiver.getX(), receiver.getY(), receiver.getZ()))
                        .distance((float) api.getVoiceChatDistance())
                        .build());
            }
        }
    }

    /**
     * Block voice packets from silenced players (they cannot speak)
     * Also block voice packets TO silenced players (they cannot hear)
     */
    public void silencerVoiceEvent(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        if (event.getSenderConnection().getPlayer() == null) return;
        if (event.getSenderConnection().getPlayer().getPlayer() == null) return;

        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        if (speaker == null) return;

        // Block silenced players from speaking
        SilencedPlayerComponent speakerSilenced = SilencedPlayerComponent.KEY.get(speaker);
        if (speakerSilenced.isSilenced()) {
            event.cancel();
        }
    }
    public <T extends Packet> void blockVoiceToSilencedPlayers(SoundPacketEvent<T> event) {
        if (event.getReceiverConnection() != null
                && event.getReceiverConnection().getPlayer() != null
                && event.getReceiverConnection().getPlayer().getPlayer() != null) {
            ServerPlayerEntity recipient = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
            if (recipient != null) {
                SilencedPlayerComponent recipientSilenced = SilencedPlayerComponent.KEY.get(recipient);
                if (recipientSilenced.isSilenced()) {
                    event.cancel();
                }
            }
        }
    }

    public <T extends Packet> void blockVoiceToHiddenHallucinationTargets(SoundPacketEvent<T> event) {
        if (event.getReceiverConnection() == null
                || event.getReceiverConnection().getPlayer() == null
                || event.getReceiverConnection().getPlayer().getPlayer() == null
                || event.getSenderConnection() == null
                || event.getSenderConnection().getPlayer() == null
                || event.getSenderConnection().getPlayer().getPlayer() == null) {
            return;
        }
        ServerPlayerEntity receiver = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        if (HallucinationHelper.shouldHideVoiceFor(receiver, speaker)) {
            event.cancel();
        }
    }
    public void dualRoleSelectionVoiceBlock(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null || event.getSenderConnection().getPlayer() == null) return;
        if (event.getSenderConnection().getPlayer().getPlayer() == null) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(player.getWorld());
        if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.SELECTING) {
            event.cancel();
        }
    }

    public <T extends Packet> void dualRoleSelectionSoundBlock(SoundPacketEvent<T> event) {
        if (event.getReceiverConnection() != null
                && event.getReceiverConnection().getPlayer() != null
                && event.getReceiverConnection().getPlayer().getPlayer() != null) {
            ServerPlayerEntity recipient = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
            DualRoleSelectionWorldComponent dsComp = DualRoleSelectionWorldComponent.KEY.get(recipient.getWorld());
            if (dsComp.getPhase() == DualRoleSelectionWorldComponent.Phase.SELECTING) {
                event.cancel();
            }
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::dualRoleSelectionVoiceBlock);
        registration.registerEvent(EntitySoundPacketEvent.class, this::dualRoleSelectionSoundBlock);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::dualRoleSelectionSoundBlock);
        registration.registerEvent(MicrophonePacketEvent.class, this::paranoidEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::taotieVoiceEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::silencerVoiceEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::noisemakerBroadcastEvent);
        registration.registerEvent(CreateGroupEvent.class, this::onGroupCreated);
        registration.registerEvent(JoinGroupEvent.class, this::onGroupJoined);
        registration.registerEvent(RemoveGroupEvent.class, this::onGroupRemoved);

        registration.registerEvent(EntitySoundPacketEvent.class, this::blockVoiceToSwallowedPlayers);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockVoiceToSwallowedPlayers);

        registration.registerEvent(EntitySoundPacketEvent.class, this::blockVoiceToSilencedPlayers);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockVoiceToSilencedPlayers);
        registration.registerEvent(EntitySoundPacketEvent.class, this::blockVoiceToHiddenHallucinationTargets);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockVoiceToHiddenHallucinationTargets);

        VoicechatPlugin.super.registerEvents(registration);
    }
}

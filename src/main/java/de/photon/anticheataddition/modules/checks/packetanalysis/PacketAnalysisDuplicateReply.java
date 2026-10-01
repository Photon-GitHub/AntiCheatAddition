package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.ChallengeReplyData;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Set;

/**
 * This check makes ensures that only actual server challenges are responded to.
 */
public final class PacketAnalysisDuplicateReply extends ViolationModule
{
    public static final PacketAnalysisDuplicateReply KEEP_ALIVE = new PacketAnalysisDuplicateReply("DuplicateKeepAlive", Kind.KEEP_ALIVE);
    public static final PacketAnalysisDuplicateReply PONG = new PacketAnalysisDuplicateReply("DuplicatePong", Kind.PONG);
    public static final PacketAnalysisDuplicateReply TELEPORT = new PacketAnalysisDuplicateReply("DuplicateTeleportConfirm", Kind.TELEPORT);

    private enum Kind
    {
        KEEP_ALIVE(PacketType.Play.Server.KEEP_ALIVE, PacketType.Play.Client.KEEP_ALIVE, ServerVersion.ALL_SUPPORTED_VERSIONS),
        PONG(PacketType.Play.Server.PING, PacketType.Play.Client.PONG, ServerVersion.MC121_5.getSupVersionsFrom()),
        TELEPORT(PacketType.Play.Server.PLAYER_POSITION_AND_LOOK, PacketType.Play.Client.TELEPORT_CONFIRM, ServerVersion.NON_188_VERSIONS);

        private final PacketType.Play.Server challenge;
        private final PacketType.Play.Client response;
        private final Set<ServerVersion> allowedServers;

        Kind(PacketType.Play.Server challenge, PacketType.Play.Client response, Set<ServerVersion> allowedServers)
        {
            this.challenge = challenge;
            this.response = response;
            this.allowedServers = allowedServers;
        }

        public ChallengeReplyData getChallengeReplyData(User user)
        {
            return switch (this) {
                case KEEP_ALIVE -> user.getData().object.packetAnalysisKeepAliveReplies;
                case PONG -> user.getData().object.packetAnalysisPongReplies;
                case TELEPORT -> user.getData().object.packetAnalysisTeleportReplies;
            };
        }

        /**
         * Get the ID necessary for checking challenge-reply structures.
         */
        public long getID(PacketReceiveEvent event)
        {
            return switch (this) {
                case KEEP_ALIVE -> new WrapperPlayClientKeepAlive(event).getId();
                case PONG -> new WrapperPlayClientPong(event).getId();
                case TELEPORT -> new WrapperPlayClientTeleportConfirm(event).getTeleportId();
            };
        }

        /**
         * Get the ID necessary for checking challenge-reply structures.
         */
        public long getID(PacketSendEvent event)
        {
            return switch (this) {
                case KEEP_ALIVE -> new WrapperPlayServerKeepAlive(event).getId();
                case PONG -> new WrapperPlayServerPing(event).getId();
                case TELEPORT -> new WrapperPlayServerPlayerPositionAndLook(event).getTeleportId();
            };
        }
    }

    private final Kind kind;

    private PacketAnalysisDuplicateReply(String name, Kind kind)
    {
        super("PacketAnalysis.parts." + name);
        this.kind = kind;
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(kind.allowedServers)
                           .addPacketListeners(PacketAdapterBuilder.of(this, kind.challenge, kind.response)
                                                                   .priority(PacketListenerPriority.MONITOR)
                                                                   .onReceivingRaw(this::onPacketReceive)
                                                                   .onSendingRaw(this::onPacketSend)
                                                                   .build())
                           .build();
    }

    private void onPacketSend(PacketSendEvent event)
    {
        final User user = User.getUser(event);

        if (user == null) return;
        final ChallengeReplyData data = kind.getChallengeReplyData(user);
        if (User.isUserInvalid(user, this)) {
            data.reset();
            return;
        }

        final long id = kind.getID(event);

        // Post tasks see cancellation and wrapper changes made by later listeners, before the packet is forwarded.
        // Capture the original ID now: the post-task buffer may already have been rewritten.
        event.getPostTasks().add(() -> {
            if (event.isCancelled()) return;
            final long sentId = switch (kind) {
                case KEEP_ALIVE -> event.getLastUsedWrapper() instanceof WrapperPlayServerKeepAlive packet ? packet.getId() : id;
                case PONG -> event.getLastUsedWrapper() instanceof WrapperPlayServerPing packet ? packet.getId() : id;
                case TELEPORT -> event.getLastUsedWrapper() instanceof WrapperPlayServerPlayerPositionAndLook packet ? packet.getTeleportId() : id;
            };
            if (!data.issued(sentId)) getManagement().flag(Flag.of(user)
                                                               .setAddedVl(20)
                                                               .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not reply to too many packets for " + kind.response + " | Challenge " + sentId));
        });
    }

    private void onPacketReceive(PacketReceiveEvent event)
    {
        final User user = User.getUser(event);
        if (user == null) return;
        final ChallengeReplyData data = kind.getChallengeReplyData(user);
        if (User.isUserInvalid(user, this)) {
            data.reset();
            return;
        }
        if (event.isCancelled()) return;

        final long id = kind.getID(event);
        if (!data.replied(id)) getManagement().flag(Flag.of(user)
                                                        .setAddedVl(20)
                                                        .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent an unsolicited or repeated " + kind.response + " | Reply " + id));
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}

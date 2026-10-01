package de.photon.anticheataddition.modules.packetanalysis;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.events.ViolationEvent;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisDuplicateHeldSlot;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisDuplicateReply;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisDigging;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisTickBounded;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.subdata.ChallengeReplyData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class DuplicatePacketListenerTest
{
    private User user;

    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @BeforeEach
    void createUser()
    {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        user = new User(player);
    }

    private static PacketListenerAbstract listener(ViolationModule module)
    {
        final var events = PacketEvents.getAPI().getEventManager();
        clearInvocations(events);
        assertTrue(module.getModuleLoader().load());
        final var captured = ArgumentCaptor.forClass(PacketListenerCommon.class);
        verify(events).registerListener(captured.capture());
        return (PacketListenerAbstract) captured.getValue();
    }

    private void receiveDigging(PacketListenerAbstract packets, DiggingAction action, Vector3i position, int face)
    {
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.PLAYER_DIGGING);
        try (var wrappers = mockConstruction(WrapperPlayClientPlayerDigging.class, (wrapper, context) -> {
            when(wrapper.getAction()).thenReturn(action);
            when(wrapper.getBlockPosition()).thenReturn(position);
            when(wrapper.getBlockFaceId()).thenReturn(face);
        })) {
            packets.onPacketReceive(event);
        }
    }

    @Test
    void miningCompletionAcceptsEveryFaceChangeAndConsecutiveBlocks()
    {
        final var module = PacketAnalysisDigging.INSTANCE;
        final var packets = listener(module);
        try (var violations = mockStatic(ViolationEvent.class)) {
            final var violation = mock(ViolationEvent.class);
            when(violation.call()).thenReturn(violation);
            when(violation.isCancelled()).thenReturn(true);
            violations.when(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20)).thenReturn(violation);
            final var placement = mock(PacketReceiveEvent.class);
            when(placement.getPlayer()).thenReturn(user.getPlayer());
            when(placement.getPacketType()).thenReturn(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT);
            for (int startFace = 0; startFace < 6; startFace++) {
                for (int finishFace = 0; finishFace < 6; finishFace++) {
                    final var block = new Vector3i(startFace, 64, finishFace);
                    receiveDigging(packets, DiggingAction.START_DIGGING, block, startFace);
                    receiveDigging(packets, DiggingAction.FINISHED_DIGGING, block, finishFace);
                    // Holding both buttons can replace and finish the same block without START.
                    packets.onPacketReceive(placement);
                    receiveDigging(packets, DiggingAction.FINISHED_DIGGING, block, startFace);
                    packets.onPacketReceive(placement);
                    receiveDigging(packets, DiggingAction.FINISHED_DIGGING, block, finishFace);
                    // Releasing and pressing attack again can also send a fresh START.
                    receiveDigging(packets, DiggingAction.START_DIGGING, block, finishFace);
                    receiveDigging(packets, DiggingAction.FINISHED_DIGGING, block, startFace);
                    // A stale cancellation leaves the target unknown until a new START.
                    receiveDigging(packets, DiggingAction.CANCELLED_DIGGING, block, startFace);
                    receiveDigging(packets, DiggingAction.FINISHED_DIGGING, block, finishFace);
                    receiveDigging(packets, DiggingAction.FINISHED_DIGGING, block, finishFace);
                }
            }
            violations.verifyNoInteractions();
        }
    }

    @Test
    void diggingStillRejectsDifferentBlocksAndInvalidFaces()
    {
        final var module = PacketAnalysisDigging.INSTANCE;
        final var packets = listener(module);
        try (var violations = mockStatic(ViolationEvent.class)) {
            final var violation = mock(ViolationEvent.class);
            when(violation.call()).thenReturn(violation);
            when(violation.isCancelled()).thenReturn(true);
            violations.when(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20)).thenReturn(violation);
            receiveDigging(packets, DiggingAction.START_DIGGING, new Vector3i(1, 64, 0), 1);
            receiveDigging(packets, DiggingAction.FINISHED_DIGGING, new Vector3i(2, 64, 0), 2);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20));
            receiveDigging(packets, DiggingAction.START_DIGGING, new Vector3i(1, 64, 0), 6);
            receiveDigging(packets, DiggingAction.FINISHED_DIGGING, new Vector3i(1, 64, 0), -1);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20), times(3));
        }
    }

    @Test
    void invalidSlotCannotClearPreviousSelection()
    {
        final var data = user.getData().object.packetAnalysisSlotSelection;
        data.record(4);
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.HELD_ITEM_CHANGE);
        try (var wrappers = mockConstruction(WrapperPlayClientHeldItemChange.class,
                (wrapper, context) -> when(wrapper.getSlot()).thenReturn(9))) {
            listener(PacketAnalysisDuplicateHeldSlot.INSTANCE).onPacketReceive(event);
        }
        assertTrue(data.record(4));
    }

    @Test
    void cancelledSelectionStillUpdatesClientHistory()
    {
        final var data = user.getData().object.packetAnalysisSlotSelection;
        data.record(4);
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.HELD_ITEM_CHANGE);
        when(event.isCancelled()).thenReturn(true);
        try (var wrappers = mockConstruction(WrapperPlayClientHeldItemChange.class,
                (wrapper, context) -> when(wrapper.getSlot()).thenReturn(5))) {
            listener(PacketAnalysisDuplicateHeldSlot.INSTANCE).onPacketReceive(event);
        }
        assertTrue(data.record(5));
    }

    @Test
    void cancelledTickEndDelimitsTheStreamWithoutResettingEvidence()
    {
        final var data = user.getData().object.packetAnalysisInputPackets;
        data.recordPacket(1L);
        data.recordPacket(2L);
        data.recordPacket(3L);
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.CLIENT_TICK_END);
        when(event.isCancelled()).thenReturn(true);
        listener(PacketAnalysisTickBounded.PLAYER_INPUT).onPacketReceive(event);
        assertFalse(data.recordPacket(4L));
        assertTrue(data.recordPacket(5L));
    }

    @Test
    void cancelledInputCannotDonateAFreshMeasurementWindow()
    {
        final var data = user.getData().object.packetAnalysisInputPackets;
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.PLAYER_INPUT);
        when(event.isCancelled()).thenReturn(true);
        listener(PacketAnalysisTickBounded.PLAYER_INPUT).onPacketReceive(event);
        assertFalse(data.recordPacket(1L));
        assertFalse(data.recordPacket(2L));
        assertTrue(data.recordPacket(3L));
    }

    @Test
    void respawnCannotEraseChallengeEvidence()
    {
        final var data = user.getData().object.packetAnalysisKeepAliveReplies;
        data.issued(7);
        data.replied(7);
        final var event = mock(PacketSendEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.RESPAWN);
        listener(PacketAnalysisDuplicateReply.KEEP_ALIVE).onPacketSend(event);
        assertFalse(data.replied(7));
    }

    @Test
    void cancelledChallengeDoesNotGrantReplyCredit()
    {
        final var data = user.getData().object.packetAnalysisKeepAliveReplies;
        data.issued(7);
        data.replied(7);
        final var event = mock(PacketSendEvent.class);
        final var tasks = new ArrayList<Runnable>();
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.KEEP_ALIVE);
        when(event.getPostTasks()).thenReturn(tasks);
        try (var wrappers = mockConstruction(WrapperPlayServerKeepAlive.class,
                (wrapper, context) -> when(wrapper.getId()).thenReturn(7L))) {
            listener(PacketAnalysisDuplicateReply.KEEP_ALIVE).onPacketSend(event);
        }
        // Simulate another listener cancelling after ACA's monitor callback.
        when(event.isCancelled()).thenReturn(true);
        tasks.forEach(Runnable::run);
        assertFalse(data.replied(7));
    }

    @Test
    void challengeUsesTheIdActuallyForwardedByLaterListeners()
    {
        final var data = user.getData().object.packetAnalysisKeepAliveReplies;
        data.issued(8);
        data.replied(8);
        final var event = mock(PacketSendEvent.class);
        final var tasks = new ArrayList<Runnable>();
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.KEEP_ALIVE);
        when(event.getPostTasks()).thenReturn(tasks);
        try (var wrappers = mockConstruction(WrapperPlayServerKeepAlive.class,
                (wrapper, context) -> when(wrapper.getId()).thenReturn(7L))) {
            listener(PacketAnalysisDuplicateReply.KEEP_ALIVE).onPacketSend(event);
        }
        final var forwarded = mock(WrapperPlayServerKeepAlive.class);
        when(forwarded.getId()).thenReturn(8L);
        doReturn(forwarded).when(event).getLastUsedWrapper();
        tasks.forEach(Runnable::run);
        assertFalse(data.replied(7));
        assertTrue(data.replied(8));
        assertFalse(data.replied(8));
    }

    @Test
    void allReplyStreamsFlagUnknownAndConsumedIdsButAcceptIssuedCredits()
    {
        final var modules = new PacketAnalysisDuplicateReply[] {
                PacketAnalysisDuplicateReply.KEEP_ALIVE, PacketAnalysisDuplicateReply.PONG, PacketAnalysisDuplicateReply.TELEPORT
        };
        final var types = new PacketType.Play.Client[] {
                PacketType.Play.Client.KEEP_ALIVE, PacketType.Play.Client.PONG, PacketType.Play.Client.TELEPORT_CONFIRM
        };
        final var states = new ChallengeReplyData[] {
                user.getData().object.packetAnalysisKeepAliveReplies, user.getData().object.packetAnalysisPongReplies,
                user.getData().object.packetAnalysisTeleportReplies
        };
        try (var keepAlive = mockConstruction(WrapperPlayClientKeepAlive.class,
                     (wrapper, context) -> when(wrapper.getId()).thenReturn(7L));
             var pong = mockConstruction(WrapperPlayClientPong.class,
                     (wrapper, context) -> when(wrapper.getId()).thenReturn(7));
             var teleport = mockConstruction(WrapperPlayClientTeleportConfirm.class,
                     (wrapper, context) -> when(wrapper.getTeleportId()).thenReturn(7));
             var violations = mockStatic(ViolationEvent.class)) {
            final var violation = mock(ViolationEvent.class);
            when(violation.call()).thenReturn(violation);
            when(violation.isCancelled()).thenReturn(true);
            for (int stream = 0; stream < modules.length; stream++) {
                final var module = modules[stream];
                // The join grace applies only to teleport confirmations.
                if (module == PacketAnalysisDuplicateReply.TELEPORT) user.getTimeMap().at(TimeKey.LOGIN_TIME).setToZero();
                else user.getTimeMap().at(TimeKey.LOGIN_TIME).update();
                violations.when(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20)).thenReturn(violation);
                final var packets = listener(module);
                final var event = mock(PacketReceiveEvent.class);
                when(event.getPlayer()).thenReturn(user.getPlayer());
                when(event.getPacketType()).thenReturn(types[stream]);
                packets.onPacketReceive(event);
                assertTrue(states[stream].issued(7));
                packets.onPacketReceive(event);
                packets.onPacketReceive(event);
                violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20), times(2));
                when(event.isCancelled()).thenReturn(true);
                packets.onPacketReceive(event);
                violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20), times(2));
            }
        }
    }

    @Test
    void teleportJoinGraceConsumesCreditsAndEndsAfterFiveSeconds()
    {
        final var module = PacketAnalysisDuplicateReply.TELEPORT;
        final var data = user.getData().object.packetAnalysisTeleportReplies;
        final var login = user.getTimeMap().at(TimeKey.LOGIN_TIME);
        login.update();
        final var packets = listener(module);
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.TELEPORT_CONFIRM);
        try (var wrappers = mockConstruction(WrapperPlayClientTeleportConfirm.class,
                     (wrapper, context) -> when(wrapper.getTeleportId()).thenReturn(1));
             var violations = mockStatic(ViolationEvent.class)) {
            final var violation = mock(ViolationEvent.class);
            when(violation.call()).thenReturn(violation);
            when(violation.isCancelled()).thenReturn(true);
            violations.when(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20)).thenReturn(violation);
            // The initial teleport was sent before ACA's User existed.
            packets.onPacketReceive(event);
            assertTrue(data.issued(1));
            packets.onPacketReceive(event);
            packets.onPacketReceive(event);
            violations.verifyNoInteractions();

            // Move the login timestamp into the past without sleeping.
            login.setToFuture(-6000);
            packets.onPacketReceive(event);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20));
            assertTrue(data.issued(1));
            packets.onPacketReceive(event);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20));
            packets.onPacketReceive(event);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20), times(2));
        }
    }

    @Test
    void challengeWindowOverflowFlagsOnlyAfterUncancelledSend()
    {
        final var module = PacketAnalysisDuplicateReply.KEEP_ALIVE;
        final var data = user.getData().object.packetAnalysisKeepAliveReplies;
        for (long id = 0; id < 1024; id++) assertTrue(data.issued(id));
        final var event = mock(PacketSendEvent.class);
        final var tasks = new ArrayList<Runnable>();
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.KEEP_ALIVE);
        when(event.getPostTasks()).thenReturn(tasks);
        try (var wrappers = mockConstruction(WrapperPlayServerKeepAlive.class,
                     (wrapper, context) -> when(wrapper.getId()).thenReturn(1024L));
             var violations = mockStatic(ViolationEvent.class)) {
            final var violation = mock(ViolationEvent.class);
            when(violation.call()).thenReturn(violation);
            when(violation.isCancelled()).thenReturn(true);
            violations.when(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20)).thenReturn(violation);
            final var packets = listener(module);
            packets.onPacketSend(event);
            violations.verifyNoInteractions();
            when(event.isCancelled()).thenReturn(true);
            tasks.forEach(Runnable::run);
            violations.verifyNoInteractions();
            tasks.clear();
            when(event.isCancelled()).thenReturn(false);
            packets.onPacketSend(event);
            tasks.forEach(Runnable::run);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 20));
            assertFalse(data.replied(1024));
            for (long id = 0; id < 1024; id++) assertTrue(data.replied(id));
        }
    }

    @Test
    void playerlessEventsAreIgnored()
    {
        assertNull(User.getUser(mock(PacketSendEvent.class)));
        assertNull(User.getUser(mock(PacketReceiveEvent.class)));
        final var event = mock(PacketSendEvent.class);
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.KEEP_ALIVE);
        listener(PacketAnalysisDuplicateReply.KEEP_ALIVE).onPacketSend(event);
    }

    @Test
    void cancelledServerSlotCorrectionCannotEraseEvidence()
    {
        final var data = user.getData().object.packetAnalysisSlotSelection;
        data.record(4);
        final var event = mock(PacketSendEvent.class);
        final var tasks = new ArrayList<Runnable>();
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.HELD_ITEM_CHANGE);
        when(event.getPostTasks()).thenReturn(tasks);
        listener(PacketAnalysisDuplicateHeldSlot.INSTANCE).onPacketSend(event);
        when(event.isCancelled()).thenReturn(true);
        tasks.forEach(Runnable::run);
        assertTrue(data.record(4));
    }

    @Test
    void trustedJoinStillDiscardsAnOldSlotComparison()
    {
        final var data = user.getData().object.packetAnalysisSlotSelection;
        data.record(4);
        final var event = mock(PacketSendEvent.class);
        final var tasks = new ArrayList<Runnable>();
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.JOIN_GAME);
        when(event.getPostTasks()).thenReturn(tasks);
        listener(PacketAnalysisDuplicateHeldSlot.INSTANCE).onPacketSend(event);
        tasks.forEach(Runnable::run);
        assertFalse(data.record(4));
    }
}

package de.photon.anticheataddition.modules.targeting;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.checks.targeting.TargetingRotationStep;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingRotationStepData;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class TargetingRotationStepTest
{
    private static long seedTime = System.nanoTime() + 60_000_000_000L;
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
        // Initialize the real singleton with the test configuration.
        assertEquals("targeting.parts.rotationstep", TargetingRotationStep.INSTANCE.getModuleId());
    }

    @Test
    void reportsUseExistingFlagManagement() throws ReflectiveOperationException
    {
        final var module = spy(TargetingRotationStep.INSTANCE);
        final var management = mock(ViolationManagement.class);
        doReturn(management).when(module).getManagement();
        report(module);
        verify(management).flag(argThat(flag -> flag.getAddedVl() == 15 && flag.getOnCancel() == null));
        assertEquals("anticheataddition.bypass.targeting.parts.rotationstep", module.getBypassPermission());
    }

    @Test
    void cancelledActionsPreserveTheBaselineAndBypassDiscardsIt() throws ReflectiveOperationException
    {
        final User user = Dummy.mockUser();
        final var event = mock(PacketReceiveEvent.class);
        final var packetUser = mock(com.github.retrooper.packetevents.protocol.player.User.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getUser()).thenReturn(packetUser);
        when(packetUser.getClientVersion()).thenReturn(ClientVersion.V_1_21_11);
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.ATTACK);

        final var state = user.getData().object.targetingRotationStepData;
        seed(state);
        when(event.isCancelled()).thenReturn(true);
        dispatch(event);
        assertTrue(state.getStep() > 0);
        when(event.isCancelled()).thenReturn(false);
        seed(state);
        when(user.getPlayer().hasPermission(TargetingRotationStep.INSTANCE.getBypassPermission())).thenReturn(true);
        try {
            dispatch(event);
            assertEquals(0D, state.getStep());
        } finally {
            when(user.getPlayer().hasPermission(TargetingRotationStep.INSTANCE.getBypassPermission())).thenReturn(false);
        }
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void attackThenInteractCannotResetTheModelOrPreventDetection() throws ReflectiveOperationException
    {
        for (final var client : new ClientVersion[]{ClientVersion.V_1_8, ClientVersion.V_1_21_11}) {
            for (final var action : new WrapperPlayClientInteractEntity.InteractAction[]{
                    WrapperPlayClientInteractEntity.InteractAction.INTERACT,
                    WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT}) {
                final User user = Dummy.mockUser();
                final var state = user.getData().object.targetingRotationStepData;
                final float seedYaw = seed(state);
                final var event = event(user, client);
                when(event.getPacketType()).thenReturn(PacketType.Play.Client.INTERACT_ENTITY);
                try (final var wrappers = mockConstruction(WrapperPlayClientInteractEntity.class, (packet, context) -> {
                    when(packet.getAction()).thenReturn(action);
                    when(packet.getEntityId()).thenReturn(42);
                })) {
                    float yaw = seedYaw;
                    long now = seedTime + 159 * 50_000_000L;
                    int reports = 0;
                    for (int episode = 0; episode < 6; ++episode) {
                        state.action();
                        dispatch(event);
                        yaw += 0.173F;
                        if (state.movement(yaw, true, now += 50_000_000L) != null) reports++;
                        for (int i = 0; i < 18; ++i) {
                            dispatch(event);
                            yaw += (float) (0.15D * (1 + i % 13) * (i % 2 == 0 ? 1 : -1));
                            if (state.movement(yaw, true, now += 50_000_000L) != null) reports++;
                        }
                    }
                    assertEquals(1, reports, client + " " + action);
                    assertTrue(wrappers.constructed().size() > 0);
                }
            }
        }
    }

    @Test
    void onlyKnownLegacyHorseInteractionsGrantExemption() throws ReflectiveOperationException
    {
        for (final var client : new ClientVersion[]{ClientVersion.V_1_8, ClientVersion.V_1_21_11}) {
            final User user = Dummy.mockUser();
            final var state = user.getData().object.targetingRotationStepData;
            seed(state);
            state.trackLegacyHorse(42, true);
            final var event = event(user, client);
            when(event.getPacketType()).thenReturn(PacketType.Play.Client.INTERACT_ENTITY);
            try (final var ignored = mockConstruction(WrapperPlayClientInteractEntity.class, (packet, context) -> {
                when(packet.getAction()).thenReturn(WrapperPlayClientInteractEntity.InteractAction.INTERACT);
                when(packet.getEntityId()).thenReturn(42);
            })) {
                dispatch(event);
                assertEquals(client == ClientVersion.V_1_8, state.getStep() == 0);
            }
            state.trackLegacyHorse(42, false);
            assertFalse(state.isLegacyHorse(42));
        }
    }

    @Test
    void weatherAndCancelledServerPacketsDoNotResetEvidence() throws ReflectiveOperationException
    {
        final User user = Dummy.mockUser();
        final var state = user.getData().object.targetingRotationStepData;
        seed(state);
        final var event = mock(PacketSendEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.CHANGE_GAME_STATE);
        try (final var ignored = mockConstruction(WrapperPlayServerChangeGameState.class, (packet, context) ->
                when(packet.getReason()).thenReturn(WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING))) {
            dispatchSend(event);
            assertTrue(state.getStep() > 0);
        }
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.PLAYER_POSITION_AND_LOOK);
        when(event.isCancelled()).thenReturn(true);
        dispatchSend(event);
        assertTrue(state.getStep() > 0);
        when(event.isCancelled()).thenReturn(false);
        dispatchSend(event);
        assertEquals(0D, state.getStep());
    }

    private static void dispatchSend(final PacketSendEvent event) throws ReflectiveOperationException
    {
        final Field field = ModuleLoader.class.getDeclaredField("packetListeners");
        field.setAccessible(true);
        final Set<?> listeners = (Set<?>) field.get(TargetingRotationStep.INSTANCE.getModuleLoader());
        for (final Object listener : listeners) ((PacketListenerAbstract) listener).onPacketSend(event);
    }

    private static PacketReceiveEvent event(final User user, final ClientVersion client)
    {
        final var event = mock(PacketReceiveEvent.class);
        final var packetUser = mock(com.github.retrooper.packetevents.protocol.player.User.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getUser()).thenReturn(packetUser);
        when(packetUser.getClientVersion()).thenReturn(client);
        return event;
    }

    private static void report(final TargetingRotationStep module) throws ReflectiveOperationException
    {
        final var method = TargetingRotationStep.class.getDeclaredMethod("report", User.class, TargetingRotationStepData.Report.class);
        method.setAccessible(true);
        method.invoke(module, Dummy.mockUser(), new TargetingRotationStepData.Report(0.15D, 6));
    }

    private static float seed(final TargetingRotationStepData state)
    {
        state.reset();
        seedTime += 60_000_000_000L;
        float yaw = 0;
        for (int i = 0; i < 160; ++i) {
            yaw += (float) (0.15D * (1 + i % 13) * (i % 2 == 0 ? 1 : -1));
            state.movement(yaw, true, seedTime + i * 50_000_000L);
        }
        assertTrue(state.getStep() > 0);
        return yaw;
    }

    private static void dispatch(final PacketReceiveEvent event) throws ReflectiveOperationException
    {
        final Field field = ModuleLoader.class.getDeclaredField("packetListeners");
        field.setAccessible(true);
        final Set<?> listeners = (Set<?>) field.get(TargetingRotationStep.INSTANCE.getModuleLoader());
        for (final Object listener : listeners) ((PacketListenerAbstract) listener).onPacketReceive(event);
    }
}

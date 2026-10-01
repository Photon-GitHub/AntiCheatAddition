package de.photon.anticheataddition.modules.packetanalysis;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.events.ViolationEvent;
import de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisAnimation;
import de.photon.anticheataddition.user.User;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class AnimationPacketListenerTest
{
    private User user;
    private PacketListenerAbstract listener;

    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @BeforeEach
    void createListener()
    {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        user = new User(player);
        final var events = PacketEvents.getAPI().getEventManager();
        clearInvocations(events);
        assertTrue(PacketAnalysisAnimation.INSTANCE.getModuleLoader().load());
        final var captured = ArgumentCaptor.forClass(PacketListenerCommon.class);
        verify(events).registerListener(captured.capture());
        listener = (PacketListenerAbstract) captured.getValue();
    }

    private void receive(PacketType.Play.Client type)
    {
        final var event = mock(PacketReceiveEvent.class);
        when(event.getPlayer()).thenReturn(user.getPlayer());
        when(event.getPacketType()).thenReturn(type);
        listener.onPacketReceive(event);
    }

    @Test
    void repeatedEntityHitsAcceptAnimation()
    {
        repeatedEntityHitsAcceptSwing(PacketType.Play.Client.ANIMATION);
    }

    @Test
    void repeatedEntityHitsAcceptPunch()
    {
        repeatedEntityHitsAcceptSwing(PacketType.Play.Client.PUNCH);
    }

    private void repeatedEntityHitsAcceptSwing(PacketType.Play.Client swing)
    {
        try (var violations = mockStatic(ViolationEvent.class)) {
            for (int hit = 0; hit < 10; hit++) {
                receive(PacketType.Play.Client.ATTACK);
                assertTrue(user.getData().bool.packetAnalysisAnimationExpected);
                receive(swing);
                assertFalse(user.getData().bool.packetAnalysisAnimationExpected);
            }
            violations.verifyNoInteractions();
        }
    }

    @Test
    void missingSwingStillFlagsAttack()
    {
        missingSwingStillFlags(PacketType.Play.Client.ATTACK);
    }

    @Test
    void missingSwingStillFlagsLegacyAttack()
    {
        missingSwingStillFlags(PacketType.Play.Client.INTERACT_ENTITY);
    }

    private void missingSwingStillFlags(PacketType.Play.Client attack)
    {
        final var module = PacketAnalysisAnimation.INSTANCE;
        try (var wrappers = mockConstruction(WrapperPlayClientInteractEntity.class,
                     (wrapper, context) -> when(wrapper.getAction()).thenReturn(WrapperPlayClientInteractEntity.InteractAction.ATTACK));
             var violations = mockStatic(ViolationEvent.class)) {
            final var violation = mock(ViolationEvent.class);
            when(violation.call()).thenReturn(violation);
            when(violation.isCancelled()).thenReturn(true);
            violations.when(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 30)).thenReturn(violation);
            receive(attack);
            violations.verifyNoInteractions();
            receive(attack);
            violations.verify(() -> ViolationEvent.build(user.getPlayer(), module.getModuleId(), 30));
        }
    }

    @Test
    void legacyEntityHitsAcceptAnimation()
    {
        legacyEntityHitsAcceptSwing(PacketType.Play.Client.ANIMATION);
    }

    @Test
    void legacyEntityHitsAcceptPunch()
    {
        legacyEntityHitsAcceptSwing(PacketType.Play.Client.PUNCH);
    }

    private void legacyEntityHitsAcceptSwing(PacketType.Play.Client swing)
    {
        try (var wrappers = mockConstruction(WrapperPlayClientInteractEntity.class,
                     (wrapper, context) -> when(wrapper.getAction()).thenReturn(WrapperPlayClientInteractEntity.InteractAction.ATTACK));
             var violations = mockStatic(ViolationEvent.class)) {
            receive(PacketType.Play.Client.INTERACT_ENTITY);
            receive(swing);
            receive(PacketType.Play.Client.INTERACT_ENTITY);
            receive(swing);
            assertFalse(user.getData().bool.packetAnalysisAnimationExpected);
            violations.verifyNoInteractions();
        }
    }
}

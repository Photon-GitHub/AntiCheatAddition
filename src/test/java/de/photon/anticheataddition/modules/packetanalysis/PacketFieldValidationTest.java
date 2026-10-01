package de.photon.anticheataddition.modules.packetanalysis;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import de.photon.anticheataddition.Dummy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisHorseJump.invalidActionParameter;
import static de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisDroppingPayload.invalidDroppingPayload;
import static de.photon.anticheataddition.modules.checks.packetanalysis.PacketAnalysisHeldSlot.invalidHeldSlot;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PacketFieldValidationTest
{
    @BeforeAll
    static void setup()
    {
        // Predicates now live on singleton modules, whose initialization reads the plugin config.
        Dummy.mockAntiCheatAddition();
    }

    private static final EnumSet<DiggingAction> NON_BLOCK_ACTIONS = EnumSet.of(DiggingAction.DROP_ITEM,
                                                                               DiggingAction.DROP_ITEM_STACK, DiggingAction.RELEASE_USE_ITEM, DiggingAction.SWAP_ITEM_WITH_OFFHAND);

    @Test
    void acceptsEveryHotbarSlotIncludingRepeatedSelections()
    {
        for (int repetition = 0; repetition < 100; ++repetition) {
            for (int slot = 0; slot < 9; ++slot) assertFalse(invalidHeldSlot(slot));
        }
    }

    @Test
    void rejectsEveryOtherSlotEncodableAsAShort()
    {
        for (int slot = Short.MIN_VALUE; slot <= Short.MAX_VALUE; ++slot) {
            if (slot < 0 || slot > 8) assertTrue(invalidHeldSlot(slot));
        }
    }

    @Test
    void allowsFullSignedVanillaHorseJumpRange()
    {
        for (int boost = -100; boost <= 100; ++boost) {
            assertFalse(invalidActionParameter(Action.START_JUMPING_WITH_HORSE, boost));
        }
    }

    @Test
    void rejectsHorseJumpOverflowIncludingMinInteger()
    {
        for (final int boost : new int[]{Integer.MIN_VALUE, -101, 101, Integer.MAX_VALUE}) {
            assertTrue(invalidActionParameter(Action.START_JUMPING_WITH_HORSE, boost));
        }
    }

    @Test
    void otherKnownActionsOnlyAllowZeroRegardlessOfOrder()
    {
        for (final Action action : Action.values()) {
            if (action == Action.START_JUMPING_WITH_HORSE) continue;
            assertFalse(invalidActionParameter(action, 0));
            for (final int boost : new int[]{Integer.MIN_VALUE, -100, -1, 1, 100, Integer.MAX_VALUE}) {
                assertTrue(invalidActionParameter(action, boost));
            }
        }
        assertFalse(invalidActionParameter(null, 1000));
    }

    @Test
    void acceptsCanonicalNonBlockPayloadAcrossKnownClientVersions()
    {
        for (final ClientVersion version : ClientVersion.values()) {
            for (final DiggingAction action : NON_BLOCK_ACTIONS) {
                assertFalse(invalidDroppingPayload(action, new Vector3i(0, 0, 0), 0, version));
            }
        }
    }

    @Test
    void detectsEachNonzeroCoordinateIndependently()
    {
        for (final DiggingAction action : NON_BLOCK_ACTIONS) {
            for (final Vector3i position : new Vector3i[]{new Vector3i(1, 0, 0), new Vector3i(-1, 0, 0),
                    new Vector3i(0, 1, 0), new Vector3i(0, -1, 0), new Vector3i(0, 0, 1), new Vector3i(0, 0, -1)}) {
                assertTrue(invalidDroppingPayload(action, position, 0, ClientVersion.V_1_8));
            }
        }
    }

    @Test
    void rejectsNonzeroFacesEvenWhenTheyWouldBeValidForBlockDigging()
    {
        for (final DiggingAction action : NON_BLOCK_ACTIONS) {
            for (int face = 1; face <= 255; ++face) {
                assertTrue(invalidDroppingPayload(action, new Vector3i(0, 0, 0), face, ClientVersion.V_1_8));
            }
        }
    }

    @Test
    void releaseRequiresZeroFaceOnSupportedProtocols()
    {
        for (final ClientVersion version : new ClientVersion[]{ClientVersion.V_1_8, ClientVersion.V_1_21_11}) {
            assertTrue(invalidDroppingPayload(DiggingAction.RELEASE_USE_ITEM, new Vector3i(0, 0, 0), 255, version));
            assertFalse(invalidDroppingPayload(DiggingAction.RELEASE_USE_ITEM, new Vector3i(0, 0, 0), 0, version));
            assertTrue(invalidDroppingPayload(DiggingAction.DROP_ITEM, new Vector3i(0, 0, 0), 255, version));
            assertTrue(invalidDroppingPayload(DiggingAction.RELEASE_USE_ITEM, new Vector3i(0, 1, 0), 255, version));
        }
    }

    @Test
    void blockDiggingAndStabAreOutsideThePayloadRule()
    {
        for (final DiggingAction action : EnumSet.complementOf(NON_BLOCK_ACTIONS)) {
            assertFalse(invalidDroppingPayload(action, new Vector3i(123, -64, -321), 5, ClientVersion.V_1_21_11));
        }
        assertFalse(invalidDroppingPayload(null, new Vector3i(1, 2, 3), 5, ClientVersion.V_1_8));
    }

    @Test
    void unknownProtocolAndUndecodedPositionAreNotEvidenceOfCheating()
    {
        assertFalse(invalidDroppingPayload(DiggingAction.RELEASE_USE_ITEM, new Vector3i(1, 2, 3), 5, null));
        assertFalse(invalidDroppingPayload(DiggingAction.RELEASE_USE_ITEM, new Vector3i(1, 2, 3), 5, ClientVersion.UNKNOWN));
        assertFalse(invalidDroppingPayload(DiggingAction.RELEASE_USE_ITEM, null, 5, ClientVersion.V_1_8));
    }
}

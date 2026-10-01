package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FlagTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void testRunApplicableActions()
    {
        final var user = Dummy.mockUser();

        // Create a Flag object and set its properties
        Flag flag = Flag.of(user);
        flag.setAddedVl(2);
        flag.setCancelAction(3, () -> {
            // Define the action to be taken when the cancelVl is reached
            throw new IllegalStateException();
        });

        Assertions.assertDoesNotThrow(() -> flag.runApplicableActions(0));
        Assertions.assertThrows(IllegalStateException.class, () -> flag.runApplicableActions(1));
    }

    @Test
    void cancelVlZeroDoesNotRunCancelAction()
    {
        final var user = Dummy.mockUser();

        Flag flag = Flag.of(user);
        flag.setCancelAction(0, () -> {
            throw new IllegalStateException();
        });

        Assertions.assertDoesNotThrow(() -> flag.runApplicableActions(100));
    }
}

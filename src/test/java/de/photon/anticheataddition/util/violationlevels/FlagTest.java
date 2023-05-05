package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FlagTest
{
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
}

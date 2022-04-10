package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntBiFunction;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
final class ScaffoldRotationSecondDerivative extends Module
{
    private static final double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @Getter
    private ToIntBiFunction<User, Double> applyingConsumer = (user, angleInformation) -> 0;

    ScaffoldRotationSecondDerivative(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Rotation.SecondDerivative");
    }


    @Override
    public void enable()
    {
        applyingConsumer = (user, angleInformation) -> {
            if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD) {
                DebugSender.INSTANCE.sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
                return 5;
            }
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, angleInformation) -> 0;
    }
}

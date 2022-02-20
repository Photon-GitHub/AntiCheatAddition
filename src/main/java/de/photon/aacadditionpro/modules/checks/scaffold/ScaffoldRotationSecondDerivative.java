package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntBiFunction;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
class ScaffoldRotationSecondDerivative extends Module
{
    private static final double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @Getter
    private ToIntBiFunction<User, Double> applyingConsumer = (user, angleInformation) -> 0;

    public ScaffoldRotationSecondDerivative(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Rotation.SecondDerivative");
    }


    @Override
    public void enable()
    {
        applyingConsumer = (user, angleInformation) -> {
            if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD) {
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
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

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntBiFunction;

/**
 * This pattern detects huge angle changes while scaffolding that
 * do not reflect legit behaviour.
 */
final class ScaffoldRotationDerivative extends Module
{
    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 7D;

    @Getter
    private ToIntBiFunction<User, Double> applyingConsumer = (user, angleInformation) -> 0;

    ScaffoldRotationDerivative(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Rotation.Derivative");
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, angleInformation) -> {
            if (angleInformation > ANGLE_CHANGE_SUM_THRESHOLD) {
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotation changes.");
                return 10;
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

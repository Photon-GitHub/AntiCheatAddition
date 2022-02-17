package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.oldmessaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntBiFunction;

/**
 * This pattern detects huge angle changes while scaffolding that
 * do not reflect legit behaviour.
 */
class ScaffoldRotationDerivative extends Module
{
    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 7D;

    @Getter
    private ToIntBiFunction<User, Double> applyingConsumer = (user, angleInformation) -> 0;

    public ScaffoldRotationDerivative(String scaffoldConfigString)
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

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}

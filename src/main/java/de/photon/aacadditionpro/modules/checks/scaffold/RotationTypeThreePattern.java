package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.ToIntBiFunction;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
class RotationTypeThreePattern implements Module
{
    @Getter
    private static final RotationTypeThreePattern instance = new RotationTypeThreePattern();

    private static final double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @Getter
    private ToIntBiFunction<User, Float> applyingConsumer = (user, angleInformation) -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = (user, angleInformation) -> {
            if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD) {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
                return 1;
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
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.rotation.type3";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

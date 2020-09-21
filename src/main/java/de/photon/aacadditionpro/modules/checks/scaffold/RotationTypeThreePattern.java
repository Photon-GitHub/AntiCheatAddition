package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.BiFunction;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
class RotationTypeThreePattern implements Module
{
    @Getter
    private static final RotationTypeThreePattern instance = new RotationTypeThreePattern();

    private static final double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @Getter
    private BiFunction<User, Float, Integer> applyingConsumer = (user, angleInformation) -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = (user, angleInformation) -> {
            if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD) {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
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

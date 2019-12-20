package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
class RotationTypeThreePattern extends PatternModule.Pattern<User, Float>
{
    private final static double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @Override
    protected int process(User user, Float angleInformation)
    {
        if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD) {
            message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3";
            return 1;
        }
        return 0;
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

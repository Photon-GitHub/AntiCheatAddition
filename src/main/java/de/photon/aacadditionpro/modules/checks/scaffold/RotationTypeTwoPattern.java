package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.Pattern} detects huge angle changes while scaffolding that
 * do not reflect legit behaviour.
 */
class RotationTypeTwoPattern extends PatternModule.Pattern<User, Float>
{
    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 7D;

    @Override
    protected int process(User user, Float angleInformation)
    {
        if (angleInformation > ANGLE_CHANGE_SUM_THRESHOLD) {
            message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 2";
            return 2;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.rotation.type2";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

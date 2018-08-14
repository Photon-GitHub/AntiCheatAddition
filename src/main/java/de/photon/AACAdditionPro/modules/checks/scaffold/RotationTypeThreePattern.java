package de.photon.AACAdditionPro.modules.checks.scaffold;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;

class RotationTypeThreePattern extends PatternModule.Pattern<User, Float>
{
    private final static double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @Override
    protected int process(User user, Float angleInformation)
    {
        if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD)
        {
            VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
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

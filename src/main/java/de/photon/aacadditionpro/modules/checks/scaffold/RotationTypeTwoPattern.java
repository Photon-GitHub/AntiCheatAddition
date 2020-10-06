package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.ToIntBiFunction;

/**
 * This pattern detects huge angle changes while scaffolding that
 * do not reflect legit behaviour.
 */
class RotationTypeTwoPattern implements Module
{
    @Getter
    private static final RotationTypeTwoPattern instance = new RotationTypeTwoPattern();

    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 7D;

    @Getter
    private ToIntBiFunction<User, Float> applyingConsumer = (user, angleInformation) -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = (user, angleInformation) -> {
            if (angleInformation > ANGLE_CHANGE_SUM_THRESHOLD) {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 2");
                return 2;
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
        return this.getModuleType().getConfigString() + ".parts.rotation.type2";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

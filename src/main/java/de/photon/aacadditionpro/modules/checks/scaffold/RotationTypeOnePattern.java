package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.Function;

/**
 * This pattern detects significant rotation "jumps" in the last two ticks.
 */
class RotationTypeOnePattern implements Module
{
    @Getter
    private static final RotationTypeOnePattern instance = new RotationTypeOnePattern();

    @Getter
    private Function<User, Integer> applyingConsumer = user -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.getTimestampMap().recentlyUpdated(TimestampKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE, 125)) {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 1");
                return 3;
            }
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = user -> 0;
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.rotation.type1";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

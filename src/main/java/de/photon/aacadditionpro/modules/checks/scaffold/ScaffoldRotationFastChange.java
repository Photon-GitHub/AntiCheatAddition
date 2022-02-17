package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.oldmessaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntFunction;


/**
 * This pattern detects significant rotation "jumps" in the last two ticks.
 */
class ScaffoldRotationFastChange extends Module
{
    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    public ScaffoldRotationFastChange(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Rotation.FastChange");
    }

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.getTimestampMap().at(TimestampKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).recentlyUpdated(125)) {
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent fast rotation changes.");
                return 15;
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
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}

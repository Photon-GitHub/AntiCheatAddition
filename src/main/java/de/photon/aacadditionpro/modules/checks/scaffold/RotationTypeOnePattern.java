package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern detects significant rotation "jumps" in the last two ticks.
 */
class RotationTypeOnePattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @Override
    protected int process(User user, BlockPlaceEvent event)
    {
        if (user.getTimestampMap().recentlyUpdated(TimestampKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE, 125)) {
            message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 1";
            return 3;
        }
        return 0;
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

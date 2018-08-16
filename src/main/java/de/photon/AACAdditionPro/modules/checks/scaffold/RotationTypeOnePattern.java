package de.photon.AACAdditionPro.modules.checks.scaffold;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern detects significant rotation "jumps" in the last two ticks.
 */
class RotationTypeOnePattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @Override
    protected int process(User user, BlockPlaceEvent event)
    {
        if (user.getLookPacketData().recentlyUpdated(0, 125))
        {
            VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 1");
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

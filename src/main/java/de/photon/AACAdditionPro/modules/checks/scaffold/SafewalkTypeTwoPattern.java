package de.photon.AACAdditionPro.modules.checks.scaffold;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
class SafewalkTypeTwoPattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @Override
    protected int process(User user, BlockPlaceEvent event)
    {
        // Moved recently
        if (user.getPositionData().hasPlayerMovedRecently(355, PositionData.MovementType.XZONLY) &&
            // Suddenly stopped
            !user.getPositionData().hasPlayerMovedRecently(175, PositionData.MovementType.XZONLY) &&
            // Has not sneaked recently
            !(user.getPositionData().hasPlayerSneakedRecently(175) && user.getPositionData().getLastSneakTime() > 148))
        {
            VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)");
            return 2;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.safewalk.type2";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

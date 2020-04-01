package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
class PositionPattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @Override
    public int process(User user, BlockPlaceEvent event)
    {
        final double xOffset = MathUtils.offset(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
        final double zOffset = MathUtils.offset(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());

        boolean flag;
        switch (event.getBlock().getFace(event.getBlockAgainst())) {
            case EAST:
                flag = xOffset <= 0;
                break;
            case WEST:
                flag = xOffset <= 1;
                break;
            case NORTH:
                flag = zOffset <= 1;
                break;
            case SOUTH:
                flag = zOffset <= 0;
                break;
            default:
                // Some other, mostly weird blockplaces.
                flag = false;
                break;
        }

        if (flag) {
            message = "Scaffold-Verbose | Player: " + event.getPlayer().getName() + " placed from a suspicious location.";
            return 5;
        }

        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.position";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

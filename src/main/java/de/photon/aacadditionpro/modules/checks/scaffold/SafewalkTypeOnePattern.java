package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.Pattern} detects suspicious stops right before the edges
 * of {@link org.bukkit.block.Block}s.
 */
class SafewalkTypeOnePattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Override
    protected int process(User user, BlockPlaceEvent event)
    {
        // Moved to the edge of the block
        if (user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 175) &&
            // Not sneaked recently. The sneaking must endure some time to prevent bypasses.
            !(user.hasSneakedRecently(125) && user.getDataMap().getLong(DataKey.LAST_SNEAK_DURATION) > 148))
        {

            final double xOffset = MathUtils.offset(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
            final double zOffset = MathUtils.offset(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());

            boolean sneakBorder;
            switch (event.getBlock().getFace(event.getBlockAgainst())) {
                case EAST:
                    sneakBorder = xOffset > 0.28D && xOffset < 0.305D;
                    break;
                case WEST:
                    sneakBorder = xOffset > 1.28D && xOffset < 1.305D;
                    break;
                case NORTH:
                    sneakBorder = zOffset > 1.28D && zOffset < 1.305D;
                    break;
                case SOUTH:
                    sneakBorder = zOffset > 0.28D && zOffset < 0.305D;
                    break;
                default:
                    // Some other, mostly weird blockplaces.
                    sneakBorder = false;
                    break;
            }

            if (sneakBorder) {
                if (++user.getScaffoldData().safewalkTypeOneFails >= this.violationThreshold) {
                    message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 1)";
                    return 3;
                }
            } else {
                user.getScaffoldData().safewalkTypeOneFails = 0;
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.safewalk.type1";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

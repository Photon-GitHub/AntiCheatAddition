package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern detects suspicious stops right before the edges
 * of {@link org.bukkit.block.Block}s.
 */
class ScaffoldSafewalkPosition extends Module
{
    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    public ScaffoldSafewalkPosition(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Safewalk.Position");
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            // Moved to the edge of the block
            if (user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 175) &&
                // Not sneaked recently. The sneaking must endure some time to prevent bypasses.
                !(user.hasSneakedRecently(125) && user.getDataMap().getLong(DataKey.LongKey.LAST_SNEAK_DURATION) > 148))
            {
                val xOffset = MathUtil.absDiff(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
                val zOffset = MathUtil.absDiff(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());
                val face = event.getBlock().getFace(event.getBlockAgainst());

                // Not building in a straight line.
                if (face == null || event.getBlockAgainst().getRelative(face).isEmpty()) return 0;

                boolean sneakBorder;
                switch (face) {
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
                    if (user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SAFEWALK_POSITION_FAILS).incrementCompareThreshold()) {
                        DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Position)");
                        return 15;
                    }
                } else user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SAFEWALK_POSITION_FAILS).setToZero();
            }
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, event) -> 0;
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}

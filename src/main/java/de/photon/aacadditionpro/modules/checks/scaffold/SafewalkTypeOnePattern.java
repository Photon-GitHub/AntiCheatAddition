package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.DataKey;
import de.photon.aacadditionproold.user.TimestampKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.mathematics.MathUtils;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern detects suspicious stops right before the edges
 * of {@link org.bukkit.block.Block}s.
 */
class SafewalkTypeOnePattern implements Module
{
    @Getter
    private static final SafewalkTypeOnePattern instance = new SafewalkTypeOnePattern();

    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
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
                        VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 1)");
                        return 3;
                    }
                } else {
                    user.getScaffoldData().safewalkTypeOneFails = 0;
                }
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
    public boolean isSubModule()
    {
        return true;
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

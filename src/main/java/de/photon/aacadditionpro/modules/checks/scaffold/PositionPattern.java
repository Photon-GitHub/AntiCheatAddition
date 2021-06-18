package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.util.mathematics.MathUtils;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
class PositionPattern implements Module
{
    @Getter
    private static final PositionPattern instance = new PositionPattern();
    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            // This sorts out scaffolding with non-full block hitboxes that will cause false positives (e.g. fences).
            if (!event.getBlockPlaced().getType().isOccluding()) {
                return 0;
            }

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
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed from a suspicious location.");
                return 5;
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
        return this.getModuleType().getConfigString() + ".parts.position";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

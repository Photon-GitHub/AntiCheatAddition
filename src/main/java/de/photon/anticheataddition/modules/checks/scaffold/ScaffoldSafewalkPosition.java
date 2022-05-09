package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern detects suspicious stops right before the edges
 * of {@link org.bukkit.block.Block}s.
 */
final class ScaffoldSafewalkPosition extends Module
{
    private static final int MIN_SNEAK_BYPASS_MILLIS = 148;

    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    ScaffoldSafewalkPosition(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Safewalk.Position");
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            // A non-moving player is not of interest.
            if (!user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 175) ||
                // Long sneak durations are bypassed as this check mainly targets safewalk or similar mods that start sneaking at the edge.
                user.hasSneakedRecently(125) && user.getDataMap().getLong(DataKey.Long.LAST_SNEAK_DURATION) > MIN_SNEAK_BYPASS_MILLIS ||
                // If the player is still sneaking and started long ago they are also bypassed.
                user.getPlayer().isSneaking() && user.getTimeMap().at(TimeKey.SNEAK_ENABLE).passedTime() > MIN_SNEAK_BYPASS_MILLIS) return 0;

            final double xOffset = MathUtil.absDiff(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
            final double zOffset = MathUtil.absDiff(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());
            val face = event.getBlock().getFace(event.getBlockAgainst());

            // Not building in a straight line.
            if (face == null || event.getBlockAgainst().getRelative(face).isEmpty()) return 0;

            boolean sneakBorder = switch (face) {
                case EAST -> xOffset > 0.28D && xOffset < 0.305D;
                case WEST -> xOffset > 1.28D && xOffset < 1.305D;
                case NORTH -> zOffset > 1.28D && zOffset < 1.305D;
                case SOUTH -> zOffset > 0.28D && zOffset < 0.305D;
                // Some other, weird block placement.
                default -> false;
            };
            // Moved to the edge of the block

            if (sneakBorder) {
                if (user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_SAFEWALK_POSITION_FAILS).incrementCompareThreshold()) {
                    Log.fine(() -> "Scaffold-Debug | Player: %s has behaviour associated with safe-walk. (Position) | Face: %s | xOffset: %f | zOffset: %f".formatted(user.getPlayer().getName(), face, xOffset, zOffset));
                    return 15;
                }
            } else user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_SAFEWALK_POSITION_FAILS).setToZero();
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, event) -> 0;
    }
}

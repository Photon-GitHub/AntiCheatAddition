package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.function.ToIntBiFunction;

final class ScaffoldAngle extends Module
{
    private static final double MAX_ANGLE = Math.toRadians(90);

    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    ScaffoldAngle(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Angle");
    }


    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            val placedFace = event.getBlock().getFace(event.getBlockAgainst());
            val placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());

            // If greater than 90 in radians.
            if (user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_ANGLE_FAILS).conditionallyIncDec(user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE)) {
                AntiCheatAddition.getInstance().getLogger().fine("Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
                return 15;
            }
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, event) -> 0;
    }
}

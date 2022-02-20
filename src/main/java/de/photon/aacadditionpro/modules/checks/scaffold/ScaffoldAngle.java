package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.function.ToIntBiFunction;

class ScaffoldAngle extends Module
{
    private static final double MAX_ANGLE = Math.toRadians(90);

    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    public ScaffoldAngle(String scaffoldConfigString)
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
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
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

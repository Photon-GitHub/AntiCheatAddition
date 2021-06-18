package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.function.ToIntBiFunction;

class AnglePattern extends Module
{
    private static final double MAX_ANGLE = Math.toRadians(90);

    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    public AnglePattern(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.angle");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            val placedFace = event.getBlock().getFace(event.getBlockAgainst());
            val placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());

            // If greater than 90 in radians.
            if (user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE) {
                if (user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_ANGLE_FAILS).incrementCompareThreshold()) {
                    DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
                }
            }


            if (user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE) {
                if (++user.getScaffoldData().angleFails >= this.violationThreshold) {
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
                    return 3;
                }
            } else if (user.getScaffoldData().angleFails > 0) {
                --user.getScaffoldData().angleFails;
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
        return this.getModuleType().getConfigString() + ".parts.angle";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

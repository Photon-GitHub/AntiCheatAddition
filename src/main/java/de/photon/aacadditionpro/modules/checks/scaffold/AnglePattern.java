package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.function.ToIntBiFunction;

class AnglePattern implements Module
{
    @Getter
    private static final AnglePattern instance = new AnglePattern();

    private static final double MAX_ANGLE = Math.toRadians(90);

    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    protected int apply(User user, BlockPlaceEvent event)
    {
        final BlockFace placedFace = event.getBlock().getFace(event.getBlockAgainst());
        final Vector placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());

        // If greater than 90
        if (user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE) {
            if (++user.getScaffoldData().angleFails >= this.violationThreshold) {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
                return 3;
            }
        } else if (user.getScaffoldData().angleFails > 0) {
            user.getScaffoldData().angleFails--;
        }
        return 0;
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            final BlockFace placedFace = event.getBlock().getFace(event.getBlockAgainst());
            final Vector placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());

            // If greater than 90
            if (user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE) {
                if (++user.getScaffoldData().angleFails >= this.violationThreshold) {
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
                    return 3;
                }
            } else if (user.getScaffoldData().angleFails > 0) {
                user.getScaffoldData().angleFails--;
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

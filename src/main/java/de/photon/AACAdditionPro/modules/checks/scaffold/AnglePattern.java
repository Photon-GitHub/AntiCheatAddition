package de.photon.AACAdditionPro.modules.checks.scaffold;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

class AnglePattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    private static final double MAX_ANGLE = Math.toRadians(90);

    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Override
    protected int process(User user, BlockPlaceEvent event)
    {
        final BlockFace placedFace = event.getBlock().getFace(event.getBlockAgainst());
        final Vector placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());

        // If greater than 90
        if (user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE)
        {
            if (++user.getScaffoldData().angleFails >= this.violationThreshold)
            {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
                return 3;
            }
        }
        else if (user.getScaffoldData().angleFails > 0)
        {
            user.getScaffoldData().angleFails--;
        }
        return 0;
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

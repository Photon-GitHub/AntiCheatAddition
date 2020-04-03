package de.photon.aacadditionpro.user.subdata.datawrappers;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@Getter
public class ScaffoldBlockPlace extends BlockPlace
{
    private final BlockFace blockFace;
    private final double yaw;
    private final boolean sneaked;
    private final double speedModifier;

    public ScaffoldBlockPlace(Block block, BlockFace blockFace, Integer speedLevel, double yaw, boolean sneaked)
    {
        super(block);
        this.blockFace = blockFace;
        this.yaw = yaw;
        this.sneaked = sneaked;

        // Init the speed modifier which indicates how much the speed potion effect effects this blockplace.
        if (speedLevel == null || speedLevel < 0)
        {
            this.speedModifier = 1.0D;
        }
        else
        {
            //If the speedLevel is <= 0, the speed_modifier is 1
            switch (speedLevel)
            {
                case 0:
                    this.speedModifier = 1.01D;
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    this.speedModifier = 1.5D;
                    break;
                case 6:
                    this.speedModifier = 1.55D;
                    break;
                case 7:
                    this.speedModifier = 2.3D;
                    break;
                default:
                    // Everything above 8 should have a speed_modifier of 3
                    this.speedModifier = 3.0D;
                    break;
            }
        }
    }
}

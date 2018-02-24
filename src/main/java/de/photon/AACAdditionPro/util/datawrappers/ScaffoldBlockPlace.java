package de.photon.AACAdditionPro.util.datawrappers;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@Getter
public class ScaffoldBlockPlace extends BlockPlace
{
    private final BlockFace blockFace;
    private final Integer speedLevel;
    private final double yawSinus;
    private final boolean sneaked;

    public ScaffoldBlockPlace(Block block, BlockFace blockFace, Integer speedLevel, double yawSinus, boolean sneaked)
    {
        super(block);
        this.blockFace = blockFace;
        this.speedLevel = speedLevel;
        this.yawSinus = yawSinus;
        this.sneaked = sneaked;
    }
}

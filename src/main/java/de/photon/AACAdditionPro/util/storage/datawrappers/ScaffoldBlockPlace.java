package de.photon.AACAdditionPro.util.storage.datawrappers;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@Getter
public class ScaffoldBlockPlace extends BlockPlace
{
    private final BlockFace blockFace;
    private final Integer speedLevel;

    public ScaffoldBlockPlace(Block block, BlockFace blockFace, Integer speedLevel)
    {
        super(block);
        this.blockFace = blockFace;
        this.speedLevel = speedLevel;
    }
}

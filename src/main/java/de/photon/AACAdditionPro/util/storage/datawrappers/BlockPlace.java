package de.photon.AACAdditionPro.util.storage.datawrappers;

import lombok.Getter;
import org.bukkit.block.Block;

@Getter
public class BlockPlace
{
    private final long time;
    private final Block block;
    private final Integer speedLevel;
    private final Integer jumpBoostLevel;

    public BlockPlace(Block block, Integer speedLevel, Integer jumpBoostLevel)
    {
        this(System.currentTimeMillis(), block, speedLevel, jumpBoostLevel);
    }

    public BlockPlace(long time, Block block, Integer speedLevel, Integer jumpBoostLevel)
    {
        this.time = time;
        this.block = block;
        this.speedLevel = speedLevel;
        this.jumpBoostLevel = jumpBoostLevel;
    }
}
package de.photon.AACAdditionPro.util.datawrappers;

import lombok.Getter;
import org.bukkit.block.Block;

@Getter
public class TowerBlockPlace extends BlockPlace
{
    private final Integer jumpBoostLevel;

    public TowerBlockPlace(Block block, Integer jumpBoostLevel)
    {
        super(block);
        this.jumpBoostLevel = jumpBoostLevel;
    }
}

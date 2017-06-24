package de.photon.AACAdditionPro.util.storage.datawrappers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;

@RequiredArgsConstructor(suppressConstructorProperties = true)
@Getter
public class BlockPlace
{
    private final long time;
    private final Block block;
    private final short speedLevel;
    private final short jumpBoostLevel;
}
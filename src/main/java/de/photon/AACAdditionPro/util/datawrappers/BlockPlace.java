package de.photon.AACAdditionPro.util.datawrappers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;

@Getter
@RequiredArgsConstructor(suppressConstructorProperties = true)
public class BlockPlace
{
    private final long time = System.currentTimeMillis();
    private final Block block;
}
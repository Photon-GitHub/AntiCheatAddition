package de.photon.aacadditionproold.user.subdata.datawrappers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;

@Getter
@RequiredArgsConstructor
public class BlockPlace
{
    private final long time = System.currentTimeMillis();
    private final Block block;
}
package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.buffer.DequeBuffer;
import de.photon.aacadditionpro.util.datastructures.buffer.SimpleBuffer;
import lombok.Getter;

public class InventoryData extends SubData
{
    @Getter
    private final DequeBuffer<InventoryClick> averageHeuristicClicks = new SimpleBuffer<>(15);
    public int averageHeuristicMisclicks = 0;

    public long perfectExitFails = 0;

    public InventoryData(User user)
    {
        super(user);
    }
}

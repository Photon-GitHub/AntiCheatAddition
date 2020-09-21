package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.modules.checks.inventory.AverageHeuristicPattern;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.batch.Batch;
import lombok.Getter;

public class InventoryData extends SubData
{
    // Default buffer size is 6, being well tested.
    public static final int AVERAGE_HEURISTICS_BATCH_SIZE = 15;
    @Getter
    private final Batch<InventoryClick> batch;
    public int averageHeuristicMisclicks = 0;
    public long perfectExitFails = 0;

    public InventoryData(final User user)
    {
        super(user);

        batch = new Batch<>(user, AVERAGE_HEURISTICS_BATCH_SIZE, InventoryClick.dummyClick());
        batch.registerProcessor(AverageHeuristicPattern.getInstance().getBatchProcessor());
    }
}

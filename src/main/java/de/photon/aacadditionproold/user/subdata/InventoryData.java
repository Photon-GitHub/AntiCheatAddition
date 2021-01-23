package de.photon.aacadditionproold.user.subdata;

import de.photon.aacadditionproold.modules.checks.inventory.AverageHeuristicPattern;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionproold.util.datastructures.batch.Batch;
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

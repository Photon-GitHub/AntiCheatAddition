package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class InventoryStatistical extends ViolationModule
{
    public static final InventoryStatistical INSTANCE = new InventoryStatistical();

    private InventoryStatistical()
    {
        super("Inventory.parts.Statistical");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var batchProcessor = new StatisticalBatchProcessor(this);
        return ModuleLoader.builder(this)
                           .batchProcessor(batchProcessor)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(160, 1).build();
    }
}

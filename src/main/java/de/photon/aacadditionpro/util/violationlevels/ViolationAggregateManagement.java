package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class ViolationAggregateManagement extends ViolationAggregation
{
    public ViolationAggregateManagement(@NotNull ViolationModule module, @NotNull ThresholdManagement thresholds, Set<ViolationManagement> children)
    {
        super(module, thresholds, children);
    }

    @Override
    public int getVL(UUID uuid)
    {
        int i = 0;
        for (ViolationManagement child : this.children) {
            i += child.getVL(uuid);
        }
        return i;
    }
}

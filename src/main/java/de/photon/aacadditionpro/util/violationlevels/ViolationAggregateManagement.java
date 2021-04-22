package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class ViolationAggregateManagement extends ViolationManagement
{
    private final Set<ViolationManagement> subViolationManagements;

    public ViolationAggregateManagement(Module module, ThresholdManagement management, ViolationManagement... subViolationManagements)
    {
        super(module, management);
        this.subViolationManagements = ImmutableSet.copyOf(subViolationManagements);
    }

    public ViolationAggregateManagement(Module module, ViolationManagement... subViolationManagements)
    {
        this(module, ThresholdManagement.loadThresholds(module.getConfigString() + ".thresholds"), subViolationManagements);
    }

    @Override
    public void flag(Flag flag)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getVL(UUID uuid)
    {
        int aggregateVl = 0;
        for (ViolationManagement vlm : this.subViolationManagements) {
            aggregateVl += vlm.getVL(uuid);
        }
        return aggregateVl;
    }

    @Override
    public void setVL(Player player, int newVl)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void addVL(Player player, int vl)
    {
        throw new UnsupportedOperationException();
    }
}

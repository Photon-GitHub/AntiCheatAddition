package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ImmutableSet;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class ViolationAggregateManagement extends ViolationManagement
{
    private final Set<ViolationManagement> subViolationManagements;

    /**
     * Create a new {@link ViolationManagement}
     *
     * @param moduleId the module id of the module this {@link ViolationManagement} is being used by.
     */
    public ViolationAggregateManagement(String moduleId, ThresholdManagement management, ViolationManagement... subViolationManagements)
    {
        super(moduleId, management);
        this.subViolationManagements = ImmutableSet.copyOf(subViolationManagements);
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

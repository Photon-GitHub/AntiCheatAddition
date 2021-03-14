package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.events.ViolationEvent;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ViolationLevelManagement extends ViolationManagement
{
    private final ViolationLevelMultiSet vlMultiSet;

    /**
     * Create a new {@link ViolationManagement}
     *
     * @param moduleId   the module id of the module this {@link ViolationManagement} is being used by.
     * @param decayTicks the time in ticks until the vl of a player is decreased by one. If this is negative no decrease will happen.
     */
    public ViolationLevelManagement(String moduleId, boolean hasThresholds, long decayTicks)
    {
        super(moduleId, hasThresholds);
        vlMultiSet = new ViolationLevelMultiSet(decayTicks);
    }

    @Override
    public void flag(ViolationManagement.Flag flag)
    {
        if (flag.addedVl <= 0) return;

        if (!ViolationEvent.build(flag.player, this.moduleId, flag.addedVl).call().isCancelled()) {
            this.addVL(flag.player, flag.addedVl);
            flag.executeRunnablesIfNeeded(this.getVL(flag.player.getUniqueId()));
        }
    }

    @Override
    public int getVL(UUID uuid)
    {
        return this.vlMultiSet.getMultiset().count(uuid);
    }

    @Override
    public void setVL(Player player, int newVl)
    {
        final int oldVl = this.vlMultiSet.getMultiset().setCount(player.getUniqueId(), newVl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        if (oldVl < newVl) {
            this.punishPlayer(player, oldVl, newVl);
        }
    }

    @Override
    protected void addVL(Player player, int vl)
    {
        final int oldVl = this.vlMultiSet.getMultiset().add(player.getUniqueId(), vl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        this.punishPlayer(player, oldVl, oldVl + vl);
    }
}

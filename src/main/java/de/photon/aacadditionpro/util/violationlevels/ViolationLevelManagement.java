package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.events.ViolationEvent;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ViolationLevelManagement extends ViolationManagement
{
    private final ViolationLevelMultiSet vlMultiSet;

    public ViolationLevelManagement(ViolationModule module, ThresholdManagement management, long decayTicks)
    {
        super(module, management);
        vlMultiSet = new ViolationLevelMultiSet(decayTicks);
    }

    public ViolationLevelManagement(ViolationModule module, long decayTicks)
    {
        this(module, ThresholdManagement.loadThresholds(module.getConfigString() + ".thresholds"), decayTicks);
    }

    @Override
    public void flag(Flag flag)
    {
        Preconditions.checkNotNull(flag.getPlayer(), "Tried to flag null player.");
        if (!ViolationEvent.build(flag.getPlayer(), this.module.getModuleId(), flag.getAddedVl()).call().isCancelled()) {
            this.addVL(flag.getPlayer(), flag.getAddedVl());
            flag.executeRunnablesIfNeeded(this.getVL(flag.getPlayer().getUniqueId()));
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
        val oldVl = this.vlMultiSet.getMultiset().setCount(player.getUniqueId(), newVl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        if (oldVl < newVl) this.punishPlayer(player, oldVl, newVl);
    }

    @Override
    protected void addVL(Player player, int vl)
    {
        val oldVl = this.vlMultiSet.getMultiset().add(player.getUniqueId(), vl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        this.punishPlayer(player, oldVl, oldVl + vl);
    }
}

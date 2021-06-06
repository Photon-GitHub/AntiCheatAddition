package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.events.ViolationEvent;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ViolationLevelManagement extends ViolationManagement
{
    private final ViolationLevelMultiSet vlMultiSet;

    public ViolationLevelManagement(@NotNull ViolationModule module, @NotNull ThresholdManagement management, long decayTicks, int decayAmount)
    {
        super(module, management);
        vlMultiSet = new ViolationLevelMultiSet(decayTicks, decayAmount);
    }

    public ViolationLevelManagement(@NotNull ViolationModule module, long decayTicks, int decayAmount)
    {
        this(module, ThresholdManagement.loadThresholds(module), decayTicks, decayAmount);
    }

    @Override
    public void flag(@NotNull Flag flag)
    {
        Preconditions.checkNotNull(flag.getPlayer(), "Tried to flag null player.");
        if (!ViolationEvent.build(flag.getPlayer(), this.module.getModuleId(), flag.getAddedVl()).call().isCancelled()) {
            this.addVL(flag.getPlayer(), flag.getAddedVl());
            flag.executeRunnablesIfNeeded(this.getVL(flag.getPlayer().getUniqueId()));
        }
    }

    @Override
    public int getVL(@NotNull UUID uuid)
    {
        return this.vlMultiSet.getMultiset().count(uuid);
    }

    @Override
    public void setVL(@NotNull Player player, int newVl)
    {
        val oldVl = this.vlMultiSet.getMultiset().setCount(player.getUniqueId(), newVl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        if (oldVl < newVl) this.punishPlayer(player, oldVl, newVl);
    }

    @Override
    protected void addVL(@NotNull Player player, int vl)
    {
        val oldVl = this.vlMultiSet.getMultiset().add(player.getUniqueId(), vl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        this.punishPlayer(player, oldVl, oldVl + vl);
    }
}

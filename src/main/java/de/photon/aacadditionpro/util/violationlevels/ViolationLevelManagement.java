package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.events.ViolationEvent;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ViolationLevelManagement extends ViolationManagement
{
    private final ViolationLevelMultiSet vlMultiSet;

    private ViolationLevelManagement(@NotNull ViolationModule module, @NotNull ThresholdManagement management, long decayTicks, int decayAmount)
    {
        super(module, management);
        vlMultiSet = new ViolationLevelMultiSet(decayTicks * 50, decayAmount);
    }

    public static Builder builder(ViolationModule module)
    {
        return new Builder(module);
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

    @RequiredArgsConstructor
    public static class Builder
    {
        @NotNull private final ViolationModule module;
        private ThresholdManagement management = null;
        private long decayTicks = -1;
        private int decayAmount = 0;

        /**
         * This will set the {@link ThresholdManagement} to {@link ThresholdManagement#EMPTY}
         */
        public Builder emptyThresholdManagement()
        {
            return withCustomThresholdManagement(ThresholdManagement.EMPTY);
        }

        /**
         * This allows to define a custom {@link ThresholdManagement}.
         * The standard is {@link ThresholdManagement#loadThresholds(ViolationModule)}.
         */
        public Builder withCustomThresholdManagement(@NotNull ThresholdManagement management)
        {
            Preconditions.checkNotNull(management, "The custom management must not be null.");
            this.management = management;
            return this;
        }

        public Builder loadThresholdsToManagement()
        {
            this.management = ThresholdManagement.loadThresholds(this.module);
            return this;
        }

        /**
         * Allows defining decay.
         * Not calling this method indicates no decay.
         */
        public Builder withDecay(long decayTicks, int decayAmount)
        {
            Preconditions.checkArgument(decayTicks > 0, "The decay ticks need to be greater than 0. No decay is the default setting.");
            Preconditions.checkArgument(decayAmount > 0, "The decay amount needs to be greater than 0. No decay is the default setting.");
            this.decayTicks = decayTicks;
            this.decayAmount = decayAmount;
            return this;
        }

        public ViolationLevelManagement build()
        {
            Preconditions.checkNotNull(management, "Tried to create module without specifying threshold management.");
            return new ViolationLevelManagement(this.module, management, decayTicks, decayAmount);
        }
    }
}

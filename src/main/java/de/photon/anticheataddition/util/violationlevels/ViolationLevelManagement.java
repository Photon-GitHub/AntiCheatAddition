package de.photon.anticheataddition.util.violationlevels;

import com.google.common.base.Preconditions;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.events.ViolationEvent;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ViolationLevelManagement extends ViolationManagement
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
            flag.callNotCancelledActions(this.getVL(flag.getPlayer().getUniqueId()));
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
        final int oldVl = this.vlMultiSet.getMultiset().setCount(player.getUniqueId(), newVl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        if (oldVl < newVl) this.punishPlayer(player, oldVl, newVl);

        // Update potential aggregations.
        this.broadcast(player);
    }

    @Override
    protected void addVL(@NotNull Player player, int vl)
    {
        final int oldVl = this.vlMultiSet.getMultiset().add(player.getUniqueId(), vl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        this.punishPlayer(player, oldVl, oldVl + vl);

        // Update potential aggregations.
        this.broadcast(player);
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

    private static class ViolationLevelMultiSet implements Listener
    {
        private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

        @Getter
        private final Multiset<UUID> multiset = ConcurrentHashMultiset.create();
        private final int vlDecayAmount;

        ViolationLevelMultiSet(final long decayMilliseconds, final int vlDecayAmount)
        {
            this.vlDecayAmount = vlDecayAmount;

            // Might need to have a vl manager without vl decrease
            if (decayMilliseconds > 0) {
                // Schedule the decay with 3000 milliseconds to free startup.
                SCHEDULER.scheduleAtFixedRate(this::decay, 3000, decayMilliseconds, TimeUnit.MILLISECONDS);
            }

            AntiCheatAddition.getInstance().registerListener(this);
        }

        /**
         * Decrements the vl of every player.
         */
        private void decay()
        {
            // Decrement the vl of every player.
            for (UUID uuid : multiset.elementSet()) multiset.remove(uuid, this.vlDecayAmount);
        }

        @EventHandler
        public void onQuit(final PlayerQuitEvent event)
        {
            this.multiset.setCount(event.getPlayer().getUniqueId(), 0);
        }
    }
}

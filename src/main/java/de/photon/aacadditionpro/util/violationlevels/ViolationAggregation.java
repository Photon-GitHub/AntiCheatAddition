package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public abstract class ViolationAggregation extends ViolationManagement
{
    @NotNull protected final Set<ViolationManagement> children;
    @NotNull private final Multiset<UUID> oldVls = ConcurrentHashMultiset.create();

    public ViolationAggregation(@NotNull ViolationModule module, @NotNull ThresholdManagement thresholds, Set<ViolationManagement> children)
    {
        super(module, thresholds);

        Preconditions.checkNotNull(children, "Tried to construct ViolationAggregation with null children.");
        Preconditions.checkArgument(!children.isEmpty(), "Tried to construct ViolationAggregation with no children.");
        this.children = ImmutableSet.copyOf(children);
    }

    @Override
    public void flag(Flag flag)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVL(Player player, int newVl)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates the vl aggregation of a {@link Player} and punishes when necessary.
     */
    public void update(Player player)
    {
        val uuid = player.getUniqueId();
        val oldVl = oldVls.count(uuid);
        val newVl = getVL(uuid);
        if (oldVl < newVl) punishPlayer(player, oldVl, newVl);
        oldVls.setCount(uuid, newVl);
    }

    @Override
    protected void addVL(Player player, int vl)
    {
        throw new UnsupportedOperationException();
    }
}

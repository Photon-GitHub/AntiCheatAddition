package de.photon.anticheataddition.util.violationlevels;

import com.google.common.base.Preconditions;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class ViolationAggregation extends ViolationManagement
{
    @NotNull protected final Set<ViolationManagement> children;
    @NotNull private final Multiset<UUID> oldVls = ConcurrentHashMultiset.create();

    public ViolationAggregation(@NotNull ViolationModule module, @NotNull ThresholdManagement thresholds, Set<ViolationManagement> children)
    {
        super(module, thresholds);

        Preconditions.checkNotNull(children, "Tried to construct ViolationAggregation with null children.");
        Preconditions.checkArgument(!children.isEmpty(), "Tried to construct ViolationAggregation with no children.");
        this.children = Set.copyOf(children);

        // Receive updates from the children.
        for (ViolationManagement child : this.children) {
            child.subscribe(this);
        }
    }

    @Override
    public int getVL(@NotNull UUID uuid)
    {
        int i = 0;
        for (ViolationManagement child : this.children) {
            i += child.getVL(uuid);
        }
        return i;
    }

    @Override
    public void flag(@NotNull Flag flag)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVL(@NotNull Player player, int newVl)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void addVL(@NotNull Player player, int vl)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void receive(Player player)
    {
        final UUID uuid = player.getUniqueId();
        final int oldVl = oldVls.count(uuid);
        final int newVl = getVL(uuid);

        if (oldVl < newVl) punishPlayer(player, oldVl, newVl);
        oldVls.setCount(uuid, newVl);
    }
}

package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * This class presents options for the flagging process.
 */
@Getter
public class Flag
{
    private Player player;
    private Set<Player> team;
    private int addedVl = 1;
    private int cancelVl = -1;
    private Runnable onCancel = null;
    private Runnable eventNotCancelled = null;

    private Flag(Player player)
    {
        this.player = player;
    }

    private Flag(Set<Player> team)
    {
        this.team = team;
    }

    public static Flag of(Player player)
    {
        return new Flag(player);
    }

    public static Flag of(Player... team)
    {
        return new Flag(ImmutableSet.copyOf(team));
    }

    public static Flag of(Set<Player> team)
    {
        return new Flag(ImmutableSet.copyOf(team));
    }

    public Flag setAddedVl(int addedVl)
    {
        Preconditions.checkArgument(addedVl >= 1, "Tried to add no or negative vl in flag.");
        this.addedVl = addedVl;
        return this;
    }

    public Flag setCancelAction(int cancelVl, Runnable onCancel)
    {
        Preconditions.checkArgument(cancelVl >= 0, "Set negative cancel vl in flag.");
        this.cancelVl = cancelVl;
        this.onCancel = Preconditions.checkNotNull(onCancel, "Tried to set null onCancel action in flag.");
        return this;
    }

    public Flag setEventNotCancelledAction(Runnable eventNotCancelled)
    {
        this.eventNotCancelled = Preconditions.checkNotNull(eventNotCancelled, "Tried to set null eventNotCancelled action in flag.");
        return this;
    }

    /**
     * This method will execute both the
     */
    public void executeRunnablesIfNeeded(int currentVl)
    {
        if (this.cancelVl >= 0 && currentVl >= this.cancelVl) this.onCancel.run();
        if (this.eventNotCancelled != null) this.eventNotCancelled.run();
    }
}
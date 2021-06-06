package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.user.User;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * This class presents options for the flagging process.
 */
@Getter
public class Flag
{
    private final Player player;
    private final Set<Player> team;
    private int addedVl = 1;
    private int cancelVl = -1;
    private Runnable onCancel = null;
    private Runnable eventNotCancelled = null;

    private Flag(Player player)
    {
        this.player = player;
        this.team = null;
    }

    private Flag(Set<Player> team)
    {
        this.player = null;
        this.team = team;
    }

    /**
     * Creates a new flag concerning a {@link Player}
     */
    public static Flag of(User user)
    {
        return new Flag(user.getPlayer());
    }

    /**
     * Creates a new flag concerning a {@link Player}
     */
    public static Flag of(Player player)
    {
        return new Flag(player);
    }

    /**
     * Creates a new flag concerning multiple {@link Player}s.
     */
    public static Flag of(Player... team)
    {
        return new Flag(ImmutableSet.copyOf(team));
    }


    /**
     * Creates a new flag concerning multiple {@link Player}s.
     */
    public static Flag of(Set<Player> team)
    {
        return new Flag(ImmutableSet.copyOf(team));
    }

    /**
     * This method defines how many vls are added to the score of the {@link Player}s defined in the constructors.
     */
    public Flag setAddedVl(int addedVl)
    {
        Preconditions.checkArgument(addedVl >= 1, "Tried to add no or negative vl in flag.");
        this.addedVl = addedVl;
        return this;
    }

    /**
     * This method defines what action should be taken once a certain vl is surpassed to cancel a flagged action.
     *
     * @param cancelVl the vl needed to trigger the action. Must be greater or equal to 0.
     * @param onCancel the action that will be performed once the cancelVl is reached.
     */
    public Flag setCancelAction(int cancelVl, Runnable onCancel)
    {
        Preconditions.checkArgument(cancelVl >= 0, "Set negative cancel vl in flag.");
        this.cancelVl = cancelVl;
        this.onCancel = Preconditions.checkNotNull(onCancel, "Tried to set null onCancel action in flag.");
        return this;
    }

    /**
     * This is used to set an action that will only be run if the event caused by the flag has not been cancelled.
     */
    public Flag setEventNotCancelledAction(Runnable eventNotCancelled)
    {
        this.eventNotCancelled = Preconditions.checkNotNull(eventNotCancelled, "Tried to set null eventNotCancelled action in flag.");
        return this;
    }

    /**
     * This method will execute the runnables when applicable.
     */
    public void executeRunnablesIfNeeded(int currentVl)
    {
        if (this.cancelVl >= 0 && currentVl >= this.cancelVl) this.onCancel.run();
        if (this.eventNotCancelled != null) this.eventNotCancelled.run();
    }
}
package de.photon.anticheataddition.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * This class presents options for the flagging process.
 */
@Getter
public final class Flag
{
    private final Player player;
    private int addedVl = 1;
    private int cancelVl = -1;
    private Supplier<String> debug = null;
    private Runnable onCancel = null;
    private Runnable eventNotCancelled = null;

    private Flag(Player player)
    {
        this.player = player;
    }

    /**
     * Creates a new flag concerning a {@link Player}
     */
    public static Flag of(User user)
    {
        return of(user.getPlayer());
    }

    /**
     * Creates a new flag concerning a {@link Player}
     */
    public static Flag of(Player player)
    {
        return new Flag(player);
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
     * @param cancelVl the vl needed to trigger the action. Must be greater or equal to 0, otherwise no action will be taken.
     * @param onCancel the action that will be performed once the cancelVl is reached.
     */
    public Flag setCancelAction(int cancelVl, Runnable onCancel)
    {
        if (cancelVl < 0) return this;
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
     * Any debug will be sent if the event was not cancelled.
     */
    public Flag setDebug(Supplier<String> debug)
    {
        this.debug = debug;
        return this;
    }

    /**
     * This method will execute the runnables when applicable.
     *
     * @param oldVl The vl of the player before this flag. The method will add the vl of this {@link Flag} to it.
     */
    public void runApplicableActions(int oldVl)
    {
        if (this.debug != null) Log.fine(this.debug);
        if (this.cancelVl >= 0 && (oldVl + this.addedVl) >= this.cancelVl) this.onCancel.run();
        if (this.eventNotCancelled != null) this.eventNotCancelled.run();
    }
}
package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Used to store when a player moved.
 * The first index of this {@link TimeData} represents the last time a player moved,
 * the second index represents the last time a player moved when ignoring head movement
 */
public class PositionData extends TimeData implements Listener
{
    public boolean allowedToJump = true;

    public PositionData(final User user)
    {
        // First one is head movement, second one normal movement and third one xz-movement.
        super(user, System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
        AACAdditionPro.getInstance().registerListener(this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final PlayerMoveEvent event)
    {
        if (this.getUser().refersToUUID(event.getPlayer().getUniqueId()))
        {
            // Head + normal movement
            this.updateTimeStamp(0);

            // xz movement only
            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getZ() != event.getTo().getZ())
            {
                this.updateTimeStamp(1);
                this.updateTimeStamp(2);
            }
            // Any non-head movement.
            else if (event.getFrom().getY() != event.getTo().getY())
            {
                updateTimeStamp(1);
            }
        }
    }

    /**
     * This checks if a player moved recently.
     *
     * @param movementType what movement should be checked
     *
     * @return true if the player has moved in the last second
     */
    public boolean hasPlayerMovedRecently(final long milliseconds, final MovementType movementType)
    {
        return this.recentlyUpdated(movementType.ordinal(), milliseconds);
    }

    @Override
    public void unregister()
    {
        HandlerList.unregisterAll(this);
        super.unregister();
    }

    /**
     * Determines what index should be checkd in the {@link PositionData}.
     */
    public enum MovementType
    {
        // Position here is important as ordinal is used!
        ANY,
        NONHEAD,
        XZONLY
    }
}
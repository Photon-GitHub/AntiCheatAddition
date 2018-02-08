package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Used to store when a player moved.
 * The first index of this {@link TimeData} represents the last time a player moved,
 * the second index represents the last time a player moved when ignoring head movement
 */
public class PositionData extends TimeData
{
    public boolean allowedToJump = true;

    public PositionData(final User theUser)
    {
        super(true, theUser, System.currentTimeMillis(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final PlayerMoveEvent event)
    {
        if (this.theUser.refersToUUID(event.getPlayer().getUniqueId()))
        {
            // Head + normal movement
            this.updateTimeStamp(0);

            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ())
            {
                // Normal movement only
                this.updateTimeStamp(1);
            }
        }
    }

    /**
     * This checks if a player moved recently.
     *
     * @param ignoreHead Should head-movement be ignored
     *
     * @return true if the player has moved in the last second
     */
    public boolean hasPlayerMovedRecently(final boolean ignoreHead)
    {
        return hasPlayerMovedRecently(1000, ignoreHead);
    }

    /**
     * This checks if a player moved recently.
     *
     * @param ignoreHead Should head-movement be ignored
     *
     * @return true if the player has moved in the last second
     */
    public boolean hasPlayerMovedRecently(final long milliseconds, final boolean ignoreHead)
    {
        return this.recentlyUpdated(ignoreHead ? 1 : 0, milliseconds);
    }
}

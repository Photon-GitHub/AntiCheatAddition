package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Used to store when a player moved.
 * The first index of this {@link TimeData} represents the last time a player moved,
 * the second index represents the last time a player moved when ignoring head movement
 */
public class PositionData extends TimeData implements Listener
{
    public boolean allowedToJump = true;
    private boolean currentlySneaking = false;
    private boolean currentlySprinting = false;

    @Getter
    private long lastSprintTime = 0;
    @Getter
    private long lastSneakTime = 0;


    public PositionData(final User user)
    {
        /*
         * 1 -> is head movement
         * 2 -> normal movement
         * 3 -> xz-movement.
         *
         * 4 -> last sprinting
         * 5 -> last sneaking
         */
        super(user, System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(), 0, 0);
        AACAdditionPro.getInstance().registerListener(this);
    }

    @EventHandler
    public void on(final PlayerToggleSprintEvent event)
    {
        if (this.getUser().refersToUUID(event.getPlayer().getUniqueId()))
        {
            this.currentlySprinting = event.isSprinting();
            if (!this.currentlySprinting)
            {
                this.lastSprintTime = this.passedTime(3);
            }
            this.updateTimeStamp(3);
        }
    }

    @EventHandler
    public void on(final PlayerToggleSneakEvent event)
    {
        if (this.getUser().refersToUUID(event.getPlayer().getUniqueId()))
        {
            this.currentlySneaking = event.isSneaking();
            if (!this.currentlySneaking)
            {
                this.lastSneakTime = this.passedTime(4);
            }
            this.updateTimeStamp(4);
        }
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

    public boolean hasPlayerSprintedRecently(final long milliseconds)
    {
        return this.currentlySprinting || this.recentlyUpdated(3, milliseconds);
    }

    public boolean hasPlayerSneakedRecently(final long milliseconds)
    {
        return this.currentlySneaking || this.recentlyUpdated(4, milliseconds);
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
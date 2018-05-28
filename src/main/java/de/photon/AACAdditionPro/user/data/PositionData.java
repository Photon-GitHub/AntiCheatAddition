package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Used to store when a player moved.
 * The first index of this {@link TimeData} represents the last time a player moved,
 * the second index represents the last time a player moved when ignoring head movement
 */
public class PositionData extends TimeData
{
    static
    {
        AACAdditionPro.getInstance().registerListener(new PositionDataUpdater());
    }

    public boolean allowedToJump = true;
    private boolean currentlySneaking = false;
    private boolean currentlySprinting = false;

    @Getter
    private long lastSprintTime = Long.MAX_VALUE;
    @Getter
    private long lastSneakTime = Long.MAX_VALUE;

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

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class PositionDataUpdater implements Listener
    {
        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void on(final PlayerToggleSprintEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getPositionData().currentlySprinting = event.isSprinting();
                if (!user.getPositionData().currentlySprinting)
                {
                    user.getPositionData().lastSprintTime = user.getPositionData().passedTime(3);
                }
                user.getPositionData().updateTimeStamp(3);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void on(final PlayerToggleSneakEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getPositionData().currentlySneaking = event.isSneaking();
                if (!user.getPositionData().currentlySneaking)
                {
                    user.getPositionData().lastSneakTime = user.getPositionData().passedTime(4);
                }
                user.getPositionData().updateTimeStamp(4);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void on(final PlayerMoveEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                // Head + normal movement
                user.getPositionData().updateTimeStamp(0);

                // xz movement only
                if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getZ() != event.getTo().getZ())
                {
                    user.getPositionData().updateTimeStamp(1);
                    user.getPositionData().updateTimeStamp(2);
                }
                // Any non-head movement.
                else if (event.getFrom().getY() != event.getTo().getY())
                {
                    user.getPositionData().updateTimeStamp(1);
                }
            }
        }
    }
}
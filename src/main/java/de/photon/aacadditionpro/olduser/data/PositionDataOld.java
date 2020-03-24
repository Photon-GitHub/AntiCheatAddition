package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Used to store when a player moved.
 * The first index of this {@link TimeDataOld} represents the last time a player moved,
 * the second index represents the last time a player moved when ignoring head movement
 */
public class PositionDataOld extends TimeDataOld
{
    static {
        AACAdditionPro.getInstance().registerListener(new PositionDataUpdater());
    }

    public boolean allowedToJump = true;
    private boolean currentlySneaking = false;
    private boolean currentlySprinting = false;

    @Getter
    private long lastSprintTime = Long.MAX_VALUE;
    @Getter
    private long lastSneakTime = Long.MAX_VALUE;

    public PositionDataOld(final UserOld user)
    {
        /*
         * [0]  head movement
         * [1]  normal movement
         * [2]  xz-movement.
         *
         * [3]  last sprinting
         * [4]  last sneaking
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
     * Determines what index should be checkd in the {@link PositionDataOld}.
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
        public void onMove(final PlayerMoveEvent event)
        {
            final UserOld user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
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
                else if (event.getFrom().getY() != event.getTo().getY()) {
                    user.getPositionData().updateTimeStamp(1);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onToggleSneak(final PlayerToggleSneakEvent event)
        {
            final UserOld user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getPositionData().currentlySneaking = event.isSneaking();
                if (!user.getPositionData().currentlySneaking) {
                    user.getPositionData().lastSneakTime = user.getPositionData().passedTime(4);
                }
                user.getPositionData().updateTimeStamp(4);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onToggleSpring(final PlayerToggleSprintEvent event)
        {
            final UserOld user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getPositionData().currentlySprinting = event.isSprinting();
                if (!user.getPositionData().currentlySprinting) {
                    user.getPositionData().lastSprintTime = user.getPositionData().passedTime(3);
                }
                user.getPositionData().updateTimeStamp(3);
            }
        }
    }
}
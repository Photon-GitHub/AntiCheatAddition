package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Used to store a player was teleported
 * The first index of this {@link TimeData} represents the last time a player was teleported.
 */
public class TeleportData extends TimeData
{
    public TeleportData(final User theUser)
    {
        super(true, theUser);
    }

    @EventHandler
    public void on(final PlayerChangedWorldEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId())) {
            this.updateTimeStamp();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final PlayerTeleportEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId())) {
            this.updateTimeStamp();
        }
    }
}

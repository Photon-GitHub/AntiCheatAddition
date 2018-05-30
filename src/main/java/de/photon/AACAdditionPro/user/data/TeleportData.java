package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Used to store a player was teleported
 * The first index of this {@link TimeData} represents the last time a player was teleported.
 */
public class TeleportData extends TimeData implements Listener
{
    public TeleportData(final User user)
    {
        // [0] = Teleport
        // [1] = World change
        // [2] = Respawn
        super(user, 0, 0, 0);
        AACAdditionPro.getInstance().registerListener(this);
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event)
    {
        this.updateIfRefersToUser(event.getPlayer().getUniqueId(), 0);
        this.updateIfRefersToUser(event.getPlayer().getUniqueId(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(final PlayerTeleportEvent event)
    {
        this.updateIfRefersToUser(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event)
    {
        this.updateIfRefersToUser(event.getPlayer().getUniqueId(), 0);
        this.updateIfRefersToUser(event.getPlayer().getUniqueId(), 2);
    }

    @Override
    public void unregister()
    {
        HandlerList.unregisterAll(this);
        super.unregister();
    }
}

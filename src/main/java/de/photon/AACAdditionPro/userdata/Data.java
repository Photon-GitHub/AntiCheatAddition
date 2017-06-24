package de.photon.AACAdditionPro.userdata;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class Data implements Listener
{
    protected final User theUser;

    protected Data(final boolean enableListener, final User theUser)
    {
        this.theUser = theUser;
        if(enableListener)
        {
            AACAdditionPro.getInstance().registerListener(this);
        }
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        // Listener cleanup
        if(event.getPlayer().getUniqueId().equals(theUser.getPlayer().getUniqueId()))
        {
            HandlerList.unregisterAll(this);
        }
    }
}

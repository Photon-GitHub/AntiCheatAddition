package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TeamingData extends TimeData implements Listener
{
    public TeamingData(final User user)
    {
        super(user);
        AACAdditionPro.getInstance().registerListener(this);
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event)
    {
        // Hit somebody else
        if (event.getDamager().getEntityId() == this.getUser().getPlayer().getEntityId() ||
            // Was hit
            event.getEntity().getEntityId() == this.getUser().getPlayer().getEntityId())
        {
            this.updateTimeStamp(0);
        }
    }

    @Override
    public void unregister()
    {
        HandlerList.unregisterAll(this);
        super.unregister();
    }
}

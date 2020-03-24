package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TeamingDataOld extends TimeDataOld implements Listener
{
    public TeamingDataOld(final UserOld user)
    {
        super(user, 0);
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

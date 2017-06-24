package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TeamingData extends TimeData
{
    public TeamingData(final User theUser)
    {
        super(true, theUser);
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event)
    {
        // Hit somebody else
        if (event.getDamager().getEntityId() == this.theUser.getPlayer().getEntityId() ||
            // Was hit
            event.getEntity().getEntityId() == this.theUser.getPlayer().getEntityId())
        {
            this.updateTimeStamp();
        }
    }
}

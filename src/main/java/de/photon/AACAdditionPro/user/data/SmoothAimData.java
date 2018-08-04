package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SmoothAimData extends TimeData
{
    static
    {
        SmoothAimDataUpdater dataUpdater = new SmoothAimDataUpdater();
        AACAdditionPro.getInstance().registerListener(dataUpdater);
    }

    public int smoothAimCounter = 0;

    public SmoothAimData(final User user)
    {
        // [0] = Last time the user attacked somebody.
        super(user, 0);
    }

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    public static class SmoothAimDataUpdater implements Listener
    {
        // Event handling
        @EventHandler(priority = EventPriority.MONITOR)
        public void onDamage(final EntityDamageByEntityEvent event)
        {
            final User user = UserManager.getUser(event.getDamager().getUniqueId());

            if (user != null)
            {
                user.getSmoothAimData().updateTimeStamp(0);
            }
        }
    }
}

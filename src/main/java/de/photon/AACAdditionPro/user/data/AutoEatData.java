package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class AutoEatData extends TimeData implements Listener
{
    public AutoEatData(User user)
    {
        /*
        [0] = Time of saturation decline
        [1] = Timeout
        */
        super(user, 0, 0);
        AACAdditionPro.getInstance().registerListener(this);
    }

    @EventHandler
    public void on(final FoodLevelChangeEvent event)
    {
        if (this.getUser().refersToUUID(event.getEntity().getUniqueId()) &&
            // Food level decline
            this.getUser().getPlayer().getFoodLevel() > event.getFoodLevel())
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

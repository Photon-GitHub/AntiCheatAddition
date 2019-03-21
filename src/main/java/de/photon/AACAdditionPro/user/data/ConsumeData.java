package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class ConsumeData extends TimeData
{
    static {
        ConsumeDataUpdater consumeDataUpdater = new ConsumeDataUpdater();
        AACAdditionPro.getInstance().registerListener(consumeDataUpdater);
    }

    @Getter
    private ItemStack lastConsumedItemStack;

    public ConsumeData(final User user)
    {
        // [0] last velocity change
        super(user, 0);

    }

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class ConsumeDataUpdater implements Listener
    {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onConsume(final PlayerItemConsumeEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getConsumeData().updateTimeStamp(0);
                user.getConsumeData().lastConsumedItemStack = event.getItem();
            }
        }
    }
}

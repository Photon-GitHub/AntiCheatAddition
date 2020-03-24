package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ConsumeDataOld extends TimeDataOld
{
    static {
        ConsumeDataUpdater consumeDataUpdater = new ConsumeDataUpdater();
        AACAdditionPro.getInstance().registerListener(consumeDataUpdater);
    }

    @Getter
    private ItemStack lastConsumedItemStack;

    public ConsumeDataOld(final UserOld user)
    {
        // [0] last velocity change
        super(user, 0);

    }

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class ConsumeDataUpdater implements Listener
    {

    }
}

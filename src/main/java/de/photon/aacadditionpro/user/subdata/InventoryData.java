package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.olduser.DataOld;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;

public class InventoryData extends SubData
{
    public InventoryData(User user)
    {
        super(user);
    }

    public boolean notRecentlyOpened(final long milliseconds)
    {
        return !this.user.getTimestampMap().recentlyUpdated(TimestampKey.INVENTORY_OPENED, milliseconds);
    }

    public boolean recentlyClicked(final long milliseconds)
    {
        return this.user.getTimestampMap().recentlyUpdated(TimestampKey.LAST_INVENTORY_CLICK, milliseconds);
    }

    /**
     * Determines whether the {@link User} of this {@link DataOld} currently has an open inventory.
     */
    public boolean hasOpenInventory()
    {
        return this.user.getTimestampMap().getTimeStamp(TimestampKey.INVENTORY_OPENED) != 0;
    }
}

package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.olduser.DataOld;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.User;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

public class InventoryData extends SubData
{
    @Getter
    @Setter
    private Material lastMaterial = Material.BEDROCK;

    /**
     * The last slot a person clicked.<br>
     * This variable is used to prevent false positives based on spam-clicking one slot.
     */
    @Getter
    @Setter
    private int lastRawSlot = 0;

    public InventoryData(User user)
    {
        super(user);
    }

    public boolean notRecentlyOpened(final long milliseconds)
    {
        return !this.user.getDataMap().recentlyUpdated(DataKey.INVENTORY_OPENED, milliseconds);
    }

    public boolean recentlyClicked(final long milliseconds)
    {
        return this.user.getDataMap().recentlyUpdated(DataKey.LAST_INVENTORY_CLICK, milliseconds);
    }

    /**
     * Determines whether the {@link User} of this {@link DataOld} currently has an open inventory.
     */
    public boolean hasOpenInventory()
    {
        return this.user.getDataMap().getTimeStamp(DataKey.INVENTORY_OPENED) != 0;
    }
}

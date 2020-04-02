package de.photon.aacadditionpro.user.subdata.datawrappers;

import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;

@RequiredArgsConstructor
public class InventoryClick
{
    public final long timeStamp = System.currentTimeMillis();
    public final double[] slotLocation;
    public final ClickType clickType;

    /**
     * Creates a dummy {@link InventoryClick} to use as a starting data point.
     */
    public static InventoryClick dummyClick()
    {
        return new InventoryClick(new double[]{0, 0}, ClickType.CREATIVE);
    }

    public static InventoryClick fromClickEvent(final InventoryClickEvent event)
    {
        return new InventoryClick(InventoryUtils.locateSlot(event.getRawSlot(), event.getClickedInventory().getType()), event.getClick());
    }

    @Override
    public String toString()
    {
        return "InventoryClick{" +
               "timeStamp=" + timeStamp +
               ", slotLocation=" + Arrays.toString(slotLocation) +
               ", clickType=" + clickType +
               '}';
    }

    public static class BetweenClickInformation
    {
        public final long timeDelta;
        public final double xDistance;
        public final double yDistance;
        public final ClickType clickType;

        public BetweenClickInformation(final InventoryClick older, final InventoryClick younger)
        {
            this.timeDelta = younger.timeStamp - older.timeStamp;
            this.xDistance = younger.slotLocation[0] - older.slotLocation[0];
            this.yDistance = younger.slotLocation[1] - older.slotLocation[1];
            this.clickType = younger.clickType;
        }
    }
}
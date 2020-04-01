package de.photon.aacadditionpro.user.subdata.datawrappers;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;

@RequiredArgsConstructor
public class InventoryClick
{
    public final long timeStamp = System.currentTimeMillis();
    public final double[] slotLocation;
    public final ClickType clickType;

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
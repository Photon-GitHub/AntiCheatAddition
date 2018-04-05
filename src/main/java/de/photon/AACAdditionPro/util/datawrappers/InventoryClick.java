package de.photon.AACAdditionPro.util.datawrappers;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;

@RequiredArgsConstructor(suppressConstructorProperties = true)
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
}
package de.photon.AACAdditionPro.util.datawrappers;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;

@RequiredArgsConstructor(suppressConstructorProperties = true)
public class InventoryClick
{
    public final long timeStamp = System.currentTimeMillis();
    public final int clickedRawSlot;
    public final InventoryType inventoryType;
    public final ClickType clickType;

    @Override
    public String toString()
    {
        return "InventoryClick{" +
               "timeStamp=" + timeStamp +
               ", clickedRawSlot=" + clickedRawSlot +
               ", inventoryType=" + inventoryType +
               ", clickType=" + clickType +
               '}';
    }
}
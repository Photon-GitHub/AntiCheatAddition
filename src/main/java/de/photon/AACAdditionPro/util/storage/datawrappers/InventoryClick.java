package de.photon.AACAdditionPro.util.storage.datawrappers;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;

@RequiredArgsConstructor(suppressConstructorProperties = true)
public class InventoryClick
{
    /**
     * How many {@link de.photon.AACAdditionPro.util.storage.datawrappers.InventoryClick}s should {@link de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics} analyse in one run
     */
    public static final byte SAMPLES = 20;

    public final Material type;
    public final long timeStamp = System.currentTimeMillis();
    public final int clickedRawSlot;
    public final InventoryType.SlotType slotType;
    public final ClickType clickType;
}
package de.photon.aacadditionpro.user.data.batch;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.batch.Batch;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import de.photon.aacadditionpro.util.datastructure.dummy.DummyInventory;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import lombok.Value;
import lombok.val;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class InventoryBatch extends Batch<InventoryBatch.InventoryClick>
{
    // Default buffer size is 6, being well tested.
    public static final int AVERAGE_HEURISTICS_BATCH_SIZE = 15;
    public static final Broadcaster<Snapshot<InventoryClick>> INVENTORY_BATCH_BROADCASTER = new Broadcaster<>();

    public InventoryBatch(@NotNull User user)
    {
        super(INVENTORY_BATCH_BROADCASTER, user, AVERAGE_HEURISTICS_BATCH_SIZE, InventoryClick.DUMMY);
    }

    @Value
    public static class InventoryClick
    {
        public static final InventoryClick DUMMY = new InventoryClick(new DummyInventory(), InventoryUtil.SlotLocation.DUMMY, ClickType.CREATIVE);

        long time = System.currentTimeMillis();
        Inventory inventory;
        InventoryUtil.SlotLocation slotLocation;
        ClickType clickType;

        public static InventoryClick fromClickEvent(final InventoryClickEvent event)
        {
            val slotLocation = Optional.ofNullable(InventoryUtil.locateSlot(event.getRawSlot(), event.getClickedInventory().getType()));
            return new InventoryClick(event.getInventory(), slotLocation.orElse(InventoryUtil.SlotLocation.DUMMY), event.getClick());
        }

        public long timeOffset(@NotNull InventoryClick other)
        {
            val otime = other.getTime();
            return time < otime ? otime - time : time - otime;
        }
    }
}

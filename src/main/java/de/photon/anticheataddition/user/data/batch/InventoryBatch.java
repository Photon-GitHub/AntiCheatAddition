package de.photon.anticheataddition.user.data.batch;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.datastructure.broadcast.Broadcaster;
import de.photon.anticheataddition.util.datastructure.dummy.DummyInventory;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import lombok.Value;
import lombok.val;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class InventoryBatch extends Batch<InventoryBatch.InventoryClick>
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
            Preconditions.checkNotNull(event, "Tried to create InventoryClick from null event.");
            Preconditions.checkNotNull(event.getClickedInventory(), "Tried to create InventoryClick from null event clickedInventory.");

            val slotLocation = InventoryUtil.INSTANCE.locateSlot(event.getRawSlot(), event.getClickedInventory());
            return new InventoryClick(event.getInventory(), slotLocation.orElse(InventoryUtil.SlotLocation.DUMMY), event.getClick());
        }

        public long timeOffset(@NotNull InventoryClick other)
        {
            return MathUtil.absDiff(time, other.getTime());
        }
    }
}

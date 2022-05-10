package de.photon.anticheataddition.user.data.batch;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.datastructure.dummy.DummyInventory;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import lombok.val;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class InventoryBatch extends Batch<InventoryBatch.InventoryClick>
{
    public static final EventBus INVENTORY_BATCH_EVENTBUS = new EventBus();

    public InventoryBatch(@NotNull User user)
    {
        super(INVENTORY_BATCH_EVENTBUS, user, 15, InventoryClick.DUMMY);
    }

    public record InventoryClick(long time, Inventory inventory, InventoryUtil.SlotLocation slotLocation, ClickType clickType)
    {
        public static final InventoryClick DUMMY = new InventoryClick(0, new DummyInventory(), InventoryUtil.SlotLocation.DUMMY, ClickType.CREATIVE);

        public static InventoryClick fromClickEvent(final InventoryClickEvent event)
        {
            Preconditions.checkNotNull(event, "Tried to create InventoryClick from null event.");
            Preconditions.checkNotNull(event.getClickedInventory(), "Tried to create InventoryClick from null event clickedInventory.");

            val slotLocation = InventoryUtil.INSTANCE.locateSlot(event.getRawSlot(), event.getClickedInventory());
            return new InventoryClick(System.currentTimeMillis(), event.getInventory(), slotLocation.orElse(InventoryUtil.SlotLocation.DUMMY), event.getClick());
        }

        public long timeOffset(@NotNull InventoryClick other)
        {
            return MathUtil.absDiff(time, other.time);
        }
    }
}

package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datastructures.Buffer;
import de.photon.AACAdditionPro.util.datawrappers.InventoryClick;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class InventoryData extends TimeData implements Listener
{
    /**
     * Used to record inventory interactions for training the neural net.
     */
    public final Buffer<InventoryClick> inventoryClicks = new Buffer<>(InventoryClick.SAMPLES);

    /**
     * The last slot a person clicked.<br>
     * This variable is used to prevent false positives based on spam-clicking one slot.
     */
    @Getter
    private int lastRawSlot = 0;

    @Getter
    private Material lastMaterial = Material.BEDROCK;

    public InventoryData(final User user)
    {
        super(user, 0, 0);
        AACAdditionPro.getInstance().registerListener(this);
    }

    public boolean notRecentlyOpened(final long milliseconds)
    {
        return !this.recentlyUpdated(0, milliseconds);
    }

    public boolean recentlyClicked(final long milliseconds)
    {
        return this.recentlyUpdated(1, milliseconds);
    }

    /**
     * Determines whether the {@link User} of this {@link de.photon.AACAdditionPro.user.Data} currently has an open inventory.
     */
    public boolean hasOpenInventory()
    {
        return this.getTimeStamp(0) != 0;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerDeathEvent event)
    {
        this.nullifyIfRefersToUser(event.getEntity().getUniqueId(), 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerRespawnEvent event)
    {
        this.nullifyIfRefersToUser(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final InventoryOpenEvent event)
    {
        // Removed theUser.getPlayer().getOpenInventory().getType() != InventoryType.CRAFTING.
        this.updateIfRefersToUser(event.getPlayer().getUniqueId(), 0);
    }

    // Low to be after the MultiInteract EventHandler.
    @EventHandler(priority = EventPriority.LOW)
    public void on(final InventoryClickEvent event)
    {
        if (this.getUser().refersToUUID(event.getWhoClicked().getUniqueId()) &&
            event.getSlotType() != InventoryType.SlotType.QUICKBAR)
        {
            // Only update if the inventory is currently closed to not interfere with opening time checks.
            if (!this.hasOpenInventory())
            {
                this.updateTimeStamp(0);
            }
            this.updateTimeStamp(1);
            this.lastRawSlot = event.getRawSlot();
            this.lastMaterial = event.getCurrentItem() == null ? Material.AIR : event.getCurrentItem().getType();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryCloseEvent event)
    {
        this.nullifyIfRefersToUser(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final PlayerTeleportEvent event)
    {
        this.nullifyIfRefersToUser(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler
    public void on(final PlayerChangedWorldEvent event)
    {
        this.nullifyIfRefersToUser(event.getPlayer().getUniqueId(), 0);
    }

    @Override
    public void unregister()
    {
        HandlerList.unregisterAll(this);
        super.unregister();
    }
}

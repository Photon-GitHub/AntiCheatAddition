package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class InventoryData extends TimeData
{
    static
    {
        AACAdditionPro.getInstance().registerListener(new InventoryDataUpdater());
    }

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
        // [0] = Time of opening the inventory (or first click)
        // [1] = Latest click
        super(user, 0, 0);
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

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class InventoryDataUpdater implements Listener
    {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onDeath(final PlayerDeathEvent event)
        {
            final User user = UserManager.getUser(event.getEntity().getUniqueId());

            if (user != null)
            {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onRespawn(final PlayerRespawnEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInteract(final PlayerInteractEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                    BlockUtils.CONTAINERS.contains(event.getClickedBlock().getType()))
                {
                    user.getInventoryData().updateTimeStamp(0);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInventoryOpen(final InventoryOpenEvent event)
        {
            // Removed theUser.getPlayer().getOpenInventory().getType() != InventoryType.CRAFTING.
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getInventoryData().updateTimeStamp(0);
            }
        }

        // Low to be after the MultiInteract EventHandler.
        @EventHandler(priority = EventPriority.LOW)
        public void onInventoryClick(final InventoryClickEvent event)
        {
            final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

            if (user != null)
            {
                if (event.getSlotType() != InventoryType.SlotType.QUICKBAR)
                {
                    // Only update if the inventory is currently closed to not interfere with opening time checks.
                    if (!user.getInventoryData().hasOpenInventory())
                    {
                        user.getInventoryData().updateTimeStamp(0);
                    }
                    user.getInventoryData().updateTimeStamp(1);
                    user.getInventoryData().lastRawSlot = event.getRawSlot();
                    user.getInventoryData().lastMaterial = event.getCurrentItem() == null ?
                                                           Material.AIR :
                                                           event.getCurrentItem().getType();
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onInventoryClose(final InventoryCloseEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onPlayerTeleport(final PlayerTeleportEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler
        public void onWorldChange(final PlayerChangedWorldEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }
    }
}

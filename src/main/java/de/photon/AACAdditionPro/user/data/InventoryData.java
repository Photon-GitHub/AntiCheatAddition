package de.photon.AACAdditionPro.user.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientCustomPayload;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
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
import org.bukkit.inventory.ItemStack;

public class InventoryData extends TimeData
{
    static {
        new InventoryDataUpdater();
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
     * A singleton class to reduce the required {@link Listener}s to a minimum.
     */
    private static class InventoryDataUpdater implements Listener
    {
        public InventoryDataUpdater()
        {
            AACAdditionPro.getInstance().registerListener(this);

            // No longer needed in 1.13.2, thus only legacy handling
            // On 1.13.2 this is handled by the InventoryCloseEvent
            if (ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion())) {
                ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.CUSTOM_PAYLOAD)
                {
                    @Override
                    public void onPacketReceiving(PacketEvent event)
                    {
                        final WrapperPlayClientCustomPayload customPayloadWrapper = new WrapperPlayClientCustomPayload(event.getPacket());

                        if (!event.isCancelled() &&
                            // No longer needed in 1.13.2, thus only legacy handling
                            "MC|Beacon".equals(customPayloadWrapper.getChannel().getLegacyName()))
                        {
                            final User user = UserManager.getUser(event.getPlayer().getUniqueId());
                            if (user != null) {
                                // User has made a beacon action/transaction so the inventory must internally be closed this way as
                                // no InventoryCloseEvent is fired.
                                user.getInventoryData().nullifyTimeStamp(0);
                            }
                        }
                    }
                });
            }
        }

        // Event handling
        @EventHandler(priority = EventPriority.MONITOR)
        public void onDeath(final PlayerDeathEvent event)
        {
            final User user = UserManager.getUser(event.getEntity().getUniqueId());

            if (user != null) {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInteract(final PlayerInteractEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                final Material clickedMaterial = event.getClickedBlock().getType();

                if (BlockUtils.CONTAINERS.contains(clickedMaterial)) {
                    // Make sure that obstructed containers are handled correctly.
                    if (BlockUtils.FREE_SPACE_CONTAINERS.contains(clickedMaterial)) {
                        final Block aboveBlock = event.getClickedBlock().getRelative(BlockFace.UP);

                        // Make sure that the block above is not obstructed by blocks
                        if (!(aboveBlock.isEmpty() ||
                              aboveBlock.isPassable() ||
                              aboveBlock.getType() == Material.CHEST ||
                              aboveBlock.getType() == Material.TRAPPED_CHEST ||
                              aboveBlock.getType().name().endsWith("_SLAB") ||
                              aboveBlock.getType().name().endsWith("_STAIRS")))
                        {
                            return;
                        }

                        // Make sure that the block above is not obstructed by cats
                        switch (ServerVersion.getActiveServerVersion()) {
                            case MC188:
                            case MC112:
                                // Cannot check for cats as the server version doesn't provide the newer methods.
                                break;
                            case MC113:
                                // Make sure that the block above is not obstructed by cats
                                if (!aboveBlock.getWorld().getNearbyEntities(aboveBlock.getLocation(), 0.5, 1, 0.5, entity -> entity.getType() == EntityType.OCELOT).isEmpty()) {
                                    return;
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unknown minecraft version");
                        }

                    }

                    // Make sure that the container is opened and the player doesn't just place a block next to it.
                    boolean sneakingRequiredToPlaceBlock = false;
                    for (ItemStack handStack : InventoryUtils.getHandContents(event.getPlayer())) {
                        // Check if the material is a placable block
                        if (handStack.getType().isBlock()) {
                            sneakingRequiredToPlaceBlock = true;
                            break;
                        }
                    }

                    // Not sneaking when the player can place a block that way.
                    if (!(sneakingRequiredToPlaceBlock && event.getPlayer().isSneaking())) {
                        user.getInventoryData().updateTimeStamp(0);
                    }
                }
            }
        }

        // Low to be after the MultiInteract EventHandler.
        @EventHandler(priority = EventPriority.LOW)
        public void onInventoryClick(final InventoryClickEvent event)
        {
            final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

            if (user != null &&
                // Quickbar actions can be performed outside the inventory.
                event.getSlotType() != InventoryType.SlotType.QUICKBAR)
            {
                // Only update if the inventory is currently closed to not interfere with opening time checks.
                if (!user.getInventoryData().hasOpenInventory()) {
                    user.getInventoryData().updateTimeStamp(0);
                }
                user.getInventoryData().updateTimeStamp(1);
                user.getInventoryData().lastRawSlot = event.getRawSlot();
                user.getInventoryData().lastMaterial = event.getCurrentItem() == null ?
                                                       Material.AIR :
                                                       event.getCurrentItem().getType();
            }

        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onInventoryClose(final InventoryCloseEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInventoryOpen(final InventoryOpenEvent event)
        {
            // Removed theUser.getPlayer().getOpenInventory().getType() != InventoryType.CRAFTING.
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getInventoryData().updateTimeStamp(0);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onRespawn(final PlayerRespawnEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onTeleport(final PlayerTeleportEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }

        @EventHandler
        public void onWorldChange(final PlayerChangedWorldEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null) {
                user.getInventoryData().nullifyTimeStamp(0);
            }
        }
    }
}

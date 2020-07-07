package de.photon.aacadditionpro.user;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.user.subdata.KeepAliveData;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayPosition;
import de.photon.aacadditionpro.util.packetwrappers.WrapperPlayKeepAlive;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerKeepAlive;
import de.photon.aacadditionpro.util.world.BlockUtils;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A singleton to update the data in {@link de.photon.aacadditionpro.AACAdditionPro}s internal data storage.
 */
public final class DataUpdaterEvents implements Listener
{
    public static final DataUpdaterEvents INSTANCE = new DataUpdaterEvents();

    private final VelocityChangeDataUpdater velocityChangeDataUpdater;
    private final KeepAliveDataUpdater keepAliveDataUpdater;

    private DataUpdaterEvents()
    {
        this.keepAliveDataUpdater = new KeepAliveDataUpdater();
        this.velocityChangeDataUpdater = new VelocityChangeDataUpdater();
        ProtocolLibrary.getProtocolManager().addPacketListener(this.keepAliveDataUpdater);
        ProtocolLibrary.getProtocolManager().addPacketListener(this.velocityChangeDataUpdater);
    }

    public void register()
    {
        AACAdditionPro.getInstance().registerListener(this);
    }

    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_CONSUME_EVENT);
            user.getDataMap().setValue(DataKey.LAST_CONSUMED_ITEM_STACK, event.getItem());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event)
    {
        final User user = UserManager.getUser(event.getEntity().getUniqueId());

        if (user != null) {
            user.getTimestampMap().nullifyTimeStamp(TimestampKey.INVENTORY_OPENED);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
    {
        // Was hit
        if (event.getEntity() instanceof HumanEntity) {
            final User user = UserManager.getUser(event.getEntity().getUniqueId());

            if (user != null) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.TEAMING_COMBAT_TAG);
            }
        }

        // Hit somebody else
        if (event.getDamager() instanceof HumanEntity) {
            final User user = UserManager.getUser(event.getEntity().getUniqueId());

            if (user != null) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.TEAMING_COMBAT_TAG);
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(final PlayerInteractEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if ((user != null) && (event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            // Make sure that the block can open an InventoryView.
            BlockUtils.isInventoryOpenable(event.getClickedBlock()))
        {
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
                user.getTimestampMap().updateTimeStamp(TimestampKey.INVENTORY_OPENED);
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
            if (!user.hasOpenInventory()) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.INVENTORY_OPENED);
            }

            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_INVENTORY_CLICK);
            if (event.getCurrentItem() != null) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_INVENTORY_CLICK_ON_ITEM);
            }

            user.getDataMap().setValue(DataKey.LAST_RAW_SLOT_CLICKED, event.getRawSlot());
            user.getDataMap().setValue(DataKey.LAST_MATERIAL_CLICKED, event.getCurrentItem() == null ?
                                                                      Material.AIR :
                                                                      event.getCurrentItem().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getTimestampMap().nullifyTimeStamp(TimestampKey.INVENTORY_OPENED);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryOpen(final InventoryOpenEvent event)
    {
        // Removed theUser.getPlayer().getOpenInventory().getType() != InventoryType.CRAFTING.
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getTimestampMap().updateTimeStamp(TimestampKey.INVENTORY_OPENED);
        }
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null) {
            return;
        }

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_RIGHT_CLICK_EVENT);

            if (event.getMaterial().isEdible()) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_RIGHT_CLICK_CONSUMABLE_ITEM_EVENT);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(final PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            // Head + normal movement
            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_HEAD_OR_OTHER_MOVEMENT);

            // xz movement only
            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getZ() != event.getTo().getZ())
            {
                user.getTimestampMap().updateTimeStamps(TimestampKey.LAST_XYZ_MOVEMENT, TimestampKey.LAST_XZ_MOVEMENT);
            }
            // Any non-head movement.
            else if (event.getFrom().getY() != event.getTo().getY()) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_XYZ_MOVEMENT);
            }

            // Slime block -> Tower slime jump
            if (event.getFrom().getY() < event.getTo().getY()
                && event.getFrom().getBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK)
            {
                // Custom formula fitted from test data. Capped to make sure that cheat clients cannot give themselves infinite protection millis.
                // 2000 is already unreasonable, even for very fast block placing.
                user.getTimestampMap().setValue(TimestampKey.TOWER_SLIME_JUMP, System.currentTimeMillis() + Math.min((long) (550 * (event.getTo().getY() - event.getFrom().getY()) + 75), 2000));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getTimestampMap().nullifyTimeStamp(TimestampKey.INVENTORY_OPENED);
            user.getTimestampMap().updateTimeStamps(TimestampKey.LAST_RESPAWN, TimestampKey.LAST_TELEPORT);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(final PlayerTeleportEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getTimestampMap().nullifyTimeStamp(TimestampKey.INVENTORY_OPENED);
            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_TELEPORT);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onToggleSneak(final PlayerToggleSneakEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getDataMap().setValue(DataKey.SNEAKING, event.isSneaking());
            if (!event.isSneaking()) {
                user.getDataMap().setValue(DataKey.LAST_SNEAK_DURATION, user.getTimestampMap().passedTime(TimestampKey.LAST_SNEAK_TOGGLE));
            }
            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_SNEAK_TOGGLE);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onToggleSprint(final PlayerToggleSprintEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getDataMap().setValue(DataKey.SPRINTING, event.isSprinting());
            if (!event.isSprinting()) {
                user.getDataMap().setValue(DataKey.LAST_SPRINT_DURATION, user.getTimestampMap().passedTime(TimestampKey.LAST_SPRINT_TOGGLE));
            }
            user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_SPRINT_TOGGLE);
        }
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (user != null) {
            user.getTimestampMap().nullifyTimeStamp(TimestampKey.INVENTORY_OPENED);
            user.getTimestampMap().updateTimeStamps(TimestampKey.LAST_TELEPORT, TimestampKey.LAST_WORLD_CHANGE);
        }
    }

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class VelocityChangeDataUpdater extends PacketAdapter
    {
        private VelocityChangeDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            final User user = PacketListenerModule.safeGetUserFromEvent(event);

            if (user == null) {
                return;
            }

            if (user != null) {
                user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_VELOCITY_CHANGE);

                // The player wasn't hurt and got velocity for that.
                if (user.getPlayer().getNoDamageTicks() == 0
                    // Recent teleports can cause bugs
                    && !user.hasTeleportedRecently(1000))
                {
                    final IWrapperPlayPosition position = event::getPacket;

                    final boolean updatedPositiveVelocity = user.getPlayer().getLocation().getY() < position.getY();

                    if (updatedPositiveVelocity != user.getDataMap().getBoolean(DataKey.POSITIVE_VELOCITY)) {
                        user.getDataMap().setValue(DataKey.POSITIVE_VELOCITY, updatedPositiveVelocity);
                        user.getTimestampMap().updateTimeStamp(TimestampKey.LAST_VELOCITY_CHANGE_NO_EXTERNAL_CAUSES);
                    }
                }
            }
        }
    }

    private static class KeepAliveDataUpdater extends PacketAdapter
    {
        private KeepAliveDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Server.KEEP_ALIVE);
        }

        @Override
        public void onPacketSending(final PacketEvent event)
        {
            if (event.isPlayerTemporary()) {
                return;
            }

            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (user == null) {
                return;
            }

            final WrapperPlayKeepAlive wrapper = new WrapperPlayServerKeepAlive(event.getPacket());

            // Register the KeepAlive
            synchronized (user.getKeepAliveData().getKeepAlives()) {
                user.getKeepAliveData().getKeepAlives().bufferObject(new KeepAliveData.KeepAlivePacketData(wrapper.getKeepAliveId()));
            }
        }
    }
}

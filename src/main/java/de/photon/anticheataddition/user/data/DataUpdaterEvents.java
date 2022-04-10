package de.photon.anticheataddition.user.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayPosition;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A singleton to update the data in {@link AntiCheatAddition}s internal data storage.
 */
public final class DataUpdaterEvents implements Listener
{
    public static final DataUpdaterEvents INSTANCE = new DataUpdaterEvents();

    private DataUpdaterEvents()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(new VelocityChangeDataUpdater());
    }

    public void register()
    {
        AntiCheatAddition.getInstance().registerListener(this);
    }

    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        user.getTimestampMap().at(TimeKey.CONSUME_EVENT).update();
        user.getDataMap().setObject(DataKey.Obj.LAST_CONSUMED_ITEM_STACK, event.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event)
    {
        val user = User.getUser(event.getEntity().getUniqueId());
        if (user != null) user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).setToZero();
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
    {
        // Was hit
        if (event.getEntity() instanceof HumanEntity) {
            val user = User.getUser(event.getEntity().getUniqueId());
            if (user == null) return;

            user.getTimestampMap().at(TimeKey.TEAMING_COMBAT_TAG).update();
        }

        // Hit somebody else
        if (event.getDamager() instanceof HumanEntity) {
            val user = User.getUser(event.getEntity().getUniqueId());
            if (user == null) return;

            user.getTimestampMap().at(TimeKey.TEAMING_COMBAT_TAG).update();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event)
    {
        val user = User.getUser(event.getEntity().getUniqueId());
        if (user == null) return;

        user.getTimestampMap().at(TimeKey.FOOD_LEVEL_CHANGE).update();

        // Gain food level.
        if (user.getPlayer().getFoodLevel() < event.getFoodLevel()) user.getTimestampMap().at(TimeKey.FOOD_LEVEL_GAINED).update();
        else user.getTimestampMap().at(TimeKey.FOOD_LEVEL_LOST).update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(final PlayerInteractEvent event)
    {
        val user = User.getUser(event.getPlayer());
        val clickedBlock = event.getClickedBlock();

        // Make sure that we right-click a block.
        // clickedBlock == null should be impossible when the action is RIGHT_CLICK_BLOCK, but better ensure that.
        if (user == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null) return;

        // One can open the block.
        if (WorldUtil.INSTANCE.isInventoryOpenable(clickedBlock)) {
            // Make sure that the container is opened and the player doesn't just place a block next to it.
            // Check if the material is a placeable block
            val blockInHand = InventoryUtil.getHandContents(event.getPlayer()).stream().map(ItemStack::getType).anyMatch(Material::isBlock);

            // If the player is sneaking and has a block in hand, they place the block instead of opening an inventory.
            if (!blockInHand || !event.getPlayer().isSneaking()) user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).update();
        }
    }

    // Low to be after the MultiInteract EventHandler.
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(final InventoryClickEvent event)
    {
        val user = User.getUser(event.getWhoClicked().getUniqueId());
        // Quickbar actions can be performed outside the inventory.
        if (user == null || event.getSlotType() == InventoryType.SlotType.QUICKBAR) return;

        // Only update if the inventory is currently closed to not interfere with opening time checks.
        if (!user.hasOpenInventory()) user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).update();

        user.getTimestampMap().at(TimeKey.INVENTORY_CLICK).update();
        if (event.getCurrentItem() != null) user.getTimestampMap().at(TimeKey.INVENTORY_CLICK_ON_ITEM).update();


        user.getDataMap().setInt(DataKey.Int.LAST_RAW_SLOT_CLICKED, event.getRawSlot());
        user.getDataMap().setObject(DataKey.Obj.LAST_MATERIAL_CLICKED, event.getCurrentItem() == null ?
                                                                       Material.AIR :
                                                                       event.getCurrentItem().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (user != null) user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).setToZero();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryOpen(final InventoryOpenEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (user != null) user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).update();
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user != null) user.getTimestampMap().at(TimeKey.HOTBAR_SWITCH).update();
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            user.getTimestampMap().at(TimeKey.RIGHT_CLICK_EVENT).update();

            if (!MaterialUtil.isAir(event.getMaterial())) {
                user.getTimestampMap().at(TimeKey.RIGHT_CLICK_ITEM_EVENT).update();
                if (event.getMaterial().isEdible()) user.getTimestampMap().at(TimeKey.RIGHT_CLICK_CONSUMABLE_ITEM_EVENT).update();
            }

            if (event.getItem() != null && event.getItem().getType() == MaterialUtil.EXPERIENCE_BOTTLE) user.getTimestampMap().at(TimeKey.EXPERIENCE_BOTTLE_THROWN).update();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(final PlayerMoveEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null || event.getTo() == null) return;

        // Head + normal movement
        user.getTimestampMap().at(TimeKey.HEAD_OR_OTHER_MOVEMENT).update();

        // xz movement only
        if (event.getFrom().getX() != event.getTo().getX() ||
            event.getFrom().getZ() != event.getTo().getZ())
        {
            user.getTimestampMap().at(TimeKey.XYZ_MOVEMENT).update();
            user.getTimestampMap().at(TimeKey.XZ_MOVEMENT).update();
        }
        // Any non-head movement.
        else if (event.getFrom().getY() != event.getTo().getY()) {
            user.getTimestampMap().at(TimeKey.XYZ_MOVEMENT).update();
        }

        // Slime / Bed block -> Tower bounce jump
        if (event.getFrom().getY() < event.getTo().getY() && MaterialUtil.BOUNCE_MATERIALS.contains(event.getFrom().getBlock().getRelative(BlockFace.DOWN).getType())) {
            // Custom formula fitted from test data. Capped to make sure that cheat clients cannot give themselves infinite protection millis.
            // 2000 is already unreasonable, even for very fast block placing.
            user.getTimestampMap().at(TimeKey.TOWER_BOUNCE).setToFuture(Math.min((long) (550 * (event.getTo().getY() - event.getFrom().getY()) + 75), 2000L));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).setToZero();
        user.getTimestampMap().at(TimeKey.TELEPORT).update();
        user.getTimestampMap().at(TimeKey.RESPAWN).update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(final PlayerTeleportEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).setToZero();
        user.getTimestampMap().at(TimeKey.TELEPORT).update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onToggleSneak(final PlayerToggleSneakEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        val sneak = event.isSneaking();
        user.getDataMap().setBoolean(DataKey.Bool.SNEAKING, sneak);
        if (sneak) {
            user.getTimestampMap().at(TimeKey.SNEAK_ENABLE).update();
        } else {
            user.getTimestampMap().at(TimeKey.SNEAK_DISABLE).update();
            user.getDataMap().setLong(DataKey.Long.LAST_SNEAK_DURATION, user.getTimestampMap().at(TimeKey.SNEAK_TOGGLE).passedTime());
        }

        user.getTimestampMap().at(TimeKey.SNEAK_TOGGLE).update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onToggleSprint(final PlayerToggleSprintEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        val sprint = event.isSprinting();
        user.getDataMap().setBoolean(DataKey.Bool.SPRINTING, sprint);
        if (!sprint) user.getDataMap().setLong(DataKey.Long.LAST_SPRINT_DURATION, user.getTimestampMap().at(TimeKey.SPRINT_TOGGLE).passedTime());
        user.getTimestampMap().at(TimeKey.SPRINT_TOGGLE).update();
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        user.getTimestampMap().at(TimeKey.INVENTORY_OPENED).setToZero();
        user.getTimestampMap().at(TimeKey.TELEPORT).update();
        user.getTimestampMap().at(TimeKey.WORLD_CHANGE).update();
    }

    /**
     * A singleton class to reduce the required {@link Listener}s to a minimum.
     */
    private static class VelocityChangeDataUpdater extends PacketAdapter
    {
        private VelocityChangeDataUpdater()
        {
            super(AntiCheatAddition.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            val user = User.safeGetUserFromPacketEvent(event);
            if (user == null) return;

            user.getTimestampMap().at(TimeKey.VELOCITY_CHANGE).update();

            // The player wasn't hurt and got velocity for that.
            if (user.getPlayer().getNoDamageTicks() == 0
                // Recent teleports can cause bugs
                && !user.hasTeleportedRecently(1000))
            {
                final IWrapperPlayPosition position = event::getPacket;
                val updatedPositiveVelocity = user.getPlayer().getLocation().getY() < position.getY();

                if (updatedPositiveVelocity != user.getDataMap().getBoolean(DataKey.Bool.POSITIVE_VELOCITY)) {
                    user.getDataMap().setBoolean(DataKey.Bool.POSITIVE_VELOCITY, updatedPositiveVelocity);
                    user.getTimestampMap().at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).update();
                }
            }
        }
    }
}

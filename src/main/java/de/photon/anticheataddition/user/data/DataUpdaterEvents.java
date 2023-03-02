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

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A singleton to update the data in {@link AntiCheatAddition}s internal data storage.
 */
@SuppressWarnings("MethodMayBeStatic")
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

    private static final Consumer<User> CLOSE_INVENTORY = user -> user.getTimeMap().at(TimeKey.INVENTORY_OPENED).setToZero();
    private static final Consumer<User> NOTHING = user -> {};

    private static void userUpdate(UUID uuid, Consumer<User> userAction, TimeKey... update)
    {
        final var user = User.getUser(uuid);
        if (user == null) return;

        userAction.accept(user);
        for (TimeKey key : update) user.getTimeMap().at(key).update();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(),
                   user -> user.getData().object.lastConsumedItemStack = event.getItem(),
                   TimeKey.CONSUME_EVENT);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event)
    {
        userUpdate(event.getEntity().getUniqueId(), CLOSE_INVENTORY);
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
    {
        // Was hit
        if (event.getEntity() instanceof HumanEntity) userUpdate(event.getEntity().getUniqueId(), NOTHING, TimeKey.COMBAT);

        // Hit somebody else
        if (event.getDamager() instanceof HumanEntity) userUpdate(event.getDamager().getUniqueId(), NOTHING, TimeKey.COMBAT);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event)
    {
        userUpdate(event.getEntity().getUniqueId(), NOTHING, TimeKey.FOOD_LEVEL_CHANGE);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(final PlayerInteractEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        final var clickedBlock = event.getClickedBlock();

        // Make sure that we right-click a block.
        // clickedBlock == null should be impossible when the action is RIGHT_CLICK_BLOCK, but better ensure that.
        if (user == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null) return;

        // One can open the block.
        if (WorldUtil.INSTANCE.isInventoryOpenable(clickedBlock)) {
            // Make sure that the container is opened and the player doesn't just place a block next to it.
            // Check if the material is a placeable block
            final var blockInHand = InventoryUtil.INSTANCE.getHandContents(event.getPlayer()).stream().map(ItemStack::getType).anyMatch(Material::isBlock);

            // If the player is sneaking and has a block in hand, they place the block instead of opening an inventory.
            if (!blockInHand || !event.getPlayer().isSneaking()) user.getTimeMap().at(TimeKey.INVENTORY_OPENED).update();
        }
    }

    // Low to be after the MultiInteract EventHandler.
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(final InventoryClickEvent event)
    {
        final var user = User.getUser(event.getWhoClicked().getUniqueId());
        // Hotbar actions can be performed outside the inventory.
        if (user == null || event.getSlotType() == InventoryType.SlotType.QUICKBAR) return;

        // Only update if the inventory is currently closed to not interfere with opening time checks.
        if (!user.hasOpenInventory()) user.getTimeMap().at(TimeKey.INVENTORY_OPENED).update();

        user.getTimeMap().at(TimeKey.INVENTORY_CLICK).update();
        if (event.getCurrentItem() != null) user.getTimeMap().at(TimeKey.INVENTORY_CLICK_ON_ITEM).update();

        user.getData().number.lastRawSlotClicked = event.getRawSlot();
        user.getData().object.lastMaterialClicked = event.getCurrentItem() == null ? Material.AIR : event.getCurrentItem().getType();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(), CLOSE_INVENTORY);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryOpen(final InventoryOpenEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(), NOTHING, TimeKey.INVENTORY_OPENED);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(), NOTHING, TimeKey.HOTBAR_SWITCH);
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (user == null) return;

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            user.getTimeMap().at(TimeKey.RIGHT_CLICK_EVENT).update();

            if (!MaterialUtil.isAir(event.getMaterial())) {
                user.getTimeMap().at(TimeKey.RIGHT_CLICK_ITEM_EVENT).update();
                if (event.getMaterial().isEdible()) user.getTimeMap().at(TimeKey.RIGHT_CLICK_CONSUMABLE_ITEM_EVENT).update();
            }

            if (event.getItem() != null && event.getItem().getType() == MaterialUtil.EXPERIENCE_BOTTLE) user.getTimeMap().at(TimeKey.EXPERIENCE_BOTTLE_THROWN).update();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(final PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (user == null || event.getTo() == null) return;

        // Head + normal movement
        user.getTimeMap().at(TimeKey.HEAD_OR_OTHER_MOVEMENT).update();

        // xz movement only
        if (event.getFrom().getX() != event.getTo().getX() ||
            event.getFrom().getZ() != event.getTo().getZ())
        {
            user.getTimeMap().at(TimeKey.XYZ_MOVEMENT).update();
            user.getTimeMap().at(TimeKey.XZ_MOVEMENT).update();
        }
        // Any non-head movement.
        else if (event.getFrom().getY() != event.getTo().getY()) {
            user.getTimeMap().at(TimeKey.XYZ_MOVEMENT).update();
        }

        // Slime / Bed block -> Tower bounce jump
        if (event.getFrom().getY() < event.getTo().getY() && MaterialUtil.BOUNCE_MATERIALS.contains(event.getFrom().getBlock().getRelative(BlockFace.DOWN).getType())) {
            // Custom formula fitted from test data. Capped to make sure that cheat clients cannot give themselves infinite protection millis.
            // 2000 is already unreasonable, even for very fast block placing.
            user.getTimeMap().at(TimeKey.TOWER_BOUNCE).setToFuture(Math.min((long) (550 * (event.getTo().getY() - event.getFrom().getY()) + 75), 2000L));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(), CLOSE_INVENTORY, TimeKey.TELEPORT, TimeKey.RESPAWN);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(final PlayerTeleportEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(), CLOSE_INVENTORY, TimeKey.TELEPORT);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onToggleSneak(final PlayerToggleSneakEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (user == null) return;

        final boolean sneak = event.isSneaking();
        user.getData().bool.sneaking = sneak;
        if (sneak) {
            user.getTimeMap().at(TimeKey.SNEAK_ENABLE).update();
        } else {
            user.getTimeMap().at(TimeKey.SNEAK_DISABLE).update();
            user.getData().number.lastSneakDuration = user.getTimeMap().at(TimeKey.SNEAK_TOGGLE).passedTime();
        }

        user.getTimeMap().at(TimeKey.SNEAK_TOGGLE).update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onToggleSprint(final PlayerToggleSprintEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (user == null) return;

        final boolean sprint = event.isSprinting();
        user.getData().bool.sprinting = sprint;
        if (!sprint) user.getData().number.lastSprintDuration = user.getTimeMap().at(TimeKey.SPRINT_TOGGLE).passedTime();
        user.getTimeMap().at(TimeKey.SPRINT_TOGGLE).update();
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event)
    {
        userUpdate(event.getPlayer().getUniqueId(), CLOSE_INVENTORY, TimeKey.TELEPORT, TimeKey.WORLD_CHANGE);
    }

    /**
     * A singleton class to reduce the required {@link Listener}s to a minimum.
     */
    private static final class VelocityChangeDataUpdater extends PacketAdapter
    {
        private VelocityChangeDataUpdater()
        {
            super(AntiCheatAddition.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(final PacketEvent event)
        {
            final var user = User.safeGetUserFromPacketEvent(event);
            if (user == null) return;

            user.getTimeMap().at(TimeKey.VELOCITY_CHANGE).update();

            // The player wasn't hurt and got velocity for that.
            if (user.getPlayer().getNoDamageTicks() == 0
                // Recent teleports can cause bugs
                && !user.hasTeleportedRecently(1000))
            {
                final IWrapperPlayPosition position = event::getPacket;
                final boolean movingUpwards = user.getPlayer().getLocation().getY() < position.getY();

                if (movingUpwards != user.getData().bool.positiveVelocity) {
                    user.getData().bool.positiveVelocity = movingUpwards;
                    user.getTimeMap().at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).update();
                }
            }
        }
    }
}

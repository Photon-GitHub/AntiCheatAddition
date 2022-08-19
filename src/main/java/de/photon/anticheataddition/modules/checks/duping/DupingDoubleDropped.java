package de.photon.anticheataddition.modules.checks.duping;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class DupingDoubleDropped extends ViolationModule implements Listener
{
    public static final DupingDoubleDropped INSTANCE = new DupingDoubleDropped();

    private DupingDoubleDropped()
    {
        super("Duping.parts.DoubleDropped");
    }

    private void handlePickup(UUID uuid, ItemStack pickupStack)
    {
        val user = User.getUser(uuid);
        if (User.isUserInvalid(user, this)) return;

        final Material itemMaterial = pickupStack.getType();
        final int itemCount = pickupStack.getAmount();
        final int droppedCount = user.getData().number.dupingDoubleItemsDropped;

        if (itemMaterial == user.getData().object.dupingDoubleDroppedMaterial) {
            user.getData().number.dupingDoubleItemsCollected += itemCount;

            // Hard equivalence here instead of greater because of quickly dropping another item, then dropping one of this material again false positive.
            // Collected exactly twice the dropped amount.
            if (user.getData().number.dupingDoubleItemsCollected == (droppedCount << 1)) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(20)
                                              .setDebug(() -> "Duping-Debug | Player " +
                                                              user.getPlayer().getName() +
                                                              " collected exactly twice the dropped amount of item " + itemMaterial + " at " +
                                                              user.getPlayer().getLocation().toVector() +
                                                              " in world " + user.getPlayer().getWorld().getName()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        val droppedStack = event.getItemDrop().getItemStack();

        if (droppedStack.getType() == user.getData().object.dupingDoubleDroppedMaterial) {
            user.getData().number.dupingDoubleItemsDropped += droppedStack.getAmount();
        } else {
            user.getData().object.dupingDoubleDroppedMaterial = droppedStack.getType();
            user.getData().number.dupingDoubleItemsDropped = droppedStack.getAmount();
            user.getData().number.dupingDoubleItemsCollected = 0;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        for (Item item : event.getItems()) {
            if (item.getItemStack().getType() == user.getData().object.dupingDoubleDroppedMaterial) {
                user.getData().number.dupingDoubleItemsDropped += item.getItemStack().getAmount();
            }
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(1200, 20).build();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addListeners(ServerVersion.ACTIVE == ServerVersion.MC18 ? new AncientPickupListener() : new ModernPickupListener())
                           .build();
    }

    /**
     * Class for the modern event to prevent errors on 1.8.9.
     */
    private class ModernPickupListener implements Listener
    {
        @EventHandler(ignoreCancelled = true)
        public void onEntityPickupItem(EntityPickupItemEvent event)
        {
            if (event.getEntity().getType() != EntityType.PLAYER) return;

            handlePickup(event.getEntity().getUniqueId(), event.getItem().getItemStack());
        }
    }

    /**
     * Class for the ancient version 1.8.9 as it does not have the {@link EntityPickupItemEvent}.
     */
    private class AncientPickupListener implements Listener
    {
        @EventHandler(ignoreCancelled = true)
        public void onPlayerPickupItem(PlayerPickupItemEvent event)
        {
            handlePickup(event.getPlayer().getUniqueId(), event.getItem().getItemStack());
        }
    }
}

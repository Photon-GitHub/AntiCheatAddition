package de.photon.anticheataddition.modules.checks.autototem;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Detects automatic refilling of the offhand with a totem of undying shortly
 * after one was consumed. The check listens for hand swap or inventory events
 * that place a totem in the offhand and compares the timing with the last
 * recorded totem use.
 */
public final class AutoTotem extends ViolationModule implements Listener
{
    public static final AutoTotem INSTANCE = new AutoTotem();

    /**
     * Threshold in milliseconds for how quickly a totem may reappear in the offhand
     * after one has been consumed before a flag is raised.
     */
    private final int minRefillDelay = loadInt(".min_refill_delay", 150);

    private AutoTotem()
    {
        super("AutoTotem");
    }

    /**
     * Records the usage of a totem of undying.
     */
    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event)
    {
        if (!(event.getEntity() instanceof Player player)) return;
        final var user = User.getUser(player);
        if (User.isUserInvalid(user, this)) return;

        user.getTimeMap().at(TimeKey.AUTOTOTEM_TOTEM_USE).update();
    }

    private void checkForFastRefill(Player player)
    {
        final var user = User.getUser(player);
        if (User.isUserInvalid(user, this)) return;

        // Player must have a totem in the offhand
        if (player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING
            // And the last totem use was recorded recently.
            && user.getTimeMap().at(TimeKey.AUTOTOTEM_TOTEM_USE).recentlyUpdated(minRefillDelay)) {
            getManagement().flag(Flag.of(user).setAddedVl(20));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event)
    {
        checkForFastRefill(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        checkForFastRefill(player);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(2400L, 20).build();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC111.getSupVersionsFrom())
                           .build();
    }
}

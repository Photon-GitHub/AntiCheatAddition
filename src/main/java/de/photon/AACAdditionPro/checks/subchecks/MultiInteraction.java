package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class MultiInteraction implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;
    @LoadFromConfiguration(configPath = ".max_ping")
    private double max_ping;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double min_tps;
    @LoadFromConfiguration(configPath = ".min_time")
    private int min_time;

    //Priority below the priority in InventoryData of the InvClickListener required, otherwise every click will be flagged
    @EventHandler(priority = EventPriority.LOWEST)
    public void on(final InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        // Creative-clear might trigger this.
        if ((user.getPlayer().getGameMode() == GameMode.SURVIVAL || user.getPlayer().getGameMode() == GameMode.ADVENTURE) &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            AACAPIProvider.getAPI().getTPS() > min_tps &&
            // Minimum ping
            (max_ping < 0 || AACAPIProvider.getAPI().getPing(user.getPlayer()) <= max_ping) &&
            // A valid material was moved
            event.getCurrentItem() != null &&
            event.getCurrentItem().getType() != Material.AIR &&
            // False positive: Click-spamming on the same slot
            event.getRawSlot() != user.getInventoryData().getLastRawSlot() &&
            // Too fast after the last ClickEvent (Detection)
            user.getInventoryData().recentlyClicked(min_time))
        {
            boolean flag = false;

            switch (event.getAction())
            {
                // ------------------------------------------ Exemptions -------------------------------------------- //
                // Nothing happens, therefore exempted
                case NOTHING:
                    // False positive with fast clicking of numbers
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                    // Unknown reason might not be save to handle
                case UNKNOWN:
                    // False positive with collecting all items of one type in the inventory
                case COLLECT_TO_CURSOR:
                    return;

                // ------------------------------------------ Normal -------------------------------------------- //
                case DROP_ALL_SLOT:
                case DROP_ONE_SLOT:
                case PICKUP_ALL:
                case PICKUP_SOME:
                case PICKUP_HALF:
                case PICKUP_ONE:
                case PLACE_ALL:
                case PLACE_SOME:
                case PLACE_ONE:
                case DROP_ALL_CURSOR:
                case DROP_ONE_CURSOR:
                case CLONE_STACK:
                    flag = true;
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    flag = user.getInventoryData().lastMaterial != event.getCurrentItem().getType();
                    break;
                case SWAP_WITH_CURSOR:
                    // No much use besides the armour environment for cheats
                    if (event.getSlotType() == InventoryType.SlotType.ARMOR ||
                        // No false positives possible in fuel or crafting slot as it is only one slot which is separated from others
                        event.getSlotType() == InventoryType.SlotType.FUEL ||
                        event.getSlotType() == InventoryType.SlotType.RESULT)
                    {
                        flag = true;
                        break;
                    }
            }

            if (flag)
            {
                vlManager.flag(user.getPlayer(), cancel_vl, () ->
                {
                    event.setCancelled(true);
                    InventoryUtils.syncUpdateInventory(user.getPlayer());
                }, () -> {});
            }

            // Update the material as the shift-all items causes false positives
            user.getInventoryData().lastMaterial = event.getCurrentItem().getType();
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.MULTI_INTERACTION;
    }
}

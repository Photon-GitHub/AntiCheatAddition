package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.minecraft.ping.PingProvider;
import de.photon.aacadditionpro.util.minecraft.tps.TPSProvider;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryMultiInteraction extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".max_ping")
    private int maxPing;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    public InventoryMultiInteraction()
    {
        super("Inventory.parts.MultiInteraction");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        val user = User.getUser(event.getWhoClicked().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        if (event.getClickedInventory() != null &&
            // Creative-clear might trigger this.
            user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            TPSProvider.INSTANCE.atLeastTPS(minTps) &&
            // Minimum ping
            PingProvider.INSTANCE.maxPingHandling(user.getPlayer(), maxPing) &&
            // False positive: Click-spamming on the same slot
            event.getRawSlot() != user.getDataMap().getInt(DataKey.IntegerKey.LAST_RAW_SLOT_CLICKED))
        {
            // Default vl to 6
            int addedVl = 6;
            // Time in ms that will flag if it has not passed
            int enforcedTicks = 0;

            switch (event.getAction()) {
                // ------------------------------------------ Exemptions -------------------------------------------- //
                case NOTHING:
                    // Nothing happens, therefore exempted
                case UNKNOWN:
                    // Unknown reason might not be safe to handle
                case COLLECT_TO_CURSOR:
                    // False positive with collecting all items of one type in the inventory
                case DROP_ALL_SLOT:
                case DROP_ONE_SLOT:
                    // False positives due to autodropping feature of minecraft when holding q
                    return;
                // ------------------------------------------ Normal -------------------------------------------- //
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                    addedVl = 1;
                    enforcedTicks = 1;
                    // Enough distance to keep false positives at bay.
                    if (InventoryUtil.distanceBetweenSlots(event.getRawSlot(), user.getDataMap().getInt(DataKey.IntegerKey.LAST_RAW_SLOT_CLICKED), event.getClickedInventory().getType()) >= 3) return;
                    break;

                case PICKUP_ALL:
                case PICKUP_SOME:
                case PICKUP_HALF:
                case PICKUP_ONE:
                case PLACE_ALL:
                case PLACE_SOME:
                case PLACE_ONE:
                    // No false positives to check for.
                    addedVl = 8;

                    enforcedTicks = (InventoryUtil.distanceBetweenSlots(event.getRawSlot(), user.getDataMap().getInt(DataKey.IntegerKey.LAST_RAW_SLOT_CLICKED), event.getClickedInventory().getType()) < 4) ? 1 : 5;
                    break;

                case DROP_ALL_CURSOR:
                case DROP_ONE_CURSOR:
                case CLONE_STACK:
                    // No false positives to check for.
                    enforcedTicks = 4;
                    break;

                case MOVE_TO_OTHER_INVENTORY:
                    // Last material false positive due to the fast move all items shortcut.
                    if (event.getCurrentItem() == null || user.getDataMap().getObject(DataKey.ObjectKey.LAST_MATERIAL_CLICKED) == event.getCurrentItem().getType()) return;

                    // Depending on the distance of the clicks.
                    enforcedTicks = (InventoryUtil.distanceBetweenSlots(event.getRawSlot(), user.getDataMap().getInt(DataKey.IntegerKey.LAST_RAW_SLOT_CLICKED), event.getClickedInventory().getType()) < 4) ? 1 : 2;
                    break;

                case SWAP_WITH_CURSOR:
                    switch (event.getSlotType()) {
                        // Armor slots are not eligible for fewer ticks as of quick change problems with the feet slot.
                        // No false positives possible in fuel or crafting slot as it is only one slot which is separated from others
                        case FUEL:
                        case RESULT:
                            enforcedTicks = 4;
                            break;

                        // Default tested value.
                        default:
                            enforcedTicks = 2;
                            break;
                    }
                    break;
            }

            // Convert ticks to millis.
            // 25 to account for server lag.
            if (user.hasClickedInventoryRecently(25L + (enforcedTicks * 50))) {
                this.getManagement().flag(Flag.of(user).setAddedVl(addedVl).setCancelAction(cancelVl, () -> {
                    event.setCancelled(true);
                    InventoryUtil.syncUpdateInventory(user.getPlayer());
                }).setDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " moved items too quickly."));
            }
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 1).build();
    }
}

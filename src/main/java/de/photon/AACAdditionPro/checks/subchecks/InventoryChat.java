package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class InventoryChat implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 600);

    private int cancel_vl;

    @EventHandler
    public void on(final AsyncPlayerChatEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        // Is in Inventory (Detection)
        if (user.getInventoryData().hasOpenInventory() &&
            // Have the inventory opened for some time
            user.getInventoryData().notRecentlyOpened(1000))
        {
            vlManager.flag(user.getPlayer(), cancel_vl, () -> event.setCancelled(true), () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.INVENTORY_CHAT;
    }

    @Override
    public void subEnable()
    {
        this.cancel_vl = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".cancel_vl");
    }
}

package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;

public class Freecam implements ViolationModule
{
    @LoadFromConfiguration(configPath = ".idle_time")
    private static int idle_time;
    private int task_number;

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FREECAM;
    }

    @Override
    public void subEnable()
    {
        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () ->
                {
                    for (final User user : UserManager.getUsersUnwrapped())
                    {
                        if (    // Not bypassed
                                !user.isBypassed() &&
                                // Not in a vehicle
                                !user.getPlayer().isInsideVehicle() &&
                                // Not in an inventory
                                !user.getInventoryData().hasOpenInventory() &&
                                user.getPlayer().getOpenInventory().getType() == InventoryType.CRAFTING &&
                                // No item on the cursor
                                user.getPlayer().getItemOnCursor().getType() == Material.AIR &&
                                // Not moving
                                !user.getPositionData().hasPlayerMovedRecently(idle_time, true))
                        {
                            final WrapperPlayServerPosition setBackPacketWrapper = new WrapperPlayServerPosition();
                            setBackPacketWrapper.setAllFlags();
                            setBackPacketWrapper.sendPacket(user.getPlayer());
                        }
                    }
                }, 0L, AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".frequency"));
    }

    static int getIdle_time()
    {
        return idle_time;
    }

    @Override
    public void subDisable()
    {
        Bukkit.getScheduler().cancelTask(task_number);
    }
}

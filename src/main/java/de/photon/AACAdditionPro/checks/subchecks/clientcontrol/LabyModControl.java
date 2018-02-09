package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import net.labymod.serverapi.Permission;
import net.labymod.serverapi.bukkit.event.LabyModPlayerJoinEvent;
import net.labymod.serverapi.bukkit.event.PermissionsSendEvent;
import org.bukkit.event.EventHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabyModControl implements ClientControlModule
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    private final Map<Permission, Boolean> featureMap = new HashMap<>();

    @Override
    public void subEnable()
    {
        for (String key : ConfigUtils.loadKeys(this.getModuleType().getConfigString() + ".disable"))
        {
            featureMap.put(Permission.valueOf(key.toUpperCase()),
                           AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable." + key));
        }
    }

    @EventHandler
    public void onLabyModPlayerJoinEvent(LabyModPlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user))
        {
            return;
        }

        this.executeCommands(event.getPlayer());
    }

    @EventHandler
    public void onPermissionsSend(PermissionsSendEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user))
        {
            return;
        }

        // Iterating through all permissions
        for (Map.Entry<Permission, Boolean> permissionEntry : event.getPermissions().entrySet())
        {
            // Allow by default.
            permissionEntry.setValue(featureMap.getOrDefault(permissionEntry.getKey(), true));
        }
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.LABYMOD_CONTROL;
    }
}

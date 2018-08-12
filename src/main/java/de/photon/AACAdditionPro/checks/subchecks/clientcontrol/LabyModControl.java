package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import net.labymod.serverapi.Permission;
import net.labymod.serverapi.bukkit.event.LabyModPlayerJoinEvent;
import net.labymod.serverapi.bukkit.event.PermissionsSendEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LabyModControl implements Listener, ClientControlModule
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
                           !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable." + key));
        }
    }

    @EventHandler
    public void onLabyModPlayerJoinEvent(LabyModPlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        this.executeCommands(event.getPlayer());
    }

    @EventHandler
    public void onPermissionsSend(PermissionsSendEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType()))
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
    public Set<String> getDependencies()
    {
        return ImmutableSet.of("LabyModAPI");
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.LABYMOD_CONTROL;
    }
}

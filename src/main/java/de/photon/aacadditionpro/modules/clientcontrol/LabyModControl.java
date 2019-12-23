package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Dependency;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import net.labymod.serverapi.Permission;
import net.labymod.serverapi.bukkit.event.LabyModPlayerJoinEvent;
import net.labymod.serverapi.bukkit.event.PermissionsSendEvent;
import org.bukkit.event.EventHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class LabyModControl extends ClientControlModule implements ListenerModule, Dependency
{
    private final Map<Permission, Boolean> featureMap = new EnumMap<>(Permission.class);

    @Override
    public void enable()
    {
        for (String key : ConfigUtils.loadKeys(this.getModuleType().getConfigString() + ".disable")) {
            featureMap.put(Permission.valueOf(key.toUpperCase()),
                           !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable." + key));
        }
    }

    @EventHandler
    public void onLabyModPlayerJoinEvent(LabyModPlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        this.executeCommands(event.getPlayer());
    }

    @EventHandler
    public void onPermissionsSend(PermissionsSendEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Iterating through all permissions
        for (Map.Entry<Permission, Boolean> permissionEntry : event.getPermissions().entrySet()) {
            // Allow by default.
            permissionEntry.setValue(featureMap.getOrDefault(permissionEntry.getKey(), true));
        }
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

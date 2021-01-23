package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.Dependency;
import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.files.configs.ConfigUtils;
import lombok.Getter;
import net.labymod.serverapi.Permission;
import net.labymod.serverapi.bukkit.event.LabyModPlayerJoinEvent;
import net.labymod.serverapi.bukkit.event.PermissionsSendEvent;
import org.bukkit.event.EventHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class LabyModControl extends ClientControlModule implements ListenerModule, Dependency
{
    @Getter
    private static final LabyModControl instance = new LabyModControl();

    // Do not init here as that will cause errors as Permission cannot be found.
    private Map<Permission, Boolean> featureMap;

    @Override
    public void enable()
    {
        featureMap = new EnumMap<>(Permission.class);
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

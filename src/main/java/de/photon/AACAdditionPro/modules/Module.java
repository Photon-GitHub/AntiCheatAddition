package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.List;

public interface Module
{
    /**
     * This enables the check according to its interfaces.
     */
    static void enableModule(final Module module)
    {
        try
        {
            // ServerVersion check
            if (module instanceof RestrictedServerVersionModule && RestrictedServerVersionModule.allowedToStart((RestrictedServerVersionModule) module))
            {
                VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " is not compatible with your server version.");
                return;
            }

            // Enabled
            if (!AACAdditionPro.getInstance().getConfig().getBoolean(module.getConfigString() + ".enabled"))
            {
                VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " was chosen not to be enabled.");
                return;
            }

            // Dependency check
            if (module instanceof DependencyModule && !DependencyModule.allowedToStart((DependencyModule) module))
            {
                VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " has been not been enabled as of missing dependencies.");
                return;
            }

            // Config-Annotation processing
            LoadFromConfiguration annotation;
            for (Field field : module.getClass().getDeclaredFields())
            {
                // Load the annotation and check if it is present.
                annotation = field.getAnnotation(LoadFromConfiguration.class);

                if (annotation == null)
                    continue;

                // Make it possible to modify the field
                field.setAccessible(true);

                // Get the full config path.
                final String path = module.getConfigString() + annotation.configPath();

                // Get the type of the field.
                final Class clazz = field.getType();

                // The different classes
                try
                {
                    // Boolean
                    if (clazz == boolean.class || clazz == Boolean.class)
                        field.setBoolean(module, AACAdditionPro.getInstance().getConfig().getBoolean(path));
                        // Numbers
                    else if (clazz == double.class || clazz == Double.class)
                        field.setDouble(module, AACAdditionPro.getInstance().getConfig().getDouble(path));
                    else if (clazz == int.class || clazz == Integer.class)
                        field.setInt(module, AACAdditionPro.getInstance().getConfig().getInt(path));
                    else if (clazz == long.class || clazz == Long.class)
                        field.setLong(module, AACAdditionPro.getInstance().getConfig().getLong(path));
                    else if (clazz == String.class)
                        field.set(module, AACAdditionPro.getInstance().getConfig().getString(path));
                        // Special stuff
                    else if (clazz == ItemStack.class)
                        field.set(module, AACAdditionPro.getInstance().getConfig().getItemStack(path));
                    else if (clazz == Color.class)
                        field.set(module, AACAdditionPro.getInstance().getConfig().getColor(path));
                    else if (clazz == OfflinePlayer.class)
                        field.set(module, AACAdditionPro.getInstance().getConfig().getOfflinePlayer(path));
                    else if (clazz == Vector.class)
                        field.set(module, AACAdditionPro.getInstance().getConfig().getVector(path));
                        // Lists
                    else if (clazz == List.class)
                    {
                        // StringLists
                        if (annotation.listType() == String.class)
                        {
                            field.set(module, ConfigUtils.loadStringOrStringList(path));

                            // Unknown type
                        }
                        else
                        {
                            field.set(module, AACAdditionPro.getInstance().getConfig().getList(path));
                        }

                    }
                    // No special type found
                    else
                    {
                        field.set(module, AACAdditionPro.getInstance().getConfig().get(path));
                    }
                } catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
            }

            if (module instanceof ListenerModule)
                ListenerModule.enable((ListenerModule) module);

            if (module instanceof PacketListenerModule)
                PacketListenerModule.enable((PacketListenerModule) module);

            if (module instanceof PluginMessageListenerModule)
                PluginMessageListenerModule.enable((PluginMessageListenerModule) module);

            module.enable();
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " has been enabled.");
        } catch (final Exception e)
        {
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " could not be enabled.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * This disables the check according to its interfaces.
     */
    static void disableModule(final Module module)
    {
        try
        {
            if (module instanceof ListenerModule)
                ListenerModule.disable((ListenerModule) module);

            if (module instanceof PacketListenerModule)
                PacketListenerModule.disable((PacketListenerModule) module);

            if (module instanceof PluginMessageListenerModule)
                PluginMessageListenerModule.disable((PluginMessageListenerModule) module);

            module.disable();
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " has been disabled.");
        } catch (final Exception e)
        {
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " could not be disabled.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * All additional chores during enabling that are not handled by the {@link Module} - subinterfaces.
     */
    default void enable() {}

    /**
     * All additional chores during disabling that are not handled by the {@link Module} - subinterfaces.
     */
    default void disable() {}

    /**
     * Gets the direct path representing this module in the config.
     */
    default String getConfigString()
    {
        return this.getModuleType().getConfigString();
    }

    /**
     * Gets the {@link ModuleType} of this {@link Module}
     */
    ModuleType getModuleType();
}

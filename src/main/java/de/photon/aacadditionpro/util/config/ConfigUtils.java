package de.photon.aacadditionpro.util.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigUtils
{
    /**
     * This will process the {@link LoadFromConfiguration} annotation to load all config values.
     *
     * @param object  the object in which the config values should be loaded.
     * @param prePath the first path of the part (e.g. helpful when using {@link Module#getConfigString()})
     */
    public static void processLoadFromConfiguration(final Object object, String prePath)
    {
        // Config-Annotation processing
        LoadFromConfiguration annotation;
        for (Field field : object.getClass().getDeclaredFields()) {
            // Load the annotation and check if it is present.
            annotation = field.getAnnotation(LoadFromConfiguration.class);

            if (annotation == null) continue;

            // Make it possible to modify the field
            field.setAccessible(true);

            // Get the full config path.
            val path = StringUtils.defaultString(prePath) + annotation.configPath();

            // Get the type of the field.
            val type = field.getType();

            // The different classes
            try {
                // Boolean
                if (type == boolean.class || type == Boolean.class) {
                    field.setBoolean(object, AACAdditionPro.getInstance().getConfig().getBoolean(path));
                }
                // Numbers
                else if (type == double.class || type == Double.class) {
                    field.setDouble(object, AACAdditionPro.getInstance().getConfig().getDouble(path));
                } else if (type == int.class || type == Integer.class) {
                    field.setInt(object, AACAdditionPro.getInstance().getConfig().getInt(path));
                } else if (type == long.class || type == Long.class) {
                    field.setLong(object, AACAdditionPro.getInstance().getConfig().getLong(path));
                } else if (type == String.class) {
                    field.set(object, AACAdditionPro.getInstance().getConfig().getString(path));
                }
                // Special stuff
                else if (type == ItemStack.class) {
                    field.set(object, AACAdditionPro.getInstance().getConfig().getItemStack(path));
                } else if (type == Color.class) {
                    field.set(object, AACAdditionPro.getInstance().getConfig().getColor(path));
                } else if (type == OfflinePlayer.class) {
                    field.set(object, AACAdditionPro.getInstance().getConfig().getOfflinePlayer(path));
                } else if (type == Vector.class) {
                    field.set(object, AACAdditionPro.getInstance().getConfig().getVector(path));
                }
                // Lists
                else if (type == List.class) {
                    // StringLists
                    if (annotation.listType() == String.class) {
                        field.set(object, ConfigUtils.loadImmutableStringOrStringList(path));

                        // Unknown type
                    } else {
                        field.set(object, AACAdditionPro.getInstance().getConfig().getList(path));
                    }
                }
                // No special type found
                else {
                    field.set(object, AACAdditionPro.getInstance().getConfig().get(path));
                }
            } catch (IllegalAccessException e) {
                AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Unable to load config value due to unknown type.", e);
            }
        }
    }

    /**
     * Used to load a {@link List} of {@link String}s if it is uncertain if the value of path
     * is a {@link String} or a {@link List} of {@link String}s
     *
     * @param path the path which should be loaded
     *
     * @return an {@link ImmutableList} of {@link String}s with the path as entries.
     */
    @NotNull
    public static List<String> loadImmutableStringOrStringList(@NotNull final String path)
    {
        // Command list
        val input = AACAdditionPro.getInstance().getConfig().getStringList(path);

        // Single command
        if (input.isEmpty()) {
            val possibleCommand = AACAdditionPro.getInstance().getConfig().getString(path);

            // No-command indicator or null
            return possibleCommand == null || "{}".equals(possibleCommand) ?
                   List.of() :
                   List.of(possibleCommand);
        }

        // Input is not empty
        // No-command indicator
        return "{}".equals(input.get(0)) ? List.of() : ImmutableList.copyOf(input);
    }

    /**
     * Tries to load all keys from a path in the config.
     *
     * @param sectionPath the given path to the section which keys should be loaded.
     *
     * @return a {@link Set} of {@link String}s that represent the keys
     *
     * @throws NullPointerException in the case that the loaded {@link ConfigurationSection} is null.
     */
    public static Set<String> loadKeys(final String sectionPath)
    {
        // Return all the keys of the provided section.
        return Preconditions.checkNotNull(AACAdditionPro.getInstance().getConfig().getConfigurationSection(sectionPath),
                                          "Config loading error: ConfigurationSection does not exist at " + sectionPath).getKeys(false);
    }
}

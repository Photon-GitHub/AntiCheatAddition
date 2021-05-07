package de.photon.aacadditionpro.util.config

import com.google.common.collect.ImmutableList
import de.photon.aacadditionproold.AACAdditionPro
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lombok.AccessLevel
import lombok.NoArgsConstructor
import org.bukkit.Color
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.logging.Level

@NoArgsConstructor(access = AccessLevel.PRIVATE)
object ConfigUtils {
    /**
     * This will process the [LoadFromConfiguration] annotation to load all config values.
     *
     * @param `object`  the object in which the config values should be loaded.
     * @param prePath the first path of the part (e.g. helpful when using [Module.getConfigString])
     */
    @JvmStatic
    fun processLoadFromConfiguration(classInstance: Any, prePath: String?) = runBlocking {
        for (field in classInstance.javaClass.declaredFields) {
            launch {
                // Load the annotation and check if it is present.
                val annotation: LoadFromConfiguration = field.getAnnotation(LoadFromConfiguration::class.java) ?: return@launch

                // Make it possible to modify the field
                field.isAccessible = true

                // Get the full config path.
                val path = prePath.orEmpty() + annotation.configPath

                // Get the type of the field.
                val type = field.type

                // The different classes
                try {
                    // Boolean
                    when (type) {
                        Boolean::class.javaPrimitiveType, Boolean::class.java -> field.setBoolean(classInstance, AACAdditionPro.getInstance().config.getBoolean(path))
                        Double::class.javaPrimitiveType, Double::class.java -> field.setDouble(classInstance, AACAdditionPro.getInstance().config.getDouble(path))
                        Int::class.javaPrimitiveType, Int::class.java -> field.setInt(classInstance, AACAdditionPro.getInstance().config.getInt(path))
                        Long::class.javaPrimitiveType, Long::class.java -> field.setLong(classInstance, AACAdditionPro.getInstance().config.getLong(path))
                        String::class.java -> field[classInstance] = AACAdditionPro.getInstance().config.getString(path)
                        ItemStack::class.java -> field[classInstance] = AACAdditionPro.getInstance().config.getItemStack(path)
                        Color::class.java -> field[classInstance] = AACAdditionPro.getInstance().config.getColor(path)
                        OfflinePlayer::class.java -> field[classInstance] = AACAdditionPro.getInstance().config.getOfflinePlayer(path)
                        Vector::class.java -> field[classInstance] = AACAdditionPro.getInstance().config.getVector(path)
                        MutableList::class.java -> {
                            field[classInstance] = when (annotation.listType) {
                                // StringLists
                                String::class.java -> loadImmutableStringOrStringList(path)
                                // Other lists.
                                else -> AACAdditionPro.getInstance().config.getList(path)
                            }
                        }
                        // Some other object we don't know of.
                        else -> field[classInstance] = AACAdditionPro.getInstance().config[path]
                    }
                } catch (e: IllegalAccessException) {
                    AACAdditionPro.getInstance().logger.log(Level.SEVERE, "Unable to load config value due to unknown type.", e)
                }
            }
        }
    }

    /**
     * Used to load a [List] of [String]s if it is uncertain if the value of path
     * is a [String] or a [List] of [String]s
     *
     * @param path the path which should be loaded
     *
     * @return an [ImmutableList] of [String]s with the path as entries.
     */
    @JvmStatic
    fun loadImmutableStringOrStringList(path: String): List<String> {
        // Command list
        val input = AACAdditionPro.getInstance().config.getStringList(path)

        // Single command
        if (input.isEmpty()) {
            val possibleCommand = AACAdditionPro.getInstance().config.getString(path)

            // No-command indicator or null
            return if (possibleCommand == null || "{}" == possibleCommand) ImmutableList.of() else ImmutableList.of(possibleCommand)
        }

        // Input is not empty
        // No-command indicator
        return if ("{}" == input[0]) ImmutableList.of() else ImmutableList.copyOf(input)
    }

    /**
     * Tries to load all keys from a path in the config.
     *
     * @param sectionPath the given path to the section which keys should be loaded.
     *
     * @return a [Set] of [String]s that represent the keys
     *
     * @throws NullPointerException in the case that the loaded [ConfigurationSection] is null.
     */
    @JvmStatic
    fun loadKeys(sectionPath: String): Set<String> {
        // Return all the keys of the provided section.
        return checkNotNull(AACAdditionPro.getInstance().config.getConfigurationSection(sectionPath)) { "Config loading error: ConfigurationSection does not exist at $sectionPath" }.getKeys(false)
    }
}
package de.photon.AACAdditionPro.util.files;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigUtils
{
    /**
     * Used to load a {@link List} of {@link String}s if it is uncertain if the value of path
     * is a {@link String} or a {@link List} of {@link String}s
     *
     * @param path the path which should be loaded
     *
     * @return a {@link List} of {@link String}s with the path as entries.
     */
    public static List<String> loadStringOrStringList(final String path)
    {
        // Command list
        final List<String> input = AACAdditionPro.getInstance().getConfig().getStringList(path);

        // Single command
        if (input.isEmpty()) {
            final String possibleCommand = AACAdditionPro.getInstance().getConfig().getString(path);

            if (possibleCommand.equals("") || possibleCommand.equals("{}")) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(AACAdditionPro.getInstance().getConfig().getString(path));
            }
        }

        return input;
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
        //Generate a ConfigurationSection that contains all keys
        final ConfigurationSection configurationSection = AACAdditionPro.getInstance().getConfig().getConfigurationSection(sectionPath);

        // Loading error when Config-Section is null
        if (configurationSection == null) {
            throw new NullPointerException("Severe loading error: Config-Section is null when loading: " + sectionPath);
        }

        // Return the Set of keys
        return configurationSection.getKeys(false);
    }

    /**
     * Tries to load all thresholds from the given config key.
     *
     * @param thresholdSectionPath the given path to the section that contains the thresholds
     *
     * @return a {@link Map} where the keys are {@link Integer}s and representing the threshold and Objects that are {@link List}s of {@link String}(s) which contain the commands that should be run when triggering the threshold.
     */
    public static ConcurrentHashMap<Integer, List<String>> loadThresholds(final String thresholdSectionPath)
    {
        final Set<String> keys = loadKeys(thresholdSectionPath);

        // Loading error when the Set of keys is null
        if (keys == null) {
            throw new NullPointerException("Severe loading error: Keys are null when loading: " + thresholdSectionPath);
        }

        // Create the Map the thresholds will be put in
        final ConcurrentHashMap<Integer, List<String>> thresholds = new ConcurrentHashMap<>(keys.size(), 1);

        for (final String s : keys) {
            final int testedConfidence = Integer.parseInt(s);

            //Put the command into thresholds
            thresholds.put(testedConfidence, loadStringOrStringList(thresholdSectionPath + "." + testedConfidence));
        }

        return thresholds;
    }
}

package de.photon.aacadditionpro.util.files.configs;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * {@link ConfigurationRepresentation} is a class to represent a {@link YamlConfiguration} with comments.
 * Changes to the internal {@link YamlConfiguration} will be written through to the actual {@link File] upon saving.
 */
public class ConfigurationRepresentation
{
    private final File configFile;
    @Getter
    private final YamlConfiguration yamlConfiguration;
    private final Map<String, Object> requestedChanges = new HashMap<>();


    public ConfigurationRepresentation(File configFile)
    {
        this.configFile = configFile;
        yamlConfiguration = YamlConfiguration.loadConfiguration(this.configFile);
    }

    /**
     * @return the line number of a path.
     */
    private static int searchForPath(List<String> configLines, String path)
    {
        // Special handling for paths without a '.'
        final String[] pathParts = path.split("\\.");

        int currentPart = 0;
        int depthOfCurrentPart = 0;
        int currentLine = 0;
        int depthOfCurrentLine;

        for (String configLine : configLines) {
            depthOfCurrentLine = StringUtil.depth(configLine);

            // Value could not be found as not all parts are existing.
            Preconditions.checkArgument(depthOfCurrentPart <= depthOfCurrentLine, "Path " + path + " could not be found.");

            if (!isComment(configLine) && configLine.contains(pathParts[currentPart])) {
                // Update depth
                depthOfCurrentPart = depthOfCurrentLine;

                // Found the whole path?
                if (++currentPart >= pathParts.length) {
                    return currentLine;
                }
            }

            ++currentLine;
        }

        throw new IllegalArgumentException("Path " + path + " could not be found (full iteration).");
    }

    // Start at 1 because the initial line is always affected.
    private static int affectedLines(final List<String> configLines, final int initialLine, final Predicate<String> loopBreak)
    {
        int affectedLines = 0;

        // + 1 as the initial line should not be iterated over.
        final ListIterator<String> listIterator = configLines.listIterator(initialLine + 1);
        String configLine;
        while (listIterator.hasNext()) {
            configLine = listIterator.next();

            // ":" is the indicator of a new value
            if (loopBreak.test(configLine)) {
                break;
            }

            ++affectedLines;
        }
        return affectedLines;
    }

    private static boolean isComment(final String string)
    {
        return string != null && (string.isEmpty() || string.contains("#"));
    }

    public void requestValueChange(final String path, final Object value)
    {
        this.requestedChanges.put(path, value);
    }

    public void save() throws IOException
    {
        // Directly inject changes.
        if (requestedChanges.isEmpty()) return;

        // Load the whole config.
        // Use LinkedList for fast mid-config tampering.
        final List<String> configLines = Files.readAllLines(this.configFile.toPath());

        requestedChanges.forEach((path, value) -> {
            int initialLineIndex = searchForPath(configLines, path);
            int affectedLines = affectedLines(configLines, initialLineIndex, line -> isComment(line) || line.indexOf(':') != -1);

            // Remove old values
            for (int lines = affectedLines; lines > 0; lines--) {
                configLines.remove(initialLineIndex + 1);
            }

            // Change the initialLine to remove the old value
            String initialLine = configLines.get(initialLineIndex);
            // + 1 in order to not delete the ':' char.
            initialLine = initialLine.substring(0, initialLine.lastIndexOf(':') + 1);

            // Set the new value.
            // Simple sets
            if (value instanceof Boolean ||
                value instanceof Byte ||
                value instanceof Short ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Float ||
                value instanceof Double)
            {
                initialLine += ' ';
                initialLine += value.toString();
            } else if (value instanceof String) {
                initialLine += ' ';
                initialLine += '\"';
                initialLine += ((String) value);
                initialLine += '\"';
            } else if (value instanceof List) {
                List list = (List) value;

                if (list.isEmpty()) {
                    initialLine += " []";
                } else {
                    int entryDepth = StringUtil.depth(initialLine);
                    final StringBuilder preStringBuilder = new StringBuilder();

                    while (entryDepth-- > 0) {
                        preStringBuilder.append(' ');
                    }
                    preStringBuilder.append("- ");

                    final String preString = preStringBuilder.toString();

                    for (Object o : list) {
                        configLines.add(initialLineIndex + 1, preString + o.toString());
                    }
                }
            } else if (value instanceof ConfigActions) {
                switch ((ConfigActions) value) {
                    case DELETE_KEYS:
                        initialLine += " {}";
                        final int initialLineDepth = StringUtil.depth(initialLine);
                        int affectedKeyLines = affectedLines(configLines, initialLineIndex, line -> StringUtil.depth(line) <= initialLineDepth);

                        // Remove old values
                        for (int lines = affectedKeyLines; lines > 0; lines--) {
                            configLines.remove(initialLineIndex + 1);
                        }
                        break;
                }
            }
            configLines.set(initialLineIndex, initialLine);
        });

        Files.write(this.configFile.toPath(), configLines);
    }

    public enum ConfigActions
    {
        DELETE_KEYS
    }
}

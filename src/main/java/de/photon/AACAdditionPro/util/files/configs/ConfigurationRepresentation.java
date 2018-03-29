package de.photon.AACAdditionPro.util.files.configs;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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


    public ConfigurationRepresentation(File configFile, byte depthLevel)
    {
        this.configFile = configFile;
        yamlConfiguration = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public void requestValueChange(final String key, final Object value)
    {
        this.requestedChanges.put(key, value);
    }

    public void save() throws IOException
    {
        // Directly inject changes.
        if (requestedChanges.isEmpty())
        {
            return;
        }

        // Load the whole config.
        // Use LinkedList for fast mid-config tampering.
        final List<String> configLines = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.configFile)))
        {
            String line = br.readLine();

            while (line != null)
            {
                configLines.add(line);
                line = br.readLine();
            }
        }

        requestedChanges.forEach((path, value) -> {
            int initialLineIndex = searchForPath(configLines, path);
            int affectedLines = affectedLines(configLines, initialLineIndex);

            // Remove old value
            if (affectedLines > 1)
            {
                // > 1 because the initial line should not be removed.
                for (int lines = affectedLines; lines > 1; lines--)
                {
                    configLines.remove(initialLineIndex + 1);
                }
            }

            // Change the initalLine to remove the old value
            String initialLine = configLines.get(initialLineIndex);
            // + 1 in order to not delete the ':' char.
            initialLine = initialLine.substring(0, initialLine.lastIndexOf(':') + 1);
            // Add one whitespace

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
            }
            else if (value instanceof List)
            {
                List list = (List) value;

                if (list.isEmpty())
                {
                    initialLine += " []";
                }
                else
                {
                    short entryDepth = depth(initialLine);
                    final StringBuilder preStringBuilder = new StringBuilder();

                    while (entryDepth-- > 0)
                    {
                        preStringBuilder.append(' ');
                    }
                    preStringBuilder.append("- ");

                    final String preString = preStringBuilder.toString();

                    for (Object o : list)
                    {
                        configLines.add(initialLineIndex + 1, preString + o.toString());
                    }
                }
            }
            configLines.set(initialLineIndex, initialLine);
        });
    }

    /**
     * @return the line number of a path.
     */
    private static int searchForPath(List<String> configLines, String path)
    {
        final String[] pathParts = path.split(".");
        int currentPart = 0;
        short minDepth = 0;
        int currentLineIndex = 0;

        for (String configLine : configLines)
        {
            final short currentDepth = depth(configLine);

            // Value could not be found as not all parts are existing.
            if (minDepth > currentDepth)
            {
                throw new IllegalArgumentException("Path " + path + " could not be found.");
            }

            if (configLine.contains(pathParts[currentPart]))
            {
                // Update depth
                minDepth = currentDepth;

                // Found the whole path?
                if (++currentPart >= pathParts.length)
                {
                    return currentLineIndex;
                }
            }

            currentLineIndex++;
        }

        throw new IllegalArgumentException("Path " + path + " could not be found (full iteration).");
    }

    // Start at 1 because the initial line is always affected.
    private static int affectedLines(List<String> configLines, int initialLine)
    {
        int affectedLines = 1;

        final ListIterator<String> listIterator = configLines.listIterator(initialLine);
        String configLine;
        while (listIterator.hasNext())
        {
            configLine = listIterator.next();

            // ":" is the indicator of a new value
            if (configLine.indexOf(':') != -1)
            {
                break;
            }
            affectedLines++;
        }
        return affectedLines;
    }

    /**
     * Counts the leading whitespaces of a {@link String}
     *
     * @return the amount of leading whitespaces.
     */
    private static short depth(final String string)
    {
        final char[] chars = string.toCharArray();
        for (short i = 0; i < chars.length; i++)
        {
            if (chars[i] != ' ')
            {
                return i;
            }
        }
        return 0;
    }

    private static boolean isComment(final String string)
    {
        return string != null && (string.isEmpty() || string.contains("#"));
    }
}

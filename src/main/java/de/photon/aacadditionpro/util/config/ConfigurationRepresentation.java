package de.photon.aacadditionpro.util.config;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationRepresentation
{
    @NotNull private final File configFile;
    @Getter(lazy = true) private final YamlConfiguration yamlConfiguration = loadYaml();
    private final Map<String, Object> requestedChanges = new HashMap<>();

    public ConfigurationRepresentation(@NotNull File configFile)
    {
        this.configFile = Preconditions.checkNotNull(configFile, "Tried to create ConfigurationRepresentation of null config file.");
    }

    private static boolean isComment(final String string)
    {
        // <= Because a '#' at a later point indicates some data before as leading whitespaces are removed.
        return string == null || string.isEmpty() || string.trim().indexOf('#') <= 0;
    }

    private static void deleteLines(final List<String> lines, int startPosition, int lineCount)
    {
        while (lineCount-- > 0) lines.remove(startPosition);
    }

    private static int linesOfKey(final List<String> lines, int firstLineOfKey)
    {
        final int depthOfKey = StringUtil.depth(lines.get(firstLineOfKey));
        return (int) lines.stream()
                          // Skip firstLineOfKey to get to the first line and 1 as the initial line should not be iterated over to avoid stopping there in takeWhile.
                          .skip(firstLineOfKey + 1L)
                          // Smaller or equal depth is the indicator of a new key, and we only want the lines of the current key.
                          .takeWhile(line -> StringUtil.depth(line) > depthOfKey)
                          // + 1 as the first line is always there.
                          .count() + 1;
    }

    private YamlConfiguration loadYaml()
    {
        return YamlConfiguration.loadConfiguration(this.configFile);
    }

    private int searchForPath(@NotNull List<String> lines, @NotNull String path)
    {
        val pathParts = path.trim().split("\\.");
        int partIndex = 0;
        int lineIndex = 0;
        int partDepth = 0;
        int lineDepth;

        String trimmed;
        for (String line : lines) {
            lineDepth = StringUtil.depth(line);

            // The sub-part we search for does not exist.
            Preconditions.checkArgument(partDepth <= lineDepth, "Path " + path + " could not be found.");

            trimmed = line.trim();
            // New "deeper" subpart found?
            if (!isComment(trimmed) && trimmed.startsWith(pathParts[partIndex])) {
                partDepth = lineDepth;
                // Whole path found.
                if (++partIndex == pathParts.length) return lineIndex;
            }
            ++lineIndex;
        }
        throw new IllegalArgumentException("Path " + path + " could not be found (full iteration).");
    }

    public synchronized void requestValueChange(final String path, final Object value)
    {
        this.requestedChanges.put(path, value);
    }

    public synchronized void save() throws IOException
    {
        // Directly inject changes.
        if (requestedChanges.isEmpty()) return;

        // Load the whole config.
        val configLines = new ArrayList<>(Files.readAllLines(this.configFile.toPath()));

        requestedChanges.forEach((path, value) -> {
            val lineIndexOfKey = searchForPath(configLines, path);
            val originalLine = configLines.get(lineIndexOfKey);
            val affectedLines = linesOfKey(configLines, lineIndexOfKey);

            // We want to delete all lines after the initial one.
            deleteLines(configLines, lineIndexOfKey + 1, affectedLines - 1);

            // Add the removed ':' right back.
            val replacementLine = new StringBuilder(StringUtils.substringBeforeLast(originalLine, ":")).append(':');

            // Set the new value.
            if (value instanceof Boolean || value instanceof Number) replacementLine.append(' ').append(value);
            else if (value instanceof String) replacementLine.append(" \"").append(value).append('\"');
            else if (value instanceof List) {
                val list = (List<?>) value;

                if (list.isEmpty()) replacementLine.append(" []");
                else {
                    val preString = StringUtils.leftPad("- ", StringUtil.depth(originalLine));
                    for (Object o : list) configLines.add(lineIndexOfKey + 1, preString + o);
                }
            }

            configLines.set(lineIndexOfKey, replacementLine.toString());
        });

        Files.write(this.configFile.toPath(), configLines);
    }
}

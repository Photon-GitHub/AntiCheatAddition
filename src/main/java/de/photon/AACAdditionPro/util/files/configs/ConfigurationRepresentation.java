package de.photon.AACAdditionPro.util.files.configs;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * {@link ConfigurationRepresentation} is a class to represent a {@link YamlConfiguration} with comments.
 * Changes to the internal {@link YamlConfiguration} will be written through to the actual {@link File] upon saving.
 */
public class ConfigurationRepresentation
{
    private final File configFile;
    @Getter
    private final YamlConfiguration yamlConfiguration;

    public ConfigurationRepresentation(File configFile)
    {
        this.configFile = configFile;
        yamlConfiguration = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public void save() throws IOException
    {
        // Map all comments to their keys
        final Map<String, List<String>> commentMap = new HashMap<>();

        String line;
        List<String> commentBlock = new ArrayList<>();
        Stack<String> currentPath = new Stack<>();
        // Reads from the file to get the comments
        try (BufferedReader br = new BufferedReader(new FileReader(this.configFile)))
        {
            while (true)
            {
                // Read the whole comment block and save it
                line = br.readLine();

                if (line == null)
                {
                    break;
                }

                // Copy whitespaces to make sure the format is correct.
                while (isComment(line))
                {
                    commentBlock.add(line);
                    line = br.readLine();
                }

                handlePath(line, currentPath);

                final StringBuilder pathBuilder = new StringBuilder();
                for (String s : currentPath)
                {
                    pathBuilder.append(s);
                    // No need for delimiters as it is only an internal path.
                }

                // The next line after the comments is a key.
                // null key are end-of-file comments
                commentMap.put(pathBuilder.toString(), commentBlock);

                // Don't clear as all HashMap entries will point at the same value ->
                // Same comment block over and over again.
                commentBlock = new ArrayList<>();
            }
        }

        // Clear the path for injection.
        currentPath.clear();

        // Now inject those comments into the changed YAML

        // The StringBuilder containing what should be written to the new file.
        final StringBuilder resultingConfiguration = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new StringReader(this.yamlConfiguration.saveToString())))
        {
            while (true)
            {
                // Read the whole comment block and save it
                line = br.readLine();

                if (line == null)
                {
                    break;
                }

                if (isComment(line))
                {
                    continue;
                }

                handlePath(line, currentPath);

                final StringBuilder pathBuilder = new StringBuilder();
                for (String s : currentPath)
                {
                    pathBuilder.append(s);
                    // No need for delimiters as it is only an internal path.
                }

                appendComments(resultingConfiguration, commentMap.get(pathBuilder.toString()));

                resultingConfiguration.append(line);
                resultingConfiguration.append('\n');
            }
        }

        // Delete old file
        if (!this.configFile.delete())
        {
            throw new IOException("Unable to delete file " + this.configFile.getName());
        }

        // Create new, empty file
        if (!this.configFile.createNewFile())
        {
            throw new IOException("Unable to create file " + this.configFile.getName());
        }

        // Write the contents of resultingConfiguration.
        final FileWriter fileWriter = new FileWriter(this.configFile);
        fileWriter.write(resultingConfiguration.toString());
        fileWriter.close();
    }

    private static void handlePath(String line, Stack<String> currentPath)
    {
        if (currentPath.isEmpty())
        {
            currentPath.push(line);
        }
        else
        {
            // Empty lines shall not change anything
            if (pathDepth(line) < 0)
            {
                return;
            }

            // Compare the line to the current path
            byte compareResult = (byte) (pathDepth(line) - pathDepth(currentPath.peek()));

            // Change the current path accordingly.
            if (compareResult > 0)
            {
                currentPath.push(line);
            }
            else
            {
                // < 1 because one additional removal is needed!
                for (byte result = compareResult; result < 1; result++)
                {
                    currentPath.pop();
                }
                currentPath.push(line);
            }
        }
    }

    /**
     * Appends a {@link List} of {@link String}s to a {@link StringBuilder} if the {@link List} is not <code>null<code/>
     */
    private void appendComments(StringBuilder sb, List<String> comments)
    {
        if (comments != null)
        {
            for (String comment : comments)
            {
                sb.append(comment);
                sb.append('\n');
            }
        }
    }

    private static byte pathDepth(final String string)
    {
        if (string == null || string.isEmpty())
        {
            return -1;
        }

        final char[] chars = string.toCharArray();
        for (byte b = 0; b < chars.length; b++)
        {
            if (chars[b] != ' ')
            {
                // index + 1 as it starts at 0
                // / 3 as three spaces equals one level.
                return (byte) ((b + 1) / 3);
            }
        }
        return -1;
    }

    private static boolean isComment(final String string)
    {
        return string != null && (string.isEmpty() || string.contains("#"));
    }
}

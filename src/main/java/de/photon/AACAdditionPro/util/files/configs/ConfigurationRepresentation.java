package de.photon.AACAdditionPro.util.files.configs;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ConfigurationRepresentation} is a class to represent a {@link YamlConfiguration} with comments.
 * Changes to the internal {@link YamlConfiguration} will be written through to the actual {@link File] upon saving.
 */
public class ConfigurationRepresentation
{
    private final File configFile;
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
        final List<String> commentBlock = new ArrayList<>();
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

                commentBlock.clear();
                while (line != null && line.trim().charAt(0) == '#')
                {
                    commentBlock.add(line);
                    line = br.readLine();
                }

                // The next line after the comments is a key.
                // null key are end-of-file comments
                commentMap.put(line, commentBlock);
            }
        }

        // Now inject those comments into the changed YAML

        // The StringBuilder containing what should be written to the new file.
        final StringBuilder resultingConfiguration = new StringBuilder();

        // The comment block (null?) of the current key.
        List<String> potentialComments;

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

                if (line.trim().charAt(0) == '#')
                {
                    continue;
                }

                potentialComments = commentMap.get(line);

                if (potentialComments != null)
                {
                    for (String comment : potentialComments)
                    {
                        resultingConfiguration.append(comment);
                        resultingConfiguration.append('\n');
                    }
                }

                resultingConfiguration.append(line);
                resultingConfiguration.append('\n');
            }
        }
    }
}

package de.photon.AACAdditionPro.util.files;

import com.google.common.base.Preconditions;
import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;

public final class FileUtilities
{
    /**
     * This {@link Method} saves a file in the directory of the {@link org.bukkit.plugin.Plugin}
     * on the server and writes content into it if a default resource exists in this plugin.
     * <p>
     * This method ensures that the given path exists.
     * If the path does not exist it tries to create it.
     * The path is appended to the DataFolder of AACAdditionPro.
     * The path must not end with '/'
     * <p>
     * If the file does not exist it tries to create it.
     *
     * @param resourcePath the full path that should be appended to the default data folder.
     */
    public static File saveFileInFolder(String resourcePath)
    {
        Preconditions.checkNotNull(resourcePath, "ResourcePath cannot be null");
        Preconditions.checkArgument(!"".equals(resourcePath), "ResourcePath cannot empty");

        resourcePath = resourcePath.replace('\\', '/');

        final File outFile = new File(AACAdditionPro.getInstance().getDataFolder(), resourcePath);

        // Create the file if it does not exist
        if (!outFile.exists()) {
            try (InputStream in = AACAdditionPro.getInstance().getResource(resourcePath)) {
                Files.copy(in, outFile.toPath());
            } catch (IOException exception) {
                // Could not create the file
                Bukkit.getLogger().severe("The file " + outFile.getName() + " could not be created in " + outFile.getPath());
                exception.printStackTrace();
            }
        }
        return outFile;
    }
}

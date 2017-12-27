package de.photon.AACAdditionPro.util.files;

import com.google.common.io.ByteStreams;
import de.photon.AACAdditionPro.AACAdditionPro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public final class FileUtilities
{
    public static final File AACADDITIONPRO_DATA_FOLDER = AACAdditionPro.getInstance().getDataFolder();

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
     *
     * @throws IOException in the case of a failed file / folder creation.
     */
    public static File saveFileInFolder(String resourcePath) throws IOException
    {
        if (resourcePath == null || resourcePath.equals(""))
        {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');

        File outFile = new File(AACADDITIONPRO_DATA_FOLDER, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(AACADDITIONPRO_DATA_FOLDER, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists())
        {
            if (!outDir.mkdirs())
            {
                throw new IOException("Unable to create the directory \"" + outDir.getPath() + "\"");
            }
        }

        // Create the file if it does not exist
        if (!outFile.exists())
        {
            if (!outFile.createNewFile())
            {
                // Could not create the file
                throw new IOException("The file " + outFile.getName() + " could not be created in " + outFile.getPath());
            }
            else
            {
                // Stream to read from the default-file
                final InputStream in = AACAdditionPro.getInstance().getResource(resourcePath);

                // Stream to write into the newly created file
                final OutputStream out = new FileOutputStream(outFile);

                if (in != null)
                {
                    // Write the content of the default file to the newly created file
                    ByteStreams.copy(in, out);
                }
            }
        }
        return outFile;
    }
}

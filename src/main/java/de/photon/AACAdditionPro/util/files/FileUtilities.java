package de.photon.AACAdditionPro.util.files;

import com.google.common.io.ByteStreams;
import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public final class FileUtilities
{
    public static final File AACADDITIONPRO_DATAFOLDER = AACAdditionPro.getInstance().getDataFolder();

    /**
     * This {@link Method} saves a file in the directory of the {@link Plugin}
     * on the server and writes content into it if a default resource exists in this plugin.
     * <p>
     * This method ensures that the given path exists.
     * If the path does not exist it tries to create it.
     * The path is appended to the DataFolder of AACAdditionPro.
     * The path must not end with '/'
     * <p>
     * If the file does not exist it tries to create it.
     *
     * @param file_name the name of the file that should be saved
     *
     * @throws IOException in the case of a failed file / folder creation.
     */
    public static File saveFileInFolder(final String file_name) throws IOException
    {return saveFileInFolder(file_name, AACADDITIONPRO_DATAFOLDER.getPath());}

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
     * @param file_name the name of the file that should be saved
     * @param path      the path that should be checked inside the DataFolder
     *
     * @throws IOException in the case of a failed file / folder creation.
     */
    public static File saveFileInFolder(final String file_name, final String path) throws IOException
    {
        final String[] pathParts = path.split("/");
        final StringBuilder currentPath = new StringBuilder(path.length());

        for (final String s : pathParts) {
            currentPath.append(s);
            currentPath.append("/");

            final File currentFile = new File(currentPath.toString());
            if (!currentFile.exists()) {
                // Create the folder if it does not exist
                if (!currentFile.mkdir()) {
                    throw new IOException("The folder " + currentFile.getPath() + " could not be created.");
                }
            }
        }

        // Create the filegei
        final File resourceFile = new File(path, file_name);

        // Does the config-file exist
        if (!resourceFile.exists()) {
            // Create the file if it does not exist
            if (!resourceFile.createNewFile()) {
                // Could not create the file
                throw new IOException("The file " + resourceFile.getName() + " could not be created in " + resourceFile.getPath());
            }

            // Stream to read from the default-file
            final InputStream in = AACAdditionPro.getInstance().getResource(file_name);
            // Stream to write into the newly created file
            final OutputStream out = new FileOutputStream(resourceFile);

            if (in != null) {
                // Write the content of the default file to the newly created file
                ByteStreams.copy(in, out);
            }
        }

        return resourceFile;
    }
}

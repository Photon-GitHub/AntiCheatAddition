package de.photon.aacadditionproold.util.files;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil
{
    /**
     * Creates a new {@link File} and all missing parent directories.
     *
     * @param file the {@link File} which should be created.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createFile(final File file) throws IOException
    {
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }
}

package de.photon.AACAdditionPro.util.files.serialization;

import de.photon.AACAdditionPro.heuristics.PatternDeserializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressedDataSerializer
{
    /**
     * Creates a new {@link InputStream} to read compressed data from.
     *
     * @param resourceName The name of a resource to be found in the jar.
     */
    public static EnhancedDataInputStream createInputStream(final String resourceName) throws IOException
    {
        return new EnhancedDataInputStream(new GZIPInputStream(PatternDeserializer.class.getClassLoader().getResourceAsStream(resourceName)));
    }

    /**
     * Creates a new {@link InputStream} to read compressed data from.
     *
     * @param inputFile the {@link File} in which the data is saved.
     */
    public static EnhancedDataInputStream createInputStream(final File inputFile) throws IOException
    {
        return new EnhancedDataInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
    }

    /**
     * Creates a new {@link OutputStream} to save data in a compressed way.
     *
     * @param outputFile the {@link File} the data will be written to.
     */
    public static EnhancedDataOutputStream createOutputStream(final File outputFile) throws IOException
    {
        return new EnhancedDataOutputStream(new GZIPOutputStream(new FileOutputStream(outputFile)));
    }
}

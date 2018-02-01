package de.photon.AACAdditionPro.util.files.serialization;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

public class EnhancedDataInputStream extends DataInputStream
{
    /**
     * Creates a DataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public EnhancedDataInputStream(InputStream in)
    {
        super(in);
    }

    /**
     * Reads a whole array of integers from the stream.
     */
    public int[] readIntegerArray() throws IOException
    {
        return readIntegerArrayWithLength(this.readInt());
    }

    /**
     * Reads a whole array of integers from the stream.
     *
     * @param length the length of the array.
     */
    public int[] readIntegerArrayWithLength(int length) throws IOException
    {
        int[] integers = new int[length];

        for (int i = 0; i < integers.length; i++)
        {
            integers[i] = this.readInt();
        }
        return integers;
    }

    /**
     * Reads a whole array of integers from the stream.
     */
    public double[] readDoubleArray() throws IOException
    {
        return readDoubleArrayWithLength(this.readInt());
    }

    /**
     * Reads a whole array of doubles from the stream.
     *
     * @param length the length of the array.
     */
    public double[] readDoubleArrayWithLength(int length) throws IOException
    {
        double[] doubles = new double[length];

        for (int i = 0; i < doubles.length; i++)
        {
            doubles[i] = this.readDouble();
        }
        return doubles;
    }


    // ----------------------------------------- Non-primitive types below! ----------------------------------------- //


    /**
     * Reads a wrapped {@link Integer} (with null / nonnull information) from the stream.
     */
    public Integer readWrappedInteger() throws IOException
    {
        return this.readBoolean() ? this.readInt() : null;
    }

    /**
     * Reads a wrapped {@link Double} (with null / nonnull information) from the stream.
     */
    public Double readWrappedDouble() throws IOException
    {
        return this.readBoolean() ? this.readDouble() : null;
    }

    /**
     * Reads a wrapped {@link Integer} - array (with null / nonnull information) from the stream.
     */
    public Integer[] readWrappedIntegerArray() throws IOException
    {
        return this.readWrappedIntegerArrayWithLength(this.readInt());
    }

    /**
     * Reads a wrapped {@link Integer} - array (with null / nonnull information) from the stream.
     *
     * @param length the length of the array.
     */
    public Integer[] readWrappedIntegerArrayWithLength(int length) throws IOException
    {
        final boolean[] nonNullInformation = readNonNullInformation(length);
        final Integer[] integers = new Integer[length];

        for (int i = 0; i < integers.length; i++)
        {
            integers[i] = nonNullInformation[i] ? this.readInt() : null;
        }
        return integers;
    }

    /**
     * Reads a wrapped {@link Double} - array (with null / nonnull information) from the stream.
     */
    public Double[] readWrappedDoubleArray() throws IOException
    {
        return this.readWrappedDoubleArrayWithLength(this.readInt());
    }

    /**
     * Reads a wrapped {@link Double} - array (with null / nonnull information) from the stream.
     *
     * @param length the length of the array.
     */
    public Double[] readWrappedDoubleArrayWithLength(int length) throws IOException
    {
        final boolean[] nonNullInformation = readNonNullInformation(length);
        final Double[] doubles = new Double[length];

        for (int i = 0; i < doubles.length; i++)
        {
            doubles[i] = nonNullInformation[i] ? this.readDouble() : null;
        }
        return doubles;
    }

    /**
     * Reads the compressed non-null information.
     */
    private boolean[] readNonNullInformation(final int arrayLength) throws IOException
    {
        final boolean[] nonNullInformation = new boolean[arrayLength];

        // Calculate how many bytes are needed to store all the non-null infos.
        int nullInformationCount = arrayLength;
        nullInformationCount /= 8;
        nullInformationCount++;

        // Encode the non-null info into bytes
        byte[] nonNullBytes = new byte[nullInformationCount];

        for (int i = 0; i < nonNullBytes.length; i++)
        {
            nonNullBytes[i] = this.readByte();
        }

        final BitSet readBitSet = BitSet.valueOf(nonNullBytes);
        for (int i = 0; i < arrayLength; i++)
        {
            nonNullInformation[i] = readBitSet.get(i);
        }
        return nonNullInformation;
    }
}

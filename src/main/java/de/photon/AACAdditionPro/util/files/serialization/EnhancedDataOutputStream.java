package de.photon.AACAdditionPro.util.files.serialization;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

public class EnhancedDataOutputStream extends DataOutputStream
{
    /**
     * Creates a new data output stream to write data to the specified
     * underlying output stream. The counter <code>written</code> is
     * set to zero.
     *
     * @param out the underlying output stream, to be saved for later
     *            use.
     *
     * @see FilterOutputStream#out
     */
    public EnhancedDataOutputStream(OutputStream out)
    {
        super(out);
    }

    /**
     * Writes a whole array of integers to the stream.
     *
     * @param integerArray the array which should be written to the stream.
     * @param writeLength  whether or not the length of the array should be written to the stream.
     */
    public void writeIntegerArray(final int[] integerArray, final boolean writeLength) throws IOException
    {
        if (writeLength)
        {
            this.writeInt(integerArray.length);
        }

        for (int i : integerArray)
        {
            this.writeInt(i);
        }
    }

    /**
     * Writes a whole array of integers to the stream.
     *
     * @param doubleArray the array which should be written to the stream.
     * @param writeLength whether or not the length of the array should be written to the stream.
     */
    public void writeDoubleArray(final double[] doubleArray, final boolean writeLength) throws IOException
    {
        if (writeLength)
        {
            this.writeInt(doubleArray.length);
        }

        for (double i : doubleArray)
        {
            this.writeDouble(i);
        }
    }


    // ----------------------------------------- Non-primitive types below! ----------------------------------------- //


    /**
     * Writes a wrapped {@link Integer} (with null / nonnull information) to the stream.
     *
     * @param writeInteger the {@link Integer} which should be written to the stream.
     */
    public void writeWrappedInteger(final Integer writeInteger) throws IOException
    {
        if (writeInteger == null)
        {
            this.writeBoolean(false);
        }
        else
        {
            this.writeBoolean(true);
            this.writeInt(writeInteger);
        }
    }

    /**
     * Writes a wrapped {@link Double} (with null / nonnull information) to the stream.
     *
     * @param writeDouble the {@link Double} which should be written to the stream.
     */
    public void writeWrappedDouble(final Double writeDouble) throws IOException
    {
        if (writeDouble == null)
        {
            this.writeBoolean(false);
        }
        else
        {
            this.writeBoolean(true);
            this.writeDouble(writeDouble);
        }
    }

    /**
     * Writes a wrapped {@link Integer} - array (with null / nonnull information) to the stream.
     *
     * @param integerArray the {@link Integer} - array which should be written to the stream.
     * @param writeLength  whether or not the length of the array should be written to the stream.
     */
    public void writeWrappedIntegerArray(final Integer[] integerArray, final boolean writeLength) throws IOException
    {
        this.sendNonNullInformation(integerArray);

        for (Integer integer : integerArray)
        {
            this.writeInt(integer);
        }
    }

    /**
     * Writes a wrapped {@link Double} - array (with null / nonnull information) to the stream.
     *
     * @param doubleArray the {@link Double} - array which should be written to the stream.
     * @param writeLength whether or not the length of the array should be written to the stream.
     */
    public void writeWrappedDoubleArray(final Double[] doubleArray, final boolean writeLength) throws IOException
    {
        this.sendNonNullInformation(doubleArray);

        for (Double integer : doubleArray)
        {
            this.writeDouble(integer);
        }
    }

    /**
     * Writes compressed non-null information.
     */
    private void sendNonNullInformation(final Object[] array) throws IOException
    {
        final BitSet bitSet = new BitSet(array.length);

        for (int i = 0; i < array.length; i++)
        {
            // true means "nonnull"
            bitSet.set(i, array[i] != null);
        }

        for (byte b : bitSet.toByteArray())
        {
            this.writeByte(b);
        }
    }
}

package de.photon.aacadditionpro.util.sorting;

import java.util.Comparator;

public final class CompareUtils
{
    /**
     * Compares two arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one        the first of the arrays which should be compared
     * @param two        the second of the arrays which should be compared
     * @param comparator the {@link Comparator} comparing the elements of each array.
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static <T> int compareArray(T[] one, T[] two, Comparator<T> comparator)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = comparator.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }

    /**
     * Compares two byte arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one the first of the arrays which should be compared
     * @param two the second of the arrays which should be compared
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static int compareByteArray(byte[] one, byte[] two)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = Byte.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }

    /**
     * Compares two short arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one the first of the arrays which should be compared
     * @param two the second of the arrays which should be compared
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static int compareShortArray(short[] one, short[] two)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = Short.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }

    /**
     * Compares two int arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one the first of the arrays which should be compared
     * @param two the second of the arrays which should be compared
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static int compareIntegerArray(int[] one, int[] two)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = Integer.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }

    /**
     * Compares two long arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one the first of the arrays which should be compared
     * @param two the second of the arrays which should be compared
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static int compareLongArray(long[] one, long[] two)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = Long.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }

    /**
     * Compares two float arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one the first of the arrays which should be compared
     * @param two the second of the arrays which should be compared
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static int compareFloatArray(float[] one, float[] two)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = Float.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }

    /**
     * Compares two double arrays by comparing the elements of each index.
     * The closer an element is located to index 0 the more important it is.
     *
     * @param one the first of the arrays which should be compared
     * @param two the second of the arrays which should be compared
     *
     * @return a value greater than 0 if the first array should be ordered higher than the second (i.e. by having higher
     * element values or a higher length), 0 if the two are the same and a value smaller than 0 otherwise.
     */
    public static int compareDoubleArray(double[] one, double[] two)
    {
        final int smallestLength = Math.min(one.length, two.length);

        int partCompare;
        for (int i = 0; i < smallestLength; i++)
        {
            partCompare = Double.compare(one[i], two[i]);

            if (partCompare != 0)
            {
                return partCompare;
            }
        }

        // Same numbers, now check for additions
        return Integer.compare(one.length, two.length);
    }
}

package de.photon.AACAdditionPro.heuristics.storage;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * This is used for working with the matrix for various calculations
 */
public class StorageList extends ArrayList<Double>
{
    public StorageList(final int size)
    {
        // Initial capacity
        super(size);

        // Fill up the list
        for (int index = 0; index < size; index++) {
            this.add(null);
        }
    }

    /**
     * This method tests for null - values in the given area to ensure that calculations can be done.
     *
     * @param fromIndex the starting index of the area
     * @param toIndex   the terminal index of the area
     * @return false if null was found, otherwise true
     */
    public boolean isAreaValid(final int fromIndex, final int toIndex)
    {
        for (int i = fromIndex; i <= toIndex; i++) {
            if (!isValid(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method tests if the value of the given index is null
     *
     * @param index the index of the value that should be tested
     * @return false if null was found, otherwise true
     */
    public boolean isValid(final int index)
    {
        return this.get(index) != null;
    }

    /**
     * This method is used to manipulate all values of the given area
     *
     * @param fromIndex    the starting index of the area that should be manipulated
     * @param toIndex      the terminal index of the area that should be manipulated
     * @param manipulation the {@link Function} in which the manipulation is stored
     * @return the value after the manipulation
     */
    public StorageList transformInArea(final int fromIndex, final int toIndex, final Function<Double, Double> manipulation)
    {
        for(int i = fromIndex; i <= toIndex; i++)
        {
            transformIndex(i, manipulation);
        }
        return this;
    }

    /**
     * This method is used to manipulate the value of an index
     *
     * @param index        the index of the value that should be manipulated
     * @param manipulation the {@link Function} in which the manipulation is stored
     * @return the value after the manipulation
     */
    public Double transformIndex(final int index, final Function<Double, Double> manipulation)
    {
        this.set(index, manipulation.apply(this.get(index)));
        return this.get(index);
    }

    /**
     * Adds a double to an already existing value
     *
     * @param index the index of the value
     * @param value the double that should be added to the existing value
     * @return the new value after the addition
     */
    public Double addToIndex(final int index, final double value)
    {
        return this.transformIndex(index, (oldValue) -> oldValue + value);
    }

    /**
     * Multiplies an already existing value with a double
     *
     * @param index the index of the value
     * @param value the double that should be multiplied with the existing value
     * @return the new value after the addition
     */
    public Double multiplyToIndex(final int index, final double value)
    {
        return this.transformIndex(index, (oldValue) -> oldValue * value);
    }
}

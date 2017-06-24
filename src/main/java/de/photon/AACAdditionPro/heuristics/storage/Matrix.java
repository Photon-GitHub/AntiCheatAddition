package de.photon.AACAdditionPro.heuristics.storage;

import java.util.ArrayList;

public class Matrix extends ArrayList<ArrayList<Double>>
{
    public Matrix(final int size)
    {
        // Initial capacity
        super(size);

        // Fill up the matrix
        for (int verticalLayer = 0; verticalLayer < size; verticalLayer++) {
            this.add(new ArrayList<>());
            for (int innerLayer = 0; innerLayer < size; innerLayer++) {
                this.get(verticalLayer).add(0D);
            }
        }
    }

    /**
     * Multiplies all the values in the matrix with the scalar.
     *
     * @param scalar the scalar that all values of the matrix should be multiplied with.
     * @return the {@link Matrix} after the multiplication
     */
    public final Matrix s_multiply_matrix(final double scalar)
    {
        for (int verticalLayer = 0; verticalLayer < this.size(); verticalLayer++) {
            // We can use this.size() here as of the quadratic shape of the matrix
            for (int innerLayer = 0; innerLayer < this.size(); innerLayer++) {
                this.multiplyToEdge(scalar, verticalLayer, innerLayer);
            }
        }
        return this;
    }

    /**
     * Sets a {@link Matrix} entry
     *
     * @param weight the weight that should be set
     * @param from   the first coordinate in the matrix
     * @param to     the second coordinate in the matrix
     * @return the {@link Matrix} after the entry was set.
     */
    public Matrix setEdge(final double weight, final int from, final int to)
    {
        this.get(from).set(to, weight);
        return this;
    }

    /**
     * Adds a value to a {@link Matrix} entry
     *
     * @param value the number that should be added to the weight of the edge
     * @param from  the first coordinate in the matrix
     * @param to    the second coordinate in the matrix
     * @return the {@link Matrix} after the value was added to the entry.
     */
    public Matrix addToEdge(final double value, final int from, final int to)
    {
        this.get(from).set(to, this.get(from).get(to) + value);
        return this;
    }

    /**
     * Multiplies a value to a {@link Matrix} entry
     *
     * @param value the number that should be multiplied to the weight of the edge
     * @param from  the first coordinate in the matrix
     * @param to    the second coordinate in the matrix
     * @return the {@link Matrix} after the value was multiplied to the entry.
     */
    public Matrix multiplyToEdge(final double value, final int from, final int to)
    {
        this.get(from).set(to, this.get(from).get(to) * value);
        return this;
    }

    /**
     * Gets a {@link Matrix} entry
     *
     * @param from the index of the vertex which marks the beginning of the edge
     * @param to   the index of the vertex which marks the end of the edge
     * @return the weight of the edge between the two vertexes
     */
    public double getEdge(final int from, final int to)
    {
        return this.get(from).get(to);
    }

    /**
     * Multiplies a number with a {@link Matrix} entry
     *
     * @param input the input which should be multiplied with the weight of the edge
     * @param from  the index of the vertex which marks the beginning of the edge
     * @param to    the index of the vertex which marks the end of the edge
     * @return the weight of the edge between the two vertexes
     */
    public double multiplyWithEdge(final double input, final int from, final int to)
    {
        return input * this.getEdge(from, to);
    }

    /**Creates a new {@link StorageList} with the correct size that is used for several calculations*/
    public StorageList constructWorkingArrayList()
    {
        return new StorageList(this.size());
    }

    public void debugMatrix()
    {
        for (final ArrayList<Double> doubles : this) {
            final StringBuilder printString = new StringBuilder();
            for (final double d : doubles) {
                printString.append(d).append("   ");
            }
            System.out.println(printString);
            System.out.println("New Array: ");
            System.out.println(" ");
        }
    }
}

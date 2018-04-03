package de.photon.AACAdditionPro.neural;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Contains all inputs for a {@link Graph}.
 */
public class DataSet implements Iterable<double[]>
{
    @Getter
    private final String label;
    @Getter
    private final double[][] data;

    private DataSet(String label, double[][] data)
    {
        this.label = label;
        this.data = data;
    }

    /**
     * Determines if this {@link DataSet} has a label.
     * Useful for categorizing the data.
     */
    public boolean hasLabel()
    {
        return this.label != null;
    }

    /**
     * @return how many samples this {@link DataSet} contains.
     */
    public int sampleCount()
    {
        return this.data.length;
    }

    @Override
    public Iterator<double[]> iterator()
    {
        return new Iterator<double[]>()
        {
            private int i = 0;

            @Override
            public boolean hasNext()
            {
                return i < data.length;
            }

            @Override
            public double[] next()
            {
                return data[i++];
            }
        };
    }

    /**
     * Creates a new {@link DataSetBuilder} to create a {@link DataSet}
     */
    public static DataSetBuilder builder()
    {
        return new DataSetBuilder();
    }

    /**
     * Builder for {@link DataSet}s.
     * This Builder properly formats the inputs to easily iterate over it.
     */
    public static class DataSetBuilder
    {
        private String label = null;
        private final List<double[]> inputList = new ArrayList<>();

        /**
         * This adds a new input to the {@link DataSet}.
         * Remember to properly
         */
        public DataSetBuilder addInput(double... data)
        {
            if (!this.inputList.isEmpty() && this.inputList.get(this.inputList.size() - 1).length != data.length)
            {
                throw new IllegalArgumentException("Data does not have equal length: " + Arrays.toString(data));
            }

            this.inputList.add(Objects.requireNonNull(data, "The data added to a DataSet must not be null."));
            return this;
        }

        /**
         * Sets the label of the data to properly categorize training data.
         * Evaluation data should not have any label.
         */
        public DataSetBuilder setLabel(final String label)
        {
            this.label = label;
            return this;
        }

        /**
         * Builds a {@link DataSet} based on the information of this {@link DataSetBuilder}
         */
        public DataSet build()
        {
            // This asserts that this.inputList.get(0) does not return 0.
            if (this.inputList.isEmpty())
            {
                throw new NeuralNetworkException("Tried to build dataset without inputs.");
            }

            // Swap the data indices to make it possible to iterate over different events, rather than different inputs.
            final double[][] swappedData = new double[this.inputList.get(0).length][this.inputList.size()];

            for (int inputIndex = 0; inputIndex < this.inputList.get(0).length; inputIndex++)
            {
                for (int inputType = 0; inputType < this.inputList.size(); inputType++)
                {
                    swappedData[inputIndex][inputType] = this.inputList.get(inputType)[inputIndex];
                }
            }

            return new DataSet(this.label, swappedData);
        }
    }
}

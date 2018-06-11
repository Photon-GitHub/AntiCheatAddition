package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.neural.DataSet;
import de.photon.AACAdditionPro.neural.Output;
import lombok.Getter;
import lombok.Setter;

public abstract class Pattern
{
    static final byte PATTERN_VERSION = 2;
    public static final Output[] LEGIT_OUTPUT = new Output[]{
            new Output("cheating", 0),
            new Output("vanilla", 1)
    };

    @Getter
    @Setter
    private String name;

    @Getter
    private final int[] inputTypes;

    public Pattern(String name, int... inputTypes)
    {
        this.name = name;
        this.inputTypes = inputTypes;
    }

    protected Output[] createBinaryOutputFromConfidence(double confidence)
    {
        final Output[] results = LEGIT_OUTPUT;
        results[0] = results[0].newConfidenceOutput(confidence);
        results[1] = results[1].newConfidenceOutput(1 - results[0].getConfidence());
        return results;
    }

    public DataSet generateDataset(double[][] inputMatrix, String label)
    {
        final DataSet.DataSetBuilder dataSetBuilder = DataSet.builder();
        // Will automatically be null if not needed.
        dataSetBuilder.setLabel(label);
        for (int inputType : this.inputTypes)
        {
            dataSetBuilder.addInput(inputMatrix[inputType]);
        }
        return dataSetBuilder.build();
    }

    /**
     * The actual analysis of the {@link Pattern} happens here.
     * The {@link DataSet} inputs are sorted alphabetically, so that it correlates with the enum.
     */
    public abstract Output[] evaluateOrTrain(DataSet dataSet);

    /**
     * Additional weight multiplicand to make sure a close-to-zero {@link Pattern} has more impact than another.
     */
    public abstract double getWeight();
}

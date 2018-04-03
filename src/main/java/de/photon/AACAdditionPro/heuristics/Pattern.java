package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.neural.DataSet;
import de.photon.AACAdditionPro.neural.Output;
import lombok.Getter;
import lombok.Setter;

public abstract class Pattern
{
    static final byte PATTERN_VERSION = 2;
    public static Output[] LEGIT_OUTPUT = new Output[]{
            new Output("cheating", 0),
            new Output("vanilla", 1)
    };

    @Getter
    @Setter
    private String name;
    @Getter
    private Input[] inputs;

    public Pattern(String name, Input.InputType[] inputTypes)
    {
        this.name = name;
        this.inputs = new Input[inputTypes.length];
        for (int i = 0; i < inputTypes.length; i++)
        {
            this.inputs[i] = new Input(inputTypes[i], new double[0]);
        }
    }

    protected Output[] createBinaryOutputFromConfidence(double confidence)
    {
        final Output[] results = LEGIT_OUTPUT;
        results[0] = results[0].newConfidenceOutput(confidence);
        results[1] = results[1].newConfidenceOutput(1 - results[0].getConfidence());
        return results;
    }

    /**
     * This updates a certain {@link Input}.
     * If the desired {@link Input} is not present in this {@link Pattern} it will just be ignored.
     *
     * @param input the {@link Input} with the new data.
     */
    public void setInputData(final Input input)
    {
        for (Input internalInput : this.inputs)
        {
            if (internalInput.sameType(input))
            {
                internalInput.data = input.data;
            }
        }
    }

    /**
     * Creates a new {@link DataSet} from the data of inputs.
     */
    protected DataSet createDataSetFromInputs(final String label)
    {
        final DataSet.DataSetBuilder dataSetBuilder = DataSet.builder();
        for (Input input : this.inputs)
        {
            dataSetBuilder.addInput(input.data);
        }
        dataSetBuilder.setLabel(label);
        return dataSetBuilder.build();
    }

    /**
     * The actual analysis of the {@link Pattern} happens here.
     * The {@link DataSet} inputs are sorted alphabetically, so that it correlates with the {@link de.photon.AACAdditionPro.heuristics.Input.InputType} enum.
     */
    public abstract Output[] evaluate();

    /**
     * Additional weight multiplicand to make sure a close-to-zero {@link Pattern} has more impact than another.
     */
    public abstract double getWeight();
}

package de.photon.AACAdditionPro.oldheuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Deprecated
public abstract class Pattern
{
    // The version byte used for the serialization.
    static final byte PATTERN_VERSION = 1;
    public static final String[] VALID_OUTPUTS = new String[]{
            "VANILLA",
            "CHEATING"
    };

    // SERIALIZATION: CONTENT
    @Getter
    @Setter
    private String name;

    // SERIALIZATION: CONTENT
    @Getter
    private InputData[] inputs;

    protected Pattern(String name, InputData[] inputs)
    {
        this.name = name;
        this.inputs = inputs;
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     */
    protected double[][] provideInputData(final Map<Character, InputData> providedInputs)
    {
        if (Objects.requireNonNull(providedInputs, "The input values of pattern " + this.getName() + " are null.").size() == 0)
        {
            throw new NeuralNetworkException("Illegal input size.");
        }

        int maxLength = 0;
        for (InputData inputData : providedInputs.values())
        {
            if (inputData.getData().length > maxLength)
            {
                maxLength = inputData.getData().length;
            }
        }

        // Convert the input data into a double tensor
        final double[][] rawInputArray = new double[this.inputs.length][];

        for (InputData providedInput : providedInputs.values())
        {
            // Copy important inputs
            for (int inputIndex = 0; inputIndex < this.inputs.length; inputIndex++)
            {
                if (this.inputs[inputIndex].getName().equals(providedInput.getName()))
                {
                    // Assert the inputs have the same length.
                    rawInputArray[inputIndex] = providedInput.getData();
                }
            }
        }

        // Flag invalid data sets.
        final Set<Integer> invalidIndices = new HashSet<>();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < rawInputArray.length; i++)
        {
            for (int j = 0; j < rawInputArray[i].length; j++)
            {
                if (rawInputArray[i][j] == Double.MIN_VALUE)
                {
                    // Flag the whole column for consistency
                    invalidIndices.add(j);
                }
            }
        }

        // Make the invalid data consistent over the arrays.
        final double[][] resultingInputArray = new double[this.inputs.length][maxLength];
        for (int i = 0; i < rawInputArray.length; i++)
        {
            for (int j = 0; j < rawInputArray[i].length; j++)
            {
                // Set to 0 so that these values might not affect the detection.
                resultingInputArray[i][j] = invalidIndices.contains(j) ? 0 : rawInputArray[i][j];
            }
        }

        return resultingInputArray;
    }

    /**
     * Make the pattern evaluate the current input data.
     *
     * @return the confidence for cheating or null if invalid data was provided.
     */
    public abstract double analyse(final Map<Character, InputData> inputData);

    /**
     * Additional weight multiplicand to make sure a close-to-zero {@link Pattern} has more impact than another.
     */
    public abstract double getWeight();
}

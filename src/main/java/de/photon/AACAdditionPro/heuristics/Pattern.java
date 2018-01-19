package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

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
    protected double[][] provideInputData(final Map<Character, InputData> inputValues)
    {
        if (Objects.requireNonNull(inputValues, "The input values of pattern " + this.getName() + " are null.").size() == 0)
        {
            return null;
        }

        int maxLength = 0;
        for (InputData inputData : inputValues.values())
        {
            if (inputData.getData().length > maxLength)
            {
                maxLength = inputData.getData().length;
            }
        }

        // Convert the input data into a double tensor
        final double[][] inputArray = new double[this.inputs.length][maxLength];

        for (InputData inputValue : inputValues.values())
        {
            for (int i = 0; i < this.inputs.length; i++)
            {
                if (this.inputs[i].getName().equals(inputValue.getName()))
                {
                    inputArray[i] = inputValue.getData();

                    // Validate the values.
                    for (double d : inputArray[i])
                    {
                        if (d == Double.MIN_VALUE)
                        {
                            return null;
                        }
                    }

                    // Assert the inputs have the same length.
                }
            }
        }

        return inputArray;
    }

    /**
     * Make the pattern analyse the current input data.
     *
     * @return the confidence for cheating or null if invalid data was provided.
     */
    public abstract Double analyse(final Map<Character, InputData> inputData);
}

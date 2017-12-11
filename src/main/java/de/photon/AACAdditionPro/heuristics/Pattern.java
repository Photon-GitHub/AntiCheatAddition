package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import lombok.Getter;

public class Pattern
{
    @Getter
    private String name;
    private Graph graph;

    private InputData[] inputs;
    private OutputData[] outputs;

    public Pattern(String name)
    {
        this.name = name;
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     * The {@link InputData}s should have the same internal array length.
     */
    private void provideInputData(InputData[] inputValues)
    {
        int dataEntries = -1;
        for (InputData inputValue : inputValues)
        {
            for (InputData input : this.inputs)
            {
                if (input.getName().equals(inputValue.getName()))
                {
                    input.setData(inputValue.getData());
                }

                if (dataEntries == -1)
                {
                    dataEntries = input.getData().length;
                }

                if (input.getData().length != dataEntries)
                {
                    throw new NeuralNetworkException("Input " + input.getName() + " in " + this.name + " has a different length.");
                }
            }
        }
    }
}

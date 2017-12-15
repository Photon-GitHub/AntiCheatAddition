package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.userdata.User;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class Pattern
{
    @Getter
    private String name;
    private Graph graph;

    private InputData[] inputs;
    private OutputData[] outputs;

    private User currentlyCheckedUser;

    @Setter
    private TrainingData trainingData;

    public Pattern(String name, Graph graph, OutputData[] outputs)
    {
        this.name = name;
        this.graph = graph;
        this.outputs = outputs;
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     * The {@link InputData}s should have the same internal array length.
     */
    public void provideInputData(InputData[] inputValues, User user)
    {
        this.currentlyCheckedUser = user;
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

    /**
     * Make the pattern analyse the current input data.
     * Provide data via {@link Graph}.{@link #provideInputData(InputData[], User)}
     *
     * @return the result of the analyse in form of an {@link OutputData}
     */
    public OutputData analyse()
    {
        // Convert the input data to a double tensor
        double[][] inputs = new double[this.inputs[0].getData().length][this.inputs.length];

        for (int i = 0; i < this.inputs[0].getData().length; i++)
        {
            for (int j = 0; j < this.inputs.length; j++)
            {
                inputs[i][j] = this.inputs[j].getData()[i];
            }
        }

        // Actual analyse
        double[] results = graph.analyse(inputs);

        // Get the max. confidence
        int maxIndex = -1;
        double maxConfidence = -1;

        for (int i = 0; i < results.length; i++)
        {
            if (results[i] > maxConfidence)
            {
                maxIndex = i;
                maxConfidence = results[i];
            }
        }

        if (maxIndex == -1)
        {
            throw new NeuralNetworkException("Invalid confidences: " + Arrays.toString(results));
        }

        return new OutputData(this.outputs[maxIndex].getName()).setConfidence(maxConfidence);
    }
}

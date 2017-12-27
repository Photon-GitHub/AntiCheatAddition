package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Pattern implements Serializable
{
    @Getter
    private String name;
    private Graph graph;

    private InputData[] inputs;
    private OutputData[] outputs;

    private transient boolean currentlyBlockedByInvalidData;

    // The default initial capacity of 16 is not used in most cases.
    private final transient Set<TrainingData> trainingDataList = new HashSet<>(8);

    public Pattern(String name, InputData[] inputs, int samples, OutputData[] outputs, int[] hiddenNeuronsPerLayer)
    {
        this.name = name;

        // The input and output neurons need to be added prior to building the graph.
        int[] completeNeurons = new int[hiddenNeuronsPerLayer.length + 2];

        // Inputs
        completeNeurons[0] = (inputs.length * samples);

        // Hidden
        System.arraycopy(hiddenNeuronsPerLayer, 0, completeNeurons, 1, hiddenNeuronsPerLayer.length);

        // Outputs
        completeNeurons[completeNeurons.length - 1] = outputs.length;

        this.graph = new Graph(completeNeurons);
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Adds a training data to the list.
     */
    public void addTrainingData(final TrainingData trainingData)
    {
        this.trainingDataList.add(trainingData);
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     * The {@link InputData}s should have the same internal array length.
     */
    public void provideInputData(InputData[] inputValues)
    {
        int dataEntries = -1;

        if (Objects.requireNonNull(inputValues, "The input values of pattern " + this.getName() + " are null.").length == 0)
        {
            this.currentlyBlockedByInvalidData = true;
            return;
        }

        for (InputData inputValue : inputValues)
        {
            for (InputData input : this.inputs)
            {
                if (input.getName().equals(inputValue.getName()))
                {
                    input.setData(inputValue.getData());

                    for (double d : input.getData())
                    {
                        if (d == Double.MIN_VALUE)
                        {
                            this.currentlyBlockedByInvalidData = true;
                            return;
                        }
                    }

                    if (dataEntries == -1)
                    {
                        dataEntries = input.getData().length;
                    }
                    else if (input.getData().length != dataEntries)
                    {
                        throw new NeuralNetworkException("Input " + input.getName() + " in " + this.name + " has a different length.");
                    }
                }
            }

            this.currentlyBlockedByInvalidData = false;
        }
    }

    /**
     * Make the pattern analyse the current input data.
     * Provide data via {@link Graph}.{@link #provideInputData(InputData[])}
     *
     * @param checkedUUID the {@link UUID} of the player who is checked. This identifies players who are training the graph.
     *
     * @return the result of the analyse in form of an {@link OutputData}
     */
    public OutputData analyse(UUID checkedUUID)
    {
        if (this.currentlyBlockedByInvalidData)
        {
            // Debug
            // System.out.println("Blocked by invalid data.");
            return this.outputs[0].setConfidence(1);
        }

        // Convert the input data into a double tensor
        double[][] inputArray = new double[this.inputs.length][this.inputs[0].getData().length];
        for (int i = 0; i < this.inputs.length; i++)
        {
            inputArray[i] = this.inputs[i].getData();
        }

        // Actual analyse
        for (TrainingData trainingData : trainingDataList)
        {
            if (checkedUUID.equals(trainingData.getUuid()))
            {
                // Look for the index of the trained output
                for (int i = 0; i < this.outputs.length; i++)
                {
                    if (trainingData.getOutputData().getName().equals(this.outputs[i].getName()))
                    {
                        this.graph.train(inputArray, i);

                        if (--trainingData.trainingCycles < 0)
                        {
                            VerboseSender.sendVerboseMessage("Training of pattern " + this.name + " of output " + this.outputs[i].getName() + " finished.");
                            this.trainingDataList.remove(trainingData);
                        }
                        return null;
                    }
                }

                throw new NeuralNetworkException("Cannot identify output " + trainingData.getOutputData().getName() + " in pattern " + this.name);
            }
        }

        double[] results = graph.analyse(inputArray);

        // Get the max. confidence
        int maxIndex = -1;
        double maxResult = -1;
        double resultSum = 0;

        for (int i = 0; i < results.length; i++)
        {
            resultSum += results[i];
            if (results[i] > maxResult)
            {
                maxIndex = i;
                maxResult = results[i];
            }
        }

        if (maxIndex == -1)
        {
            throw new NeuralNetworkException("Invalid confidences: " + Arrays.toString(results));
        }

        // Return the max result divided by the sum of all results as the confidence of the pattern.
        return new OutputData(this.outputs[maxIndex].getName()).setConfidence(maxResult / resultSum);
    }

    /**
     * Saves this pattern as a file.
     */
    public void saveToFile()
    {
        try
        {
            // Create the file and open a FileOutputStream for it.
            final FileOutputStream fileOutputStream = new FileOutputStream(
                    FileUtilities.saveFileInFolder("heuristics/" + this.name + ".pattern")
            );

            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();

            fileOutputStream.close();
        } catch (IOException e)
        {
            VerboseSender.sendVerboseMessage("Could not save pattern " + this.name + ". See the logs for further information.", true, true);
            e.printStackTrace();
        }
    }
}

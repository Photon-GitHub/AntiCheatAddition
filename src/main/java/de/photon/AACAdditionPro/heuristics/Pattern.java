package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;
import lombok.Setter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

public class Pattern implements Serializable
{
    /**
     * The epoch count.
     */
    private static final int EPOCH = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.INVENTORY_HEURISTICS.getConfigString() + ".framework.epoch");

    @Getter
    @Setter
    private String name;
    private Graph graph;

    private InputData[] inputs;
    private OutputData[] outputs;

    @Getter
    private Set<TrainingData> trainingDataSet;

    @Getter
    private final Map<String, Stack<InputData[]>> trainingInputs;

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

        // The default initial capacity of 16 is not used in most cases.
        this.trainingDataSet = new HashSet<>(8);

        this.trainingInputs = new HashMap<>(2, 1);

        for (OutputData defaultOutputDatum : OutputData.DEFAULT_OUTPUT_DATA)
        {
            this.trainingInputs.put(defaultOutputDatum.getName(), new Stack<>());
        }
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     * The {@link InputData}s should have the same internal array length.
     */
    private double[][] provideInputData(InputData[] inputValues)
    {
        if (Objects.requireNonNull(inputValues, "The input values of pattern " + this.getName() + " are null.").length == 0)
        {
            return null;
        }

        // Convert the input data into a double tensor
        final double[][] inputArray = new double[this.inputs.length][this.inputs[0].getData().length];

        for (InputData inputValue : inputValues)
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
     * Provide data via {@link Graph}.{@link #provideInputData(InputData[])}
     *
     * @return the result of the analyse in form of an {@link OutputData}
     */
    public OutputData analyse(final InputData[] inputData)
    {
        final double[][] inputArray = this.provideInputData(inputData);

        if (inputArray == null)
        {
            // Debug
            // System.out.println("Blocked by invalid data.");
            return null;
        }

        double[] results = graph.analyse(inputArray);

        VerboseSender.sendVerboseMessage("Full network output: " + Arrays.toString(results), true, false);

        // Get the max. confidence
        int maxIndex = -1;
        double maxResult = Double.MIN_VALUE;

        for (int i = 0; i < results.length; i++)
        {
            if (results[i] > maxResult)
            {
                maxIndex = i;
                maxResult = results[i];
            }
        }

        if (maxIndex == Double.MIN_VALUE)
        {
            throw new NeuralNetworkException("Invalid confidences: " + Arrays.toString(results));
        }

        // Return the max result divided by the sum of all results as the confidence of the pattern.
        return new OutputData(this.outputs[maxIndex].getName()).setConfidence(maxResult);
    }

    /**
     * This clears the trainingInputs - stacks and learns from them.
     */
    public void train()
    {
        for (int epoch = 0; epoch < EPOCH; epoch++)
        {
            for (int dataIndex = 0; dataIndex < OutputData.DEFAULT_OUTPUT_DATA.length; dataIndex++)
            {
                for (InputData[] inputData : this.getTrainingInputs().get(OutputData.DEFAULT_OUTPUT_DATA[dataIndex].getName()))
                {
                    final double[][] inputArray = this.provideInputData(inputData);

                    if (inputArray == null)
                    {
                        continue;
                    }

                    this.graph.train(inputArray, dataIndex);
                }
            }
        }

        clearTrainingData();
    }

    /**
     * Saves this pattern as a file.
     */
    public void saveToFile()
    {
        clearTrainingData();

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

    /**
     * Clears the training data to reduce the serialized file size or prepare a new training cycle.
     */
    private void clearTrainingData()
    {
        // Clear the data.
        this.trainingDataSet.clear();

        for (OutputData defaultOutputDatum : OutputData.DEFAULT_OUTPUT_DATA)
        {
            this.trainingInputs.get(defaultOutputDatum.getName()).clear();
        }
    }
}

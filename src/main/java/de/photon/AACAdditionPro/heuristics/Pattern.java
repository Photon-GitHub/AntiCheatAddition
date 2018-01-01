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

    private transient boolean currentlyBlockedByInvalidData;

    @Getter
    private transient Set<TrainingData> trainingDataSet;


    @Getter
    private final transient Map<OutputData, Stack<InputData[]>> trainingInputs;

    public Pattern(String name, InputData[] inputs, int samples, OutputData[] outputs, int[] hiddenNeuronsPerLayer)
    {
        // Default constructor for trainingDataSet.
        this();

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
     * No-args constructor called upon deserialization.
     */
    protected Pattern()
    {
        // The default initial capacity of 16 is not used in most cases.
        this.trainingDataSet = new HashSet<>(8);
        this.trainingInputs = new HashMap<>(2, 1);

        for (OutputData defaultOutputDatum : OutputData.DEFAULT_OUTPUT_DATA)
        {
            this.trainingInputs.put(defaultOutputDatum, new Stack<>());
        }
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     * The {@link InputData}s should have the same internal array length.
     */
    public void provideInputData(InputData[] inputValues)
    {
        if (Objects.requireNonNull(inputValues, "The input values of pattern " + this.getName() + " are null.").length == 0)
        {
            this.currentlyBlockedByInvalidData = true;
            return;
        }

        int dataEntries = -1;

        for (InputData inputValue : inputValues)
        {
            for (int i = 0; i < this.inputs.length; i++)
            {
                if (this.inputs[i].getName().equals(inputValue.getName()))
                {
                    this.inputs[i] = inputValue;
                    for (double d : this.inputs[i].getData())
                    {
                        if (d == Double.MIN_VALUE)
                        {
                            this.currentlyBlockedByInvalidData = true;
                            return;
                        }
                    }

                    if (dataEntries == -1)
                    {
                        dataEntries = this.inputs[i].getData().length;
                    }
                    else if (this.inputs[i].getData().length != dataEntries)
                    {
                        throw new NeuralNetworkException("Input " + this.inputs[i].getName() + " in " + this.name + " has a different length.");
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
     * @return the result of the analyse in form of an {@link OutputData}
     */
    public OutputData analyse()
    {
        if (this.currentlyBlockedByInvalidData)
        {
            // Debug
            // System.out.println("Blocked by invalid data.");
            return this.outputs[0].setConfidence(1);
        }

        double[] results = graph.analyse(prepareInputData());

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
                for (InputData[] inputData : this.getTrainingInputs().get(OutputData.DEFAULT_OUTPUT_DATA[dataIndex]))
                {
                    this.provideInputData(inputData);

                    if (this.currentlyBlockedByInvalidData)
                    {
                        continue;
                    }

                    this.graph.train(prepareInputData(), dataIndex);
                }
            }
        }

        // Clear the data.
        this.trainingDataSet.clear();

        for (OutputData defaultOutputDatum : OutputData.DEFAULT_OUTPUT_DATA)
        {
            this.trainingInputs.get(defaultOutputDatum).clear();
        }
    }

    /**
     * Transforms {@link Pattern}.{@link #inputs} to a double matrix.
     *
     * @return the double matrix
     */
    private double[][] prepareInputData()
    {
        // Convert the input data into a double tensor
        final double[][] inputArray = new double[this.inputs.length][this.inputs[0].getData().length];
        for (int i = 0; i < this.inputs.length; i++)
        {
            inputArray[i] = this.inputs[i].getData();
        }
        return inputArray;
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

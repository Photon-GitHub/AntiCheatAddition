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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

public class Pattern implements Serializable
{
    public static final String[] VALID_OUTPUTS = new String[]{
            "VANILLA",
            "CHEATING"
    };

    /**
     * The epoch count.
     */
    private static final int EPOCH = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.INVENTORY_HEURISTICS.getConfigString() + ".framework.epoch");

    @Getter
    @Setter
    private String name;
    private Graph graph;

    private InputData[] inputs;

    @Getter
    private Set<TrainingData> trainingDataSet;

    @Getter
    private final Map<String, Stack<InputData[]>> trainingInputs;

    public Pattern(String name, InputData[] inputs, int samples, int[] hiddenNeuronsPerLayer)
    {
        this.name = name;

        // The input and output neurons need to be added prior to building the graph.
        int[] completeNeurons = new int[hiddenNeuronsPerLayer.length + 2];

        // Inputs
        completeNeurons[0] = (inputs.length * samples);

        // Hidden
        System.arraycopy(hiddenNeuronsPerLayer, 0, completeNeurons, 1, hiddenNeuronsPerLayer.length);

        // One output neuron.
        completeNeurons[completeNeurons.length - 1] = 1;

        this.graph = new Graph(completeNeurons);
        this.inputs = inputs;

        // The default initial capacity of 16 is not used in most cases.
        this.trainingDataSet = new HashSet<>(8);

        // Training input map.
        this.trainingInputs = new HashMap<>(2, 1);
        for (String validOutput : VALID_OUTPUTS)
        {
            this.trainingInputs.put(validOutput, new Stack<>());
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
        final double[][] inputArray = new double[this.inputs.length][inputValues[0].getData().length];

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
     *
     * @return the confidence for cheating or null if invalid data was provided.
     */
    public Double analyse(final InputData[] inputData)
    {
        final double[][] inputArray = this.provideInputData(inputData);

        if (inputArray == null)
        {
            // Debug
            // System.out.println("Blocked by invalid data.");
            return null;
        }

        return graph.analyse(inputArray);
    }

    /**
     * This clears the trainingInputs - stacks and learns from them.
     */
    public void train()
    {
        final Stack<InputData[]> maxSize = this.trainingInputs.values().stream().min(Comparator.comparingInt(Vector::size)).orElseThrow(() -> new NeuralNetworkException("The training inputs of pattern " + this.name + " do not have a max size."));

        for (int epoch = 0; epoch < EPOCH; epoch++)
        {
            for (int validOutputIndex = 0; validOutputIndex < VALID_OUTPUTS.length; validOutputIndex++)
            {
                // There are only 2 outputs and adding more outputs would require lots of changes, thus the index check here is ok.
                final boolean cheating = (validOutputIndex == 1);
                Stack<InputData[]> possibleTrainingInputs = this.getTrainingInputs().get(VALID_OUTPUTS[validOutputIndex]);

                for (int i = 0; i < maxSize.size(); i++)
                {
                    InputData[] inputData = possibleTrainingInputs.get(i);
                    final double[][] inputArray = this.provideInputData(inputData);

                    if (inputArray == null)
                    {
                        continue;
                    }

                    this.graph.train(inputArray, cheating);
                }
            }
        }

        clearTrainingData();
        saveToFile();
    }

    /**
     * Saves this pattern as a file.
     */
    private void saveToFile()
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
        this.trainingInputs.values().forEach(Vector::clear);
    }
}

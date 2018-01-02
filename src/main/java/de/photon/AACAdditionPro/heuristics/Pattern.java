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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    // SERIALIZATION: CONTENT
    @Getter
    @Setter
    private String name;
    // SERIALIZATION: CONTENT
    private Graph graph;

    // SERIALIZATION: CONTENT
    private InputData[] inputs;

    // SERIALIZATION: NON-null, CONTENT NOT IMPORTANT
    @Getter
    private Set<TrainingData> trainingDataSet;

    // SERIALIZATION: NON-null, CONTENT NOT IMPORTANT
    @Getter
    private final ConcurrentMap<String, Stack<InputData[]>> trainingInputs;

    // SERIALIZATION: CONTENT NOT IMPORTANT, MUST BE NULL
    private transient Thread trainingThread = null;

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
        this.trainingInputs = new ConcurrentHashMap<>(2, 1);
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
     *
     * @return the {@link Thread} which does the training to wait for it.
     *
     * @throws IllegalStateException if a training is already taking place.
     */
    public Thread train() throws IllegalStateException
    {
        if (this.trainingThread != null)
        {
            throw new IllegalStateException("Pattern " + this.name + " is already training.");
        }

        this.trainingThread = new Thread(() -> {
            final Stack<InputData[]> maxSize = this.trainingInputs.values().stream().min(Comparator.comparingInt(Vector::size)).orElseThrow(() -> new NeuralNetworkException("The training inputs of pattern " + this.name + " do not have a max size."));

            for (int epoch = 0; epoch < EPOCH; epoch++)
            {
                for (int validOutputIndex = 0; validOutputIndex < VALID_OUTPUTS.length; validOutputIndex++)
                {
                    Stack<InputData[]> possibleTrainingInputs = this.getTrainingInputs().get(VALID_OUTPUTS[validOutputIndex]);

                    for (int i = 0; i < maxSize.size(); i++)
                    {
                        InputData[] inputData = possibleTrainingInputs.get(i);
                        final double[][] inputArray = this.provideInputData(inputData);

                        if (inputArray == null)
                        {
                            continue;
                        }

                        // There are only 2 outputs and adding more outputs would require lots of changes, thus the index check for cheating here is ok.
                        this.graph.train(inputArray, (validOutputIndex == 1));
                    }
                }
            }

            System.out.println("Finished training.");
            clearTrainingData();
            saveToFile();
            this.trainingThread = null;
        });
        this.trainingThread.start();

        return this.trainingThread;
    }

    /***/
    public void pushInputData(final String outputNeuronName, final InputData[] inputData)
    {

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

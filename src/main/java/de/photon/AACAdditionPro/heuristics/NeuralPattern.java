package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NeuralPattern extends Pattern
{
    /**
     * The epoch count.
     */
    private static final int EPOCH = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.INVENTORY_HEURISTICS.getConfigString() + ".framework.epoch");

    // SERIALIZATION: CONTENT
    @Getter
    private Graph graph;

    // SERIALIZATION: NON-null, CONTENT NOT IMPORTANT
    @Getter
    private Set<TrainingData> trainingDataSet;

    // SERIALIZATION: NON-null, CONTENT NOT IMPORTANT
    private final ConcurrentMap<String, Stack<Map<Character, InputData>>> trainingInputs;

    // SERIALIZATION: CONTENT NOT IMPORTANT, MUST BE NULL
    private transient Thread trainingThread = null;

    // ONLY USE FOR DESERIALIZER !!!!!!!
    public NeuralPattern(String name, InputData[] inputs, Graph graph)
    {
        super(name, inputs);
        this.graph = graph;

        // The default initial capacity of 16 is not used in most cases.
        this.trainingDataSet = new HashSet<>(8);

        // Training input map.
        this.trainingInputs = new ConcurrentHashMap<>(2, 1);
        for (String validOutput : VALID_OUTPUTS)
        {
            this.trainingInputs.put(validOutput, new Stack<>());
        }
    }

    public NeuralPattern(String name, InputData[] inputs, int samples, int[] hiddenNeuronsPerLayer)
    {
        super(name, inputs);

        // The input and output neurons need to be added prior to building the graph.
        int[] completeNeurons = new int[hiddenNeuronsPerLayer.length + 2];

        // Inputs
        completeNeurons[0] = (inputs.length * samples);

        // Hidden
        System.arraycopy(hiddenNeuronsPerLayer, 0, completeNeurons, 1, hiddenNeuronsPerLayer.length);

        // One output neuron.
        completeNeurons[completeNeurons.length - 1] = 1;

        this.graph = new Graph(completeNeurons);

        // The default initial capacity of 16 is not used in most cases.
        this.trainingDataSet = new HashSet<>(8);

        // Training input map.
        this.trainingInputs = new ConcurrentHashMap<>(2, 1);
        for (String validOutput : VALID_OUTPUTS)
        {
            this.trainingInputs.put(validOutput, new Stack<>());
        }
    }

    @Override
    public double analyse(final Map<Character, InputData> inputData)
    {
        return this.graph.analyse(this.provideInputData(inputData));
    }

    @Override
    public double getWeight()
    {
        return 1D;
    }

    /**
     * This clears the trainingInputs - stacks and learns from them.
     *
     * @throws IllegalStateException if a training is already taking place.
     */
    public synchronized void train() throws IllegalStateException
    {
        if (this.trainingThread != null)
        {
            throw new IllegalStateException("Pattern " + this.getName() + " is already training.");
        }

        this.trainingThread = new Thread(() -> {
            for (int epoch = 0; epoch < EPOCH; epoch++)
            {
                for (int validOutputIndex = 0; validOutputIndex < VALID_OUTPUTS.length; validOutputIndex++)
                {
                    Stack<Map<Character, InputData>> possibleTrainingInputs = this.trainingInputs.get(VALID_OUTPUTS[validOutputIndex]);

                    final int minSize = this.trainingInputs.values().stream().min(Comparator.comparingInt(Vector::size)).orElseThrow(() -> new NeuralNetworkException("The training inputs of pattern " + this.getName() + " do not have a max size.")).size();
                    for (int i = 0; i < minSize; i++)
                    {
                        final double[][] inputArray = this.provideInputData(possibleTrainingInputs.get(i));

                        if (inputArray == null)
                        {
                            continue;
                        }

                        // There are only 2 outputs and adding more outputs would require lots of changes, thus the index check for cheating here is ok.
                        this.graph.train(inputArray, (validOutputIndex == 1));
                    }
                }
            }

            VerboseSender.sendVerboseMessage("Training of pattern " + this.getName() + " finished.");
            clearTrainingData();
            saveToFile();
            this.trainingThread = null;
        });
        this.trainingThread.start();
    }

    /**
     * This pushes a new {@link InputData} - Array to the trainingInputs if there is no training in progress.
     */
    public void pushInputData(final String outputNeuronName, final Map<Character, InputData> inputData)
    {
        // Only push when not training.
        if (this.trainingThread == null)
        {
            this.trainingInputs.get(outputNeuronName).push(inputData);
        }
    }

    /**
     * Saves this pattern as a file.
     */
    private void saveToFile()
    {
        clearTrainingData();

        try
        {
            PatternSerializer.save(this);
        } catch (IOException e)
        {
            VerboseSender.sendVerboseMessage("Could not save pattern " + this.getName() + ". See the logs for further information.", true, true);
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

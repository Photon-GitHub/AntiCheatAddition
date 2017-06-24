package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.heuristics.storage.Matrix;
import de.photon.AACAdditionPro.heuristics.storage.StorageList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeuralNetwork
{
    private static final double learn_parameter = 0.1;

    /**
     * The standard {@link ActivationFunction} of the network
     */
    private final ActivationFunction networkFunction;
    private final Matrix matrix;


    private final Map<String, Integer> inputNeurons = new HashMap<>();
    private final Map<String, Integer> outputNeurons = new HashMap<>();

    private final StorageList results, deltas;

    private final List<Integer> layers;

    public NeuralNetwork(final ActivationFunction networkFunction, final String[] inputNeurons, final String[] outputNeurons, final Integer... layers)
    {
        // Error-Prevention
        // Amount of input and output neurons
        if (inputNeurons.length <= 0) {
            throw new NeuralNetworkException("No Input-Neurons defined.");
        }

        if (outputNeurons.length <= 0) {
            throw new NeuralNetworkException("No Output-Neurons defined.");
        }

        // Amount of layers
        if (layers.length <= 0) {
            throw new NeuralNetworkException("No layers defined");
        }

        // Verifying the ActivationFunction
        if (networkFunction == null) {
            throw new NeuralNetworkException("No ActivationFunction defined.");
        }

        // Activation-Function
        this.networkFunction = networkFunction;

        // Layer-Management
        this.layers = new ArrayList<>(Arrays.asList(layers));

        initIONeurons(this.inputNeurons, inputNeurons);
        initIONeurons(this.outputNeurons, outputNeurons);

        // Add the input and the output to the layers
        this.layers.add(0, this.inputNeurons.size());
        this.layers.add(this.outputNeurons.size());

        // -1 because we search for an index
        //int resultingIndex = -1;

        int size = 0;
        // Add the amount of nodes in every layer to the final size
        for (final int i : layers) {
            size += i;
        }

        // Initialize the Matrix and the arrays with the calculated index
        matrix = new Matrix(size);
        results = matrix.constructWorkingArrayList();
        deltas = matrix.constructWorkingArrayList();

        // Validate all values apart from the input-neurons
        results.transformInArea(inputNeurons.length, results.size() - 1, (oldValue) -> 0D);
    }

    // Heuristic-Methods
    public Map<String, Double> check()
    {
        calculate();
        final Map<String, Double> resultMap = new HashMap<>();
        // TODO: Recode this to automatically apply the activationfunction
        outputNeurons.forEach((name, index) -> resultMap.put(name, ActivationFunction.applyActivationFunction(results.get(results.size() - 1 - index), networkFunction)));
        return resultMap;
    }

    public void train(final String nameOfNeuron)
    {
        // Calculate the current state
        calculate();

        // Get the index of the targeted neuron
        int indexOfTargetedOutputNeuron = Integer.MIN_VALUE;

        for (final Map.Entry<String, Integer> entry : outputNeurons.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(nameOfNeuron)) {
                indexOfTargetedOutputNeuron = entry.getValue();
            }
        }

        // The target-neuron should exist
        if (indexOfTargetedOutputNeuron < 0) {
            throw new NeuralNetworkException("The defined neuron was not found.");
        }

        // Calculate deltas
        for (int i = deltas.size() - 1; i >= 0; i--) {
            deltas.set(i, calculateDelta(i, indexOfTargetedOutputNeuron));
        }

        for (int fromNeuron = 0; fromNeuron < matrix.size(); fromNeuron++) {
            for (int toNeuron = 0; toNeuron < matrix.size(); toNeuron++) {
                //System.out.println("Train: " + fromNeuron + " " + toNeuron + " " + matrix.size());

                // Add the delta weight: deltaWeight = learn_parameter * delta of the current unit * Output of the sending Unit
                matrix.addToEdge(learn_parameter * deltas.get(fromNeuron) * ActivationFunction.applyActivationFunction(results.get(toNeuron), networkFunction), fromNeuron, toNeuron);
            }
        }
    }

    private void calculate()
    {
        if (!results.isAreaValid(0, this.inputNeurons.size() - 1)) {
            // Invalid input values
            throw new NeuralNetworkException("Values of Input-Neurons are not set.");
        }

        for (int fromNeuron = 0; fromNeuron < results.size(); fromNeuron++) {
            // The fromNeuron is done in the order and will no longer be manipulated (apart from one += 0)
            final double activatedInput = ActivationFunction.applyActivationFunction(results.get(fromNeuron), networkFunction);

            for (int toNeuron = 0; toNeuron < results.size(); toNeuron++) {
                results.addToIndex(toNeuron, matrix.getEdge(fromNeuron, toNeuron) * activatedInput);
            }
        }
    }

    private double calculateDelta(final int targetIndex, final int outputNeuronIndex)
    {
        // Validation of the layer
        if (targetIndex >= results.size() || this.getLayer(targetIndex) >= layers.size()) {
            throw new NeuralNetworkException("Tried to train invalid layer index");
        }

        // Output-Neuron
        // The -1 for indices is included in the >=
        if (targetIndex >= layers.size() - this.outputNeurons.size()) {
            final byte wantedResult = (byte) (targetIndex == outputNeuronIndex ?
                                              1 :
                                              0
            );
            return ActivationFunction.applyDerivedActivationFunction(results.get(targetIndex), networkFunction) *
                   // Determine if the Output-Neuron is the targeted Neuron
                   (wantedResult - ActivationFunction.applyActivationFunction(results.get(targetIndex), networkFunction));
        }

        // Hidden-Neuron
        double nextLayerSum = 0;

        final int nextLayerStartIndex = startIndexOfNextLayer(targetIndex);
        final int nextLayerEndIndex = nextLayerStartIndex + layers.get(this.getLayer(nextLayerStartIndex));

        // <= because we handle indices
        for (int i = nextLayerStartIndex; i <= nextLayerEndIndex; i++) {
            // Sum of Delta-i multiplied with the weight of the edge between the current node and the following node i
            // TODO: NULLPOINTER HERE
            nextLayerSum += deltas.get(i) * matrix.getEdge(targetIndex, i);
        }

        return nextLayerSum * ActivationFunction.applyDerivedActivationFunction(results.get(targetIndex), networkFunction);
    }

    // Layer-Utils
    private int getLayer(final int index)
    {
        // One layer is always added -> -1
        int layer = 0;
        int neurons = -1;
        while (layer < layers.size() && neurons < index) {
            neurons += layers.get(layer++);
        }
        return layer - 1;
    }

    private int startIndexOfNextLayer(final int index)
    {
        int neurons = 0;
        for (int layer = 0; layer < layers.size() && neurons < index; layer++) {
            neurons += layers.get(layer);
        }
        return neurons;
    }

    // Init-Methods
    public void setInput(final String nameOfInputNeuron, final Double inputValue)
    {
        inputNeurons.forEach(
                (name, index) -> {
                    if (name.equalsIgnoreCase(nameOfInputNeuron)) {
                        results.set(index, inputValue);
                    }
                });
    }

    // One-Time init
    private static void initIONeurons(final Map<String, Integer> map, final String[] names)
    {
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], i);
        }
    }

    // Debug methods
    public void debugMatrix()
    {
        matrix.debugMatrix();
    }

    public void debugDelta()
    {
        for (final double d : deltas) {
            System.out.println(d);
        }
    }
}

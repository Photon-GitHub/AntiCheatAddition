package de.photon.AACAdditionPro.neural;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.heuristics.ActivationFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Graph
{
    // Constants
    private final int epoch;
    private final double trainParameter;
    private final double momentum;

    // Activation function
    private final ActivationFunction activationFunction;

    // Neurons
    private final int[] neuronsInLayers;
    private final Output[] outputs;

    // Matrix
    private final Double[][] matrix;
    private final double[][] weightChangeMatrix;

    // Calculation
    private final double[] neurons;
    private final double[] activatedNeurons;

    public Graph(int epoch, double trainParameter, double momentum, ActivationFunction activationFunction, int[] neuronsInLayers, Output[] outputs, int totalNeurons)
    {
        this.epoch = epoch;
        this.trainParameter = trainParameter;
        this.momentum = momentum;
        this.activationFunction = activationFunction;
        this.neuronsInLayers = neuronsInLayers;
        this.outputs = outputs;
        this.matrix = new Double[totalNeurons][totalNeurons];
        this.weightChangeMatrix = new double[totalNeurons][totalNeurons];
        this.neurons = new double[totalNeurons];
        this.activatedNeurons = new double[totalNeurons];

        // Invalidate every connection
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < matrix.length; j++)
            {
                this.matrix[i][j] = null;
            }
        }

        int currentNeuron = 0;
        int nextLayerFirstNeuron;
        int nextNextLayerFirstNeuron = this.neuronsInLayers[0];

        // Do not iterate over the last layer as output neurons do not need any additional connections.
        for (int layer = 1; layer < this.neuronsInLayers.length; layer++)
        {
            nextLayerFirstNeuron = nextNextLayerFirstNeuron;
            nextNextLayerFirstNeuron += this.neuronsInLayers[layer];

            while (currentNeuron < nextLayerFirstNeuron)
            {
                for (int to = nextLayerFirstNeuron; to < nextNextLayerFirstNeuron; to++)
                {
                    // Validate allowed connections.
                    this.matrix[currentNeuron][to] = 0D;
                }

                // Increment the neuron here as the matrix otherwise sets the connections of the next neuron.
                currentNeuron++;
            }
        }
    }

    private void setInputs(final double[] inputs)
    {
        // Check lenght of data.
        if (inputs.length != this.neuronsInLayers[0])
        {
            throw new IllegalArgumentException("Wrong input length for Graph. Expected: " + this.neuronsInLayers[0] + " | Real: " + inputs.length);
        }

        // Clear old data
        this.resetCalculationArrays();

        // Copy all inputs into neurons.
        System.arraycopy(inputs, 0, this.neurons, 0, inputs.length);
    }

    /**
     * Evaluates a certain {@link DataSet}.
     * If the {@link DataSet} has multiple samples the {@link Graph} will calculate all of them and take the average.
     */
    public Output[] evaluate(final DataSet dataSet)
    {
        if (dataSet.hasLabel())
        {
            throw new NeuralNetworkException("Tried to evaluate DataSet with label.");
        }

        final double[] confidences = new double[outputs.length];

        for (double[] sample : dataSet)
        {
            this.setInputs(sample);
            double[] sampleConfidences = calculate();

            // Add the sampleConfidences.
            for (int i = 0; i < confidences.length; i++)
            {
                confidences[i] += sampleConfidences[i];
            }
        }

        final Output[] currentOutputs = new Output[this.outputs.length];

        // Divide the confidences by sample count
        for (int i = 0; i < confidences.length; i++)
        {
            confidences[i] /= dataSet.sampleCount();
            currentOutputs[i] = this.outputs[i].newConfidenceOutput(confidences[i]);
        }

        return currentOutputs;
    }

    /**
     * Trains the neural network.
     */
    public void train(final DataSet dataSet)
    {
        if (!dataSet.hasLabel())
        {
            throw new NeuralNetworkException("Tried to train DataSet without label.");
        }

        for (int i = 0; i < epoch; i++)
        {
            for (double[] sample : dataSet)
            {
                this.setInputs(sample);
                calculate();
                // The delta values are only important in this training cycle.
                final double[] deltas = new double[neurons.length];

                for (int currentNeuron = matrix.length - 1; currentNeuron >= 0; currentNeuron--)
                {
                    // Wikipedia's alternative solution: deltas[currentNeuron] = activatedNeurons[currentNeuron] * (1 - activatedNeurons[currentNeuron]);
                    deltas[currentNeuron] = activationFunction.applyDerivedActivationFunction(neurons[currentNeuron]);

                    // Deltas depend on the neuron class.
                    switch (this.classifyNeuron(currentNeuron))
                    {
                        case INPUT:
                            break;
                        case HIDDEN:
                            double sum = 0;
                            final int[] indices = nextLayerIndexBoundaries(currentNeuron);

                            // f'(netinput) * Sum(delta_(toHigherLayer) * matrix[thisNeuron][toHigherLayer])
                            for (int higherLayerNeuron = indices[0]; higherLayerNeuron <= indices[1]; higherLayerNeuron++)
                            {
                                // matrix[currentNeuron][higherLayerNeuron] is never 0 as the higher-layer neuron's matrix
                                // entries are updated prior to this neuron's matrix entries in the algorithm.
                                sum += (deltas[higherLayerNeuron] * matrix[currentNeuron][higherLayerNeuron]);
                            }

                            deltas[currentNeuron] *= sum;
                            break;
                        case OUTPUT:
                            // f'(netInput) * (a_wanted - a_real)
                            deltas[currentNeuron] *= (this.outputs[this.neurons.length - currentNeuron].getLabel().equals(dataSet.getLabel()) ?
                                                      activationFunction.max() :
                                                      activationFunction.min()) - activatedNeurons[currentNeuron];
                            break;
                    }

                    // "from" will never be bigger than "currentNeuron" as of the layer principle.
                    for (int from = 0; from < currentNeuron; from++)
                    {
                        if (matrix[from][currentNeuron] != null)
                        {
                            // Calculate the new weightChange.
                            // The old weight change is in here as a part of the momentum.
                            weightChangeMatrix[from][currentNeuron] = (1 - momentum) * trainParameter * activatedNeurons[from] * deltas[currentNeuron] +
                                                                      (momentum * weightChangeMatrix[from][currentNeuron]);

                            matrix[from][currentNeuron] += weightChangeMatrix[from][currentNeuron];
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate the current {@link de.photon.AACAdditionPro.heuristics.Graph}.
     * Prior to this method call the input neurons should be set.
     *
     * @return the value of all output neurons.
     */
    private double[] calculate()
    {
        // Perform all the adding
        for (int currentNeuron = 0; currentNeuron < this.matrix.length; currentNeuron++)
        {
            // Activation function
            this.activatedNeurons[currentNeuron] = activationFunction.applyActivationFunction(this.neurons[currentNeuron]);

            // Forward - pass of the values
            // "to" will never be smaller or equal to "currentNeuron" as of the layer principle.
            for (int to = currentNeuron; to < this.matrix.length; to++)
            {
                // Forbid a connection in null - values
                if (matrix[currentNeuron][to] != null)
                {
                    this.neurons[to] += (this.activatedNeurons[currentNeuron] * this.matrix[currentNeuron][to]);
                }
            }
        }

        // return all
        final double[] confidences = new double[this.outputs.length];
        System.arraycopy(this.activatedNeurons, this.activatedNeurons.length - this.outputs.length, confidences, 0, this.outputs.length);
        return confidences;
    }

    /**
     * Resets all values in neurons and activatedNeurons.
     */
    private void resetCalculationArrays()
    {
        for (int i = 0; i < neurons.length; i++)
        {
            neurons[i] = 0;
            activatedNeurons[i] = 0;
        }
    }

    /**
     * @return an array of integers. <br>
     * [0] -> the start index of the next layer <br>
     * [1] -> the end index of the next layer
     */
    private int[] nextLayerIndexBoundaries(int index)
    {
        // 0 is the correct start here as the first layer starts at 0.
        int startingIndex = 0;

        // The OutputNeurons should not be used / throw an exception, which is guaranteed via < length.
        for (int i = 0; i < neuronsInLayers.length; i++)
        {
            startingIndex += neuronsInLayers[i];

            if (startingIndex > index)
            {
                // - 1 as the algorithm first of all calculates the starting index of the next layer and the ending
                // index is that starting index - 1
                return new int[]{startingIndex, startingIndex + (neuronsInLayers[i + 1] - 1)};
            }
        }
        throw new NeuralNetworkException("Cannot identify the next layer of neuron " + index + " out of " + matrix.length);
    }

    /**
     * Determines whether a neuron is an input neuron, a hidden neuron or an output neuron
     *
     * @param indexOfNeuron the index of the neuron which should be classified (index of the matrix).
     */
    private NeuronType classifyNeuron(int indexOfNeuron)
    {
        if (indexOfNeuron < neuronsInLayers[0])
        {
            return NeuronType.INPUT;
        }

        // Only the last neuron is output.
        if (indexOfNeuron == neurons.length - 1)
        {
            return NeuronType.OUTPUT;
        }

        return NeuronType.HIDDEN;
    }

    /**
     * Classifier of a neuron
     */
    private enum NeuronType
    {
        INPUT,
        HIDDEN,
        OUTPUT
    }

    /**
     * Builds a {@link Graph}.
     */
    private class GraphBuilder
    {
        // Constants
        private Integer epoch = null;
        private Double trainParameter = null;
        // Momentum might not be used -> default to 0
        private double momentum = 0;

        // Activation function
        private ActivationFunction activationFunction = null;

        private Integer inputNeurons = null;
        private List<Integer> hiddenNeurons = new ArrayList<>();
        private List<Output> outputs = new ArrayList<>();

        /**
         * Sets how often a certain {@link DataSet} is trained during training.
         * High epoch {@link Graph}s will learn faster, but might be more focused on one {@link DataSet}.
         */
        public void setEpoch(int epoch)
        {
            this.epoch = epoch;
        }

        /**
         * Determines how much a single training will effect a connection.
         * High train parameter networks will learn fast, but might miss the optimal solution.
         */
        public void setTrainParameter(double trainParameter)
        {
            this.trainParameter = trainParameter;
        }

        /**
         * Determines how much the last connection change will effect this connection change.
         * Too low momentum means that the {@link Graph} may only find a local minimum, too high momentum will prevent
         * all learning.
         */
        public void setMomentum(double momentum)
        {
            this.momentum = momentum;
        }

        /**
         * The activation function of the {@link Graph}.
         */
        public void setActivationFunction(ActivationFunction activationFunction)
        {
            this.activationFunction = activationFunction;
        }

        /**
         * How many inputs will the {@link Graph} have?
         */
        public void setInputNeurons(int inputNeurons)
        {
            this.inputNeurons = inputNeurons;
        }

        /**
         * Adds a hidden layer to the {@link Graph}.
         *
         * @param neurons The neuron count of the hidden layer.
         */
        public void addHiddenLayer(int neurons)
        {
            this.hiddenNeurons.add(neurons);
        }

        /**
         * Adds a new {@link Output} to the {@link Graph}.
         */
        public void addOutput(String label)
        {
            this.outputs.add(new Output(label, 0));
        }

        /**
         * Builds the {@link Graph}.
         */
        public Graph build()
        {
            // Input and Output layer + hidden layers
            final int[] layers = new int[2 + this.hiddenNeurons.size()];
            // Inputs
            layers[0] = Objects.requireNonNull(this.inputNeurons, "Tried to create Graph without input neurons.");

            // Hidden layers
            if (!this.hiddenNeurons.isEmpty())
            {
                // Copy all layers.
                System.arraycopy(this.hiddenNeurons.toArray(new Integer[0]), 0, layers, 1, this.hiddenNeurons.size());
            }

            if (this.outputs.isEmpty())
            {
                throw new NeuralNetworkException("Tried to create Graph without output neurons.");
            }

            // Output layers.
            layers[layers.length - 1] = this.outputs.size();

            return new Graph(
                    Objects.requireNonNull(this.epoch, "Tried to create Graph without epoch."),
                    Objects.requireNonNull(this.trainParameter, "Tried to create Graph without train parameter."),
                    this.momentum,
                    Objects.requireNonNull(this.activationFunction, "Tried to create Graph without activation function."),
                    layers,
                    this.outputs.toArray(new Output[0]),
                    Arrays.stream(layers).sum());
        }
    }
}

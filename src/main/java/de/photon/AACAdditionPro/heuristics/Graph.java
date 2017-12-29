package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The graph of a neural network including the matrix.
 * This class is {@link Serializable} to allow easy saving of the data.
 */
public class Graph implements Serializable
{
    private static final double TRAIN_PARAMETER = 0.3;
    private static final double MOMENTUM_PARAMETER = 0.1;

    // The main matrix containing the weights of all connections
    // Use Wrapper class to be able to set a value to null
    private Double[][] matrix;
    // Working array does not need to be serialized
    private double[] neurons;
    private double[] activatedNeurons;
    private double[] deltas;

    private int[] neuronsInLayers;

    /**
     * Constructs a new Graph
     */
    Graph(int[] neuronsInLayers)
    {
        this.neuronsInLayers = neuronsInLayers;

        int sumOfNeurons = 0;
        for (int sizeOfLayer : this.neuronsInLayers)
        {
            sumOfNeurons += sizeOfLayer;
        }

        this.matrix = new Double[sumOfNeurons][sumOfNeurons];
        this.neurons = new double[sumOfNeurons];
        this.activatedNeurons = new double[sumOfNeurons];
        this.deltas = new double[sumOfNeurons];

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
                    // A connection should never be 0 to begin with as of the backpropagation algorithm.
                    this.matrix[currentNeuron][to] = 1D;
                }
                // Increment the neuron here as the matrix otherwise sets the connections of the next neuron.
                currentNeuron++;
            }
        }
    }

    /**
     * Set the input values of the {@link Graph} and {@link #calculate()}.
     *
     * @param inputValues the input values as a matrix with the different tests being the second index and the
     *                    different input sources (e.g. time delta, slot distance, etc.) being the first index.
     *
     * @return the output values of the output neurons.
     */
    public double[] analyse(double[][] inputValues)
    {
        for (int i = 0; i < neurons.length; i++)
        {
            neurons[i] = 0;
            activatedNeurons[i] = 0;
        }

        int inputIndex = 0;
        for (double[] inputArray : inputValues)
        {
            for (double input : inputArray)
            {
                this.neurons[inputIndex++] = input;
            }
        }

        // Not >= as the inputIndex is as well increased by one in the end.
        if (inputIndex > this.neuronsInLayers[0])
        {
            throw new NeuralNetworkException("Wrong amount of input values.");
        }
        return this.calculate();
    }

    /**
     * Train the neural network.
     *
     * @param inputValues  the input values as a matrix with the different tests being the first index and the
     *                     different input sources (e.g. time delta, slot distance, etc.) being the second index.
     * @param outputNeuron the index of the output neuron that is the correct result
     */
    public void train(double[][] inputValues, int outputNeuron)
    {
        if (outputNeuron >= neuronsInLayers[neuronsInLayers.length - 1])
        {
            throw new NeuralNetworkException("OutputNeuron index " + outputNeuron + " is not recognized.");
        }

        // outputNeuron is an index here.
        int indexOfOutputNeuron = matrix.length - neuronsInLayers[neuronsInLayers.length - 1] + outputNeuron;

        // Only calculate so that the neurons array is updated.
        analyse(inputValues);

        for (int currentNeuron = matrix.length - 1; currentNeuron >= 0; currentNeuron--)
        {
            for (int from = matrix.length - 1; from >= 0; from--)
            {
                if (matrix[from][currentNeuron] != null)
                {
                    // Save the old weightChange here.
                    final double lastWeightChange = deltas[currentNeuron];

                    // First factor is a part of the momentum calculation.
                    // weight change = train parameter * activation level of sending neuron * delta
                    double weightChange = (1 - MOMENTUM_PARAMETER) * TRAIN_PARAMETER * activatedNeurons[from];

                    // Deltas are dependent of the neuron class.
                    switch (classifyNeuron(currentNeuron))
                    {
                        case INPUT:
                            break;
                        case HIDDEN:
                            // f'(netinput) * Sum(delta_this,toHigherLayer * matrix[this][toHigherLayer])
                            deltas[currentNeuron] = applyActivationFunction(neurons[currentNeuron], true);

                            double sum = 0;

                            int[] indices = nextLayerIndexBoundaries(currentNeuron);
                            for (int i = indices[0]; i <= indices[1]; i++)
                            {
                                if (matrix[currentNeuron][i] != null)
                                {
                                    sum += deltas[i] * matrix[currentNeuron][i];
                                }
                            }

                            deltas[currentNeuron] *= sum;
                            break;
                        case OUTPUT:
                            // f'(netInput) * (a_wanted - a_real)
                            deltas[currentNeuron] = applyActivationFunction(neurons[currentNeuron], true) * ((currentNeuron == indexOfOutputNeuron ?
                                                                                                              1D :
                                                                                                              0D) - activatedNeurons[currentNeuron]);
                            break;
                    }

                    weightChange *= deltas[currentNeuron];
                    weightChange += (MOMENTUM_PARAMETER * lastWeightChange);

                    // Update deltas with weightChange as not the delta is important, but the whole weight change.
                    deltas[currentNeuron] = weightChange;

                    matrix[from][currentNeuron] += weightChange;
                }
            }
        }
    }

    /**
     * Calculate the current {@link Graph}.
     * Prior to this method call the input neurons should be set.
     *
     * @return an array of doubles representing the output neurons.
     */
    private double[] calculate()
    {
        double[] outputs = new double[neuronsInLayers[neuronsInLayers.length - 1]];

        // Perform all the adding
        for (int neuron = 0; neuron < matrix.length; neuron++)
        {
            // Activation function
            activatedNeurons[neuron] = applyActivationFunction(neurons[neuron], false);

            // Forward - pass of the values
            for (int connectionTo = 0; connectionTo < matrix.length; connectionTo++)
            {
                // Forbid a connection in null - values
                if (matrix[neuron][connectionTo] != null)
                {
                    neurons[connectionTo] += (activatedNeurons[neuron] * matrix[neuron][connectionTo]);
                }
            }
        }

        System.arraycopy(activatedNeurons, (activatedNeurons.length - outputs.length), outputs, 0, outputs.length);
        return outputs;
    }

    /**
     * @return the start index of the next layer in index 0 and the end in index 1
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

        if (indexOfNeuron < matrix.length - neuronsInLayers[neuronsInLayers.length - 1])
        {
            return NeuronType.HIDDEN;
        }

        return NeuronType.OUTPUT;
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
     * Calculated the activated value of a neuron.
     *
     * @param input   the netinput of the neuron
     * @param derived whether the derived activation function should be used.
     *
     * @return the value of the activated neuron or the derived neuron, depending on the parameter derived.
     */
    private static double applyActivationFunction(double input, boolean derived)
    {
        if (derived)
        {
            final double cosh = Math.cosh(input);
            return 1 / (cosh * cosh);
        }

        return Math.tanh(input);
    }
}

package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;

import java.io.Serializable;

/**
 * The graph of a neural network including the matrix.
 * This class is {@link Serializable} to allow easy saving of the data.
 */
public class Graph implements Serializable
{
    private static final double TRAIN_PARAMETER = 0.05;
    private static final double MOMENTUM_PARAMETER = 0.05;

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
     * Calculate every entry of the provided dataset and analyse the results.
     *
     * @param inputValues the input values as a matrix with the different tests being the first index and the
     *                    different input sources (e.g. time delta, slot distance, etc.) being the second index.
     *
     * @return the output values as an average of
     */
    public double[] analyse(double[][] inputValues)
    {
        double[] outputs = new double[neuronsInLayers[neuronsInLayers.length - 1]];

        for (double[] testSeries : inputValues)
        {
            // The test series are not supposed to be null and are not null within regular usage.
            if (testSeries != null)
            {
                double[] results = calculate(testSeries);

                for (int i = 0; i < outputs.length; i++)
                {
                    outputs[i] += results[i];
                }
            }
        }

        //TODO: DO YOU REALLY TAKE THE AVERAGE HERE?
        for (int i = 0; i < outputs.length; i++)
        {
            outputs[i] /= inputValues.length;
        }

        return outputs;
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

        for (double[] testSeries : inputValues)
        {
            // The test series are not supposed to be null and are not null within regular usage.
            if (testSeries != null)
            {
                // Only calculate so that the neurons array is updated.
                calculate(testSeries);

                for (int currentNeuron = matrix.length - 1; currentNeuron >= 0; currentNeuron--)
                {
                    for (int from = matrix.length - 1; from >= 0; from--)
                    {
                        if (matrix[from][currentNeuron] != null)
                        {
                            // weight change = train parameter * activation level of sending neuron * delta
                            // first and last parameters are parts of momentum
                            // deltas[currentNeuron] has not been set by now, thus it can be used as momentum.
                            double weightChange = (1 - MOMENTUM_PARAMETER) * TRAIN_PARAMETER * activatedNeurons[from] + (MOMENTUM_PARAMETER * deltas[currentNeuron]);

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

                            System.out.println("Classification: " + classifyNeuron(currentNeuron) +
                                               " | Neuron: " + currentNeuron +
                                               " | Delta-Value: " + deltas[currentNeuron] +
                                               " | Trained: " + (currentNeuron == indexOfOutputNeuron));

                            weightChange *= deltas[currentNeuron];

                            matrix[from][currentNeuron] += weightChange;
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate one data series in the {@link Graph}.
     *
     * @param testSeries the different input sources (e.g. time delta, slot distance, etc.) being the index.
     *
     * @return an array of doubles representing the output neurons.
     */
    private double[] calculate(double[] testSeries)
    {
        for (int i = 0; i < neurons.length; i++)
        {
            neurons[i] = 0;
            activatedNeurons[i] = 0;
        }

        double[] outputs = new double[neuronsInLayers[neuronsInLayers.length - 1]];

        // Set input values
        for (int i = 0; i < testSeries.length; i++)
        {
            if (testSeries.length != neuronsInLayers[0])
            {
                throw new NeuralNetworkException("Wrong amount of inputs for " + neuronsInLayers[0] + " input neurons.");
            }
            neurons[i] = testSeries[i];
        }

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

        // Debug
        for (int i = 0; i < neurons.length; i++)
        {
            System.out.println("Neuron " + i + " is " + neurons[i]);
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

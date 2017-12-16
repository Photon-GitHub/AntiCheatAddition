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

    // The main matrix containing the weights of all connections
    // Use Wrapper class to be able to set a value to null
    private Double[][] matrix;
    // Working array does not need to be serialized
    private transient double[] neurons;
    private transient double[] activatedNeurons;
    private transient double[] deltas;

    private int[] neuronsInLayers;

    /**
     * Constructs a new Graph
     */
    public Graph(int... neuronsInLayers)
    {
        this.neuronsInLayers = new int[neuronsInLayers.length + 1];
        System.arraycopy(neuronsInLayers, 0, this.neuronsInLayers, 0, neuronsInLayers.length);
        // There must be 2 output neurons.
        this.neuronsInLayers[this.neuronsInLayers.length - 1] = 2;

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
        int currentLayerLastNeuron;
        int nextLayerLastNeuron = this.neuronsInLayers[0];

        // Do not iterate over the last layer as output neurons do not need any additional connections.
        for (int layer = 0; layer < (this.neuronsInLayers.length - 1); layer++)
        {
            currentLayerLastNeuron = nextLayerLastNeuron;
            nextLayerLastNeuron += this.neuronsInLayers[layer + 1];

            while (currentNeuron++ < currentLayerLastNeuron)
            {
                for (int to = currentLayerLastNeuron; to < nextLayerLastNeuron; to++)
                {
                    // Validate allowed connections.
                    // A connection should never be 0 to begin with as of the backpropagation algorithm.
                    this.matrix[currentNeuron][to] = 0.1D;
                }
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

        int indexOfOutputNeuron = matrix.length - neuronsInLayers[neuronsInLayers.length - 1] + outputNeuron;

        for (double[] testSeries : inputValues)
        {
            if (testSeries != null)
            {
                double[] results = calculate(testSeries);

                for (int currentNeuron = matrix.length - 1; currentNeuron >= 0; currentNeuron--)
                {
                    for (int from = matrix.length; from > 0; from--)
                    {
                        if (matrix[from][currentNeuron] != null)
                        {
                            // weight change = train parameter * activation level of sending neuron * delta
                            double weightChange = TRAIN_PARAMETER * activatedNeurons[from];
                            switch (classifyNeuron(currentNeuron))
                            {
                                case INPUT:
                                    break;
                                case HIDDEN:
                                    // f'(netinput) * Sum(delta_this,toHigherLayer * matrix[this][toHigherLayer])
                                    deltas[currentNeuron] = tanhDerived(neurons[currentNeuron]);

                                    double sum = 0;

                                    int[] indices = nextLayerIndices(currentNeuron);
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
                                    deltas[currentNeuron] = tanhDerived(neurons[currentNeuron]) * ((currentNeuron == indexOfOutputNeuron ?
                                                                                                    1 :
                                                                                                    0) - activatedNeurons[currentNeuron]);
                                    break;
                            }

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
        double[] outputs = new double[neuronsInLayers[neuronsInLayers.length - 1]];

        // Set input values
        for (int i = 0; i < testSeries.length; i++)
        {
            if (testSeries.length != neuronsInLayers[0])
            {
                throw new NeuralNetworkException("Length of test series is not persistent.");
            }
            neurons[i] = testSeries[i];
        }

        // Perform all the adding
        for (int neuron = 0; neuron < matrix.length; neuron++)
        {
            // Activation function
            activatedNeurons[neuron] = Math.tanh(neurons[neuron]);

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
    private int[] nextLayerIndices(int index)
    {
        // We count with lengths here, so - 1 to get the index.
        int startIndex = -1;

        for (int i = 0; i < neuronsInLayers.length; i++)
        {
            startIndex += neuronsInLayers[i];

            if (startIndex > index)
            {
                // -1 because of the index here
                return new int[]{startIndex, startIndex + (neuronsInLayers[++i] - 1)};
            }
        }
        throw new NeuralNetworkException("Cannot identify the next layer of neuron " + index + " out of " + matrix.length);
    }

    private static double tanhDerived(double d)
    {
        double cosh = Math.cosh(d);
        return 1 / (cosh * cosh);
    }

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

    private enum NeuronType
    {
        INPUT,
        HIDDEN,
        OUTPUT
    }
}

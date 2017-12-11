package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;

import java.io.Serializable;

/**
 * The graph of a neural network including the matrix.
 * This class is {@link Serializable} to allow easy saving of the data.
 */
public class Graph implements Serializable
{
    // The main matrix containing the weights of all connections
    // Use Wrapper class to be able to set a value to null
    private Double[][] matrix;
    // Working array does not need to be serialized
    private transient double[] neurons;

    private int[] neuronsInLayers;

    /**
     * Constructs a new Graph
     */
    public Graph(int... neuronsInLayers)
    {
        this.neuronsInLayers = neuronsInLayers;

        int sumOfNeurons = 0;
        for (int sizeOfLayer : neuronsInLayers)
        {
            sumOfNeurons += sizeOfLayer;
        }

        this.matrix = new Double[sumOfNeurons][sumOfNeurons];
        this.neurons = new double[sumOfNeurons];

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
        int nextLayerLastNeuron = neuronsInLayers[0];

        // Do not iterate over the last layer as output neuron do not need any additional connections.
        for (int layer = 0; layer < (neuronsInLayers.length - 1); layer++)
        {
            currentLayerLastNeuron = nextLayerLastNeuron;
            nextLayerLastNeuron += neuronsInLayers[layer + 1];

            while (currentNeuron++ < currentLayerLastNeuron)
            {
                for (int to = currentLayerLastNeuron; to < nextLayerLastNeuron; to++)
                {
                    // Validate allowed connections.
                    this.matrix[currentNeuron][to] = 0D;
                }
            }
        }
    }

    /**
     * Calculate every entry of the provided dataset and analyse the results.
     *
     * @param inputValues the input values as a matrix with the different tests being the first index and the
     *                    different input sources (e.g. time delta, slot distance, etc.) being the second index.
     * @return the output values as an average of
     */
    public double[] analyse(double[][] inputValues)
    {
        double[] outputs = new double[neuronsInLayers[neuronsInLayers.length - 1]];

        for (double[] testSeries : inputValues)
        {
            // Set input values
            for (int i = 0; i < testSeries.length; i++)
            {
                if (testSeries.length != neuronsInLayers[0])
                {
                    throw new NeuralNetworkException("Length of test series too long.");
                }
                neurons[i] = testSeries[i];
            }

            // Perform all the adding
            for (int neuron = 0; neuron < matrix.length; neuron++)
            {
                // Activation function
                neurons[neuron] = Math.tanh(neurons[neuron]);

                // Forward - pass of the values
                for (int connectionTo = 0; connectionTo < matrix.length; connectionTo++)
                {
                    // Forbid a connection in null - values
                    if (matrix[neuron][connectionTo] != null)
                    {
                        neurons[connectionTo] += (neurons[neuron] * matrix[neuron][connectionTo]);
                    }
                }
            }

            for (int i = 0; i < outputs.length; i++)
            {
                outputs[i] += neurons[(neurons.length - outputs.length) + i];
            }
        }

        //TODO: DO YOU REALLY TAKE THE AVERAGE HERE?
        for (int i = 0; i < outputs.length; i++)
        {
            outputs[i] /= inputValues.length;
        }

        return outputs;
    }
}

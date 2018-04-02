package de.photon.AACAdditionPro.neural;

import java.util.Map;
import java.util.function.DoubleFunction;

public class Graph
{
    private Map<Integer, Output> outputs;
    private int epoch;
    private Double[][] matrix;
    private double[] activatedNeurons;
    private int[] neuronsInLayers;
    private DoubleFunction<Double> activationFunction;

    private Output[] evaluate(final DataSet dataSet)
    {
        return null;
    }

    private void train(final DataSet dataSet)
    {

    }

    private class GraphBuilder
    {

    }
}

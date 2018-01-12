package de.photon.AACAdditionPro.heuristics;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Deserializes a pattern to put it in memory again.
 * The name of the pattern must be known.
 */
public class PatternDeserializer
{
    private final String name;

    PatternDeserializer(String name)
    {
        this.name = name;
    }

    public Pattern load() throws IOException
    {
        InputStream inputStream = PatternDeserializer.class.getClassLoader().getResourceAsStream(this.name);
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(inputStream)))
        {
            // Documentation of the data is in PatternSerializer#save()
            byte version = input.readByte();
            if (version != Pattern.PATTERN_VERSION)
            {
                throw new IOException("Wrong version in pattern file: " + this.name);
            }

            // Name
            String patternName = input.readUTF();

            // Inputs
            int inputLength = input.readByte() & 0xFF;
            InputData[] inputs = new InputData[inputLength];
            for (int i = 0; i < inputLength; i++)
            {
                String inputName = new String(new char[]{(char) (input.readByte() & 0xFF)});
                inputs[i] = InputData.VALID_INPUTS.get(inputName);
                if (inputs[i] == null)
                {
                    throw new IOException("Pattern " + this.name + " wanted to get input " + inputName + " which is not valid");
                }
            }

            // Graph data
            ActivationFunction function = (input.readBoolean()) ?
                                          ActivationFunctions.HYPERBOLIC_TANGENT :
                                          ActivationFunctions.LOGISTIC;

            int matrixLength = input.readInt();

            // The matrix is quadratic
            Double[][] matrix = new Double[matrixLength][matrixLength];
            for (int i = 0; i < matrixLength; i++)
            {
                for (int i1 = 0; i1 < matrixLength; i1++)
                {
                    // If data exists load it.
                    matrix[i][i1] = input.readBoolean() ? input.readDouble() : null;
                }
            }

            // The matrix is quadratic
            double[][] weightMatrix = new double[matrixLength][matrixLength];
            for (int i = 0; i < matrixLength; i++)
            {
                for (int i1 = 0; i1 < matrixLength; i1++)
                {
                    weightMatrix[i][i1] = input.readDouble();
                }
            }

            int neuronLayerLength = input.readInt();
            int[] neuronLayer = new int[neuronLayerLength];
            for (int i = 0; i < neuronLayerLength; i++)
            {
                neuronLayer[i] = input.readInt();
            }

            final Graph graph = new Graph(function, matrix, weightMatrix, neuronLayer);
            return new Pattern(patternName, inputs, graph);
        }
    }
}

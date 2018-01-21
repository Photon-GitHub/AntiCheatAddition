package de.photon.AACAdditionPro.heuristics;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;

/**
 * Util class for the deserialization of saved {@link NeuralPattern}s.
 */
public final class PatternDeserializer
{
    private PatternDeserializer() {}

    /**
     * Loads a {@link NeuralPattern}.
     *
     * @param name the name of the {@link NeuralPattern}.
     */
    private static NeuralPattern load(final String name) throws IOException
    {
        InputStream inputStream = PatternDeserializer.class.getClassLoader().getResourceAsStream(name);
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(inputStream)))
        {
            // Documentation of the data is in PatternSerializer#save()
            byte version = input.readByte();
            if (version != Pattern.PATTERN_VERSION)
            {
                throw new IOException("Wrong version in pattern file: " + name);
            }

            // Name
            final String patternName = input.readUTF();

            // Inputs
            final InputData[] inputs = new InputData[input.readByte() & 0xFF];
            for (int i = 0; i < inputs.length; i++)
            {
                // The mapping key of the InputData in InputData.VALID_INPUTS
                final char inputKeyChar = input.readChar();

                inputs[i] = InputData.VALID_INPUTS.get(inputKeyChar);
                if (inputs[i] == null)
                {
                    throw new IOException("Pattern " + name + " wanted to get input " + inputKeyChar + " which is not valid");
                }
            }

            // Graph data
            ActivationFunction function = (input.readBoolean()) ?
                                          ActivationFunctions.HYPERBOLIC_TANGENT :
                                          ActivationFunctions.LOGISTIC;

            // The length of the matrix
            final int matrixLength = input.readInt();

            // The matrix is quadratic
            final Double[][] matrix = new Double[matrixLength][matrixLength];
            for (int i = 0; i < matrixLength; i++)
            {
                for (int i1 = 0; i1 < matrixLength; i1++)
                {
                    // If data exists load it.
                    matrix[i][i1] = input.readBoolean() ? input.readDouble() : null;
                }
            }

            // The matrix is quadratic
            final double[][] weightMatrix = new double[matrixLength][matrixLength];
            for (int i = 0; i < matrixLength; i++)
            {
                for (int i1 = 0; i1 < matrixLength; i1++)
                {
                    // No check for existence is required as null values will never be changed, thus a primitive datatype here.
                    weightMatrix[i][i1] = input.readDouble();
                }
            }

            final int[] neuronLayer = new int[input.readInt()];
            for (int i = 0; i < neuronLayer.length; i++)
            {
                neuronLayer[i] = input.readInt();
            }

            final Graph graph = new Graph(function, matrix, weightMatrix, neuronLayer);
            return new NeuralPattern(patternName, inputs, graph);
        }
    }

    /**
     * Loads all patterns that can be found as a resource into a {@link Collection} of Patterns.
     *
     * @param neuralPatternCollection the {@link Collection} the {@link NeuralPattern}s should be added to.
     */
    public static void loadPatterns(final Collection<Pattern> neuralPatternCollection)
    {
        try
        {
            final File jarFile = new File(PatternDeserializer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            try (JarFile pluginFile = new JarFile(jarFile))
            {
                final Enumeration<JarEntry> entries = pluginFile.entries();
                while (entries.hasMoreElements())
                {
                    final JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".ptrn"))
                    {
                        try
                        {
                            neuralPatternCollection.add(load(entry.getName()));
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        } catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
    }
}

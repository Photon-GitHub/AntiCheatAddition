package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.util.files.serialization.CompressedDataSerializer;
import de.photon.AACAdditionPro.util.files.serialization.EnhancedDataInputStream;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
        try (EnhancedDataInputStream input = CompressedDataSerializer.createInputStream(name))
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
            final InputData[] inputs = new InputData[input.readByte()];
            for (int i = 0; i < inputs.length; i++)
            {
                // The mapping key of the InputData in InputData.VALID_INPUTS
                final char inputKeyChar = input.readChar();

                inputs[i] = InputData.VALID_INPUTS.get(inputKeyChar);
                if (inputs[i] == null)
                {
                    throw new IOException("Pattern " + name + " tried to load invalid input key \'" + inputKeyChar + "\'");
                }
            }

            // Graph data
            ActivationFunction function = (input.readBoolean()) ?
                                          ActivationFunctions.HYPERBOLIC_TANGENT :
                                          ActivationFunctions.LOGISTIC;

            // The length of the matrix
            final int matrixLength = input.readInt();

            // The matrix is quadratic
            final Double[][] matrix = new Double[matrixLength][];
            for (int i = 0; i < matrixLength; i++)
            {
                matrix[i] = input.readWrappedDoubleArrayWithLength(matrixLength);
            }

            // The matrix is quadratic
            final double[][] weightMatrix = new double[matrixLength][];
            for (int i = 0; i < matrixLength; i++)
            {
                weightMatrix[i] = input.readDoubleArrayWithLength(matrixLength);
            }

            final int[] neuronLayer = input.readIntegerArray();

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

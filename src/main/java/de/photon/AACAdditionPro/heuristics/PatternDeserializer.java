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
 * Deserializes a pattern to put it in memory again.
 * The name of the pattern must be known.
 */
public class PatternDeserializer
{
    private final String name;

    private PatternDeserializer(String name)
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
                    throw new IOException("Pattern " + this.name + " wanted to get input " + inputKeyChar + " which is not valid");
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
            return new Pattern(patternName, inputs, graph);
        }
    }

    /**
     * Loads all patterns that can be found as a resource into a {@link Collection} of Patterns.
     *
     * @param patternCollection the {@link Collection} the {@link Pattern}s should be added to.
     */
    public static void loadPatterns(final Collection<Pattern> patternCollection)
    {
        try
        {
            final File jarFile = new File(PatternDeserializer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            try (JarFile pluginFile = new JarFile(jarFile))
            {
                Enumeration<JarEntry> entries = pluginFile.entries();
                while (entries.hasMoreElements())
                {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".ptrn"))
                    {
                        final PatternDeserializer deserializer = new PatternDeserializer(entry.getName());
                        try
                        {
                            patternCollection.add(deserializer.load());
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

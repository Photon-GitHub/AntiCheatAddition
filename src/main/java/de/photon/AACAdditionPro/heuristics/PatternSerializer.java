package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


/**
 * Util class for the serialization of saved {@link Pattern}s.
 */
public final class PatternSerializer
{
    private static final File HEURISTICS_FOLDER = new File(AACAdditionPro.getInstance().getDataFolder(), "heuristics");

    private PatternSerializer() {}

    /**
     * Serializes a pattern to a .ptrn file.
     */
    public static void save(final Pattern pattern) throws IOException
    {
        /*
         * A pattern file is structured like this:
         *
         * -------- PATTERN DATA
         * 1 byte: version number of the data
         * 2+n bytes: length and content of the name string
         * 1 byte: length of the input data
         * :length of input data
         *   1 byte: char of the input data name
         *
         * -------- GRAPH DATA
         * 1 byte: 0 for LOGISTIC, 1 for HYPERBOLIC_TANGENT
         * 4 bytes: length of matrix
         * :length of layers
         *   :length of layer (no byte as of quadratic matrix)
         *     1 byte: 0 for data is not set, 1 for data is set
         *     ?data is set
         *       8 bytes: data point
         * 4 bytes: length of weight layers
         * :length of weight layers
         *   4 bytes: length of data in layer
         *   :length of data in layer
         *     8 bytes: data point
         * 4 bytes: length of neuronsInLayers
         * :length of neuronsInLayers
         *   4 bytes: data point
         */

        if (!HEURISTICS_FOLDER.exists())
        {
            if (!HEURISTICS_FOLDER.mkdirs())
            {
                throw new IOException("Could not create heuristics folder.");
            }
        }

        // Create the writer.
        final DataOutputStream writer = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(new File(HEURISTICS_FOLDER, pattern.getName() + ".ptrn"))));

        // ------------------------- PATTERN
        // Version first
        writer.writeByte(Pattern.PATTERN_VERSION);

        // Name
        writer.writeUTF(pattern.getName());

        // Inputs
        writer.writeByte(pattern.getInputs().length);
        for (InputData inputData : pattern.getInputs())
        {
            // Find the character in the map.
            for (Map.Entry<Character, InputData> characterInputDataEntry : InputData.VALID_INPUTS.entrySet())
            {
                if (characterInputDataEntry.getValue().getName().equals(inputData.getName()))
                {
                    // Write correct char
                    writer.writeChar(characterInputDataEntry.getKey());
                    break;
                }
            }
        }

        // ------------------------- GRAPH
        writer.writeBoolean(pattern.getGraph().getActivationFunction() != ActivationFunctions.LOGISTIC);

        writer.writeInt(pattern.getGraph().getMatrix().length);
        for (Double[] layer : pattern.getGraph().getMatrix())
        {
            for (Double data : layer)
            {
                if (data == null)
                {
                    writer.writeBoolean(false);
                }
                else
                {
                    writer.writeBoolean(true);
                    writer.writeDouble(data);
                }
            }
        }

        for (double[] layer : pattern.getGraph().getWeightChangeMatrix())
        {
            for (double data : layer)
            {
                writer.writeDouble(data);
            }
        }

        writer.writeInt(pattern.getGraph().getNeuronsInLayers().length);
        for (int data : pattern.getGraph().getNeuronsInLayers())
        {
            writer.writeInt(data);
        }

        writer.flush();
        writer.close();
    }
}

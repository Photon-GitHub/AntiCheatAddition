package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes a pattern to a .ptrn file.
 */
public class PatternSerializer
{
    private static final File HEURISTICS_FOLDER = new File(AACAdditionPro.getInstance().getDataFolder(), "heuristics");

    private DataOutputStream writer;
    private final Pattern pattern;

    PatternSerializer(Pattern pattern)
    {
        this.pattern = pattern;

        try
        {
            if (!HEURISTICS_FOLDER.exists() && !HEURISTICS_FOLDER.mkdirs())
            {
                VerboseSender.sendVerboseMessage("Could not create heuristics folder.", true, true);
                return;
            }
            this.writer = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(new File(HEURISTICS_FOLDER, pattern.getName() + ".ptrn"))));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void save() throws IOException
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

        // ------------------------- PATTERN
        // Version first
        this.writer.writeByte(Pattern.PATTERN_VERSION);

        // Name
        this.writer.writeUTF(this.pattern.getName());

        // Inputs
        this.writer.writeByte(this.pattern.getInputs().length);
        for (InputData inputData : this.pattern.getInputs())
        {
            // Find the character in the map.
            for (Map.Entry<Character, InputData> characterInputDataEntry : InputData.VALID_INPUTS.entrySet())
            {
                if (characterInputDataEntry.getValue().getName().equals(inputData.getName()))
                {
                    // Write correct char
                    this.writer.writeChar(characterInputDataEntry.getKey());
                    break;
                }
            }
        }

        // ------------------------- GRAPH
        this.writer.writeBoolean(this.pattern.getGraph().getActivationFunction() != ActivationFunctions.LOGISTIC);

        this.writer.writeInt(this.pattern.getGraph().getMatrix().length);
        for (Double[] layer : this.pattern.getGraph().getMatrix())
        {
            for (Double data : layer)
            {
                if (data == null)
                {
                    this.writer.writeBoolean(false);
                }
                else
                {
                    this.writer.writeBoolean(true);
                    this.writer.writeDouble(data);
                }
            }
        }

        for (double[] layer : this.pattern.getGraph().getWeightChangeMatrix())
        {
            for (double data : layer)
            {
                this.writer.writeDouble(data);
            }
        }

        this.writer.writeInt(this.pattern.getGraph().getNeuronsInLayers().length);
        for (int data : this.pattern.getGraph().getNeuronsInLayers())
        {
            this.writer.writeInt(data);
        }

        this.writer.flush();
        this.writer.close();
    }
}

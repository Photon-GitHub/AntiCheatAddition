package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes a pattern to a .ptrn file.
 */
public class PatternSerializer
{
    private static final File HEURISTICS_FOLDER = new File(AACAdditionPro.getInstance().getDataFolder(), "heuristics");

    private DataOutputStream writer;
    private Pattern pattern;

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
         * 1 byte: version number of the data
         *
         * -------- PATTERN DATA
         * 2+n bytes: length and content of the name string
         * 1 byte: length of the input data
         * :length of input data
         *   1 byte: char of the input data name
         *
         * -------- GRAPH DATA
         * 1 byte: 0 for LOGISTIC, 1 for HYPERBOLIC_TANGENT
         * 4 bytes: length of layers
         * :length of layers
         *   4 bytes: length of data in layer
         *   :length of data in layer
         *     1 byte: 0 for data is not set, 1 for data is set
         *     ?data is set
         *       8 bytes: data point
         * 4 bytes: length of weight layers
         * :length of weight layers
         *   4 bytes: length of data in layer
         *   :length of data in layer
         *     8 bytes: data point
         * 4 bytes: length of neurons / activatedNeurons
         * 4 bytes: length of neuronsInLayers
         * :length of neuronsInLayers
         *   4 bytes: data point
         */

        // ------------------------- PATTERN
        // Version first
        this.writer.write(Pattern.PATTERN_VERSION);

        // Name
        this.writer.writeUTF(this.pattern.getName());

        // Inputs
        this.writer.write(this.pattern.getInputs().length);
        for (InputData inputData : this.pattern.getInputs())
        {
            this.writer.write(inputData.getName().charAt(0));
        }

        // ------------------------- GRAPH
        this.writer.writeBoolean(this.pattern.getGraph().getActivationFunction() != ActivationFunctions.LOGISTIC);

        this.writer.writeInt(this.pattern.getGraph().getMatrix().length);
        for (Double[] layer : this.pattern.getGraph().getMatrix())
        {
            //this.writer.writeInt(layer.length);
            for (Double data : layer)
            {
                if (data == null)
                {
                    this.writer.write(0);
                }
                else
                {
                    this.writer.write(1);
                    this.writer.writeDouble(data);
                }
            }
        }

        this.writer.writeInt(this.pattern.getGraph().getWeightChangeMatrix().length);
        for (double[] layer : this.pattern.getGraph().getWeightChangeMatrix())
        {
            //this.writer.writeInt(layer.length);
            for (double data : layer)
            {
                this.writer.writeDouble(data);
            }
        }

        //this.writer.writeInt(this.pattern.getGraph().getNeurons().length); // neurons and activatedNeurons have the same length

        this.writer.writeInt(this.pattern.getGraph().getNeuronsInLayers().length);
        for (int data : this.pattern.getGraph().getNeuronsInLayers())
        {
            this.writer.writeInt(data);
        }

        this.writer.flush();
        this.writer.close();
    }
}

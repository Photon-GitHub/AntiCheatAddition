package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.neural.ActivationFunction;
import de.photon.AACAdditionPro.neural.ActivationFunctions;
import de.photon.AACAdditionPro.neural.Graph;
import de.photon.AACAdditionPro.neural.Output;
import de.photon.AACAdditionPro.oldheuristics.PatternDeserializer;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.serialization.CompressedDataSerializer;
import de.photon.AACAdditionPro.util.files.serialization.EnhancedDataInputStream;
import de.photon.AACAdditionPro.util.files.serialization.EnhancedDataOutputStream;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NeuralPattern extends Pattern
{
    private static final File HEURISTICS_FOLDER = new File(AACAdditionPro.getInstance().getDataFolder(), "heuristics");

    private final Graph graph;
    private Thread trainingThread = null;

    public NeuralPattern(String name, Input.InputType[] inputTypes, Graph graph)
    {
        super(name, inputTypes);
        this.graph = graph;
    }

    @Override
    public Output[] evaluate()
    {
        return this.graph.evaluate(this.createDataSetFromInputs(null));
    }

    public synchronized void train(final String label)
    {
        if (this.trainingThread != null)
        {
            throw new IllegalStateException("Pattern " + this.getName() + " is already training.");
        }

        this.trainingThread = new Thread(() -> {
            this.graph.train(this.createDataSetFromInputs(label));

            try
            {
                saveToFile();
            } catch (IOException e)
            {
                VerboseSender.sendVerboseMessage("Failed to save pattern " + this.getName() + ".", true, true);
                e.printStackTrace();
            }

            this.trainingThread = null;
            VerboseSender.sendVerboseMessage("Training of pattern " + this.getName() + " finished.");
        });
        this.trainingThread.start();
    }

    @Override
    public double getWeight()
    {
        return 1D;
    }

    private void saveToFile() throws IOException
    {
        /*
         * A pattern file is structured like this:
         *
         * -------- PATTERN DATA
         * 1 byte: version number of the data
         * 2+n bytes: length and content of the name string
         * 4 bytes: epoch of the graph
         * 8 bytes: train parameter of the graph
         * 8 bytes: momentum of the graph
         * 1 byte: length of the input data
         * :length of input data
         *   4 bytes: ordinal of the InputTypes
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
        final EnhancedDataOutputStream writer = CompressedDataSerializer.createOutputStream(new File(HEURISTICS_FOLDER, this.getName() + ".ptrn"));

        // ------------------------- PATTERN
        // Version first
        writer.writeByte(PATTERN_VERSION);

        // Name
        writer.writeUTF(this.getName());

        // Epoch
        writer.writeInt(this.graph.getEpoch());
        // Train parameter
        writer.writeDouble(this.graph.getTrainParameter());
        // Momentum
        writer.writeDouble(this.graph.getMomentum());

        // Inputs
        writer.writeByte(this.getInputs().length);
        for (Input input : this.getInputs())
        {
            writer.writeInt(input.getInputType().ordinal());
        }

        // ------------------------- GRAPH
        writer.writeBoolean(this.graph.getActivationFunction() != ActivationFunctions.LOGISTIC);

        writer.writeInt(this.graph.getMatrix().length);
        for (Double[] layer : this.graph.getMatrix())
        {
            writer.writeWrappedDoubleArray(layer, false);
        }

        for (double[] layer : this.graph.getWeightChangeMatrix())
        {
            writer.writeDoubleArray(layer, false);
        }

        writer.writeIntegerArray(this.graph.getNeuronsInLayers(), true);

        writer.flush();
        writer.close();
    }

    public static NeuralPattern load(final String name) throws IOException
    {
        try (EnhancedDataInputStream input = CompressedDataSerializer.createInputStream(name))
        {
            // Documentation of the data is in PatternSerializer#save()
            byte version = input.readByte();
            if (version != PATTERN_VERSION)
            {
                if (version < PATTERN_VERSION)
                {
                    throw new IOException("Tríed to load old pattern layout: " + name);
                }
                else
                {
                    throw new IOException("Tríed to load wrong pattern layout: " + name);
                }
            }

            // Name
            final String patternName = input.readUTF();

            // Epoch
            final int epoch = input.readInt();
            // Train parameter
            final double trainParameter = input.readDouble();
            // Momentum
            final double momentum = input.readDouble();

            // Inputs
            final int[] inputTypeIndices = input.readIntegerArray();
            final List<Input.InputType> inputList = new ArrayList<>();
            final Input.InputType[] inputTypes = Input.InputType.values();
            for (int inputTypeIndex : inputTypeIndices)
            {
                inputList.add(inputTypes[inputTypeIndex]);
            }
            final Input.InputType[] inputs = inputList.toArray(new Input.InputType[0]);

            // Graph data
            final ActivationFunction activationFunction = (input.readBoolean()) ?
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

            final int[] neuronsInLayers = input.readIntegerArray();

            final Graph graph = new Graph(epoch, trainParameter, momentum, activationFunction, neuronsInLayers, LEGIT_OUTPUT, matrix, weightMatrix);
            return new NeuralPattern(patternName, inputs, graph);
        }
    }

    /**
     * Loads all patterns that can be found as a resource into a {@link Collection} of {@link NeuralPattern}s.
     */
    public static Set<NeuralPattern> loadPatterns()
    {
        final Set<NeuralPattern> neuralPatterns = new HashSet<>();
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
                            neuralPatterns.add(load(entry.getName()));
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
        return neuralPatterns;
    }
}

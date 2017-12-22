package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class Pattern implements Serializable
{
    @Getter
    private String name;
    private Graph graph;

    private InputData[] inputs;
    private OutputData[] outputs;

    private transient UUID currentlyCheckedUUID;

    private transient boolean currentlyBlockedByInvalidData;

    @Setter
    private transient TrainingData trainingData;

    public Pattern(String name, InputData[] inputs, OutputData[] outputs, int[] hiddenNeuronsPerLayer)
    {
        this.name = name;

        // The input and output neurons need to be added prior to building the graph.
        int[] completeNeurons = new int[hiddenNeuronsPerLayer.length + 2];
        // Inputs
        completeNeurons[0] = inputs.length;
        System.arraycopy(hiddenNeuronsPerLayer, 0, completeNeurons, 1, hiddenNeuronsPerLayer.length);
        // Outputs
        completeNeurons[completeNeurons.length - 1] = outputs.length;

        this.graph = new Graph(completeNeurons);
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Prepares the calculation of the {@link Graph} by setting the values of the {@link InputData}s.
     * The {@link InputData}s should have the same internal array length.
     */
    public void provideInputData(InputData[] inputValues, UUID checkedUUID)
    {
        this.currentlyCheckedUUID = checkedUUID;
        int dataEntries = -1;
        for (InputData inputValue : inputValues)
        {
            for (InputData input : this.inputs)
            {
                if (input.getName().equals(inputValue.getName()))
                {
                    input.setData(inputValue.getData());

                    for (double d : input.getData())
                    {
                        if (d == Double.MIN_VALUE)
                        {
                            currentlyBlockedByInvalidData = true;
                            return;
                        }
                    }

                    currentlyBlockedByInvalidData = false;

                    if (dataEntries == -1)
                    {
                        dataEntries = input.getData().length;
                    }

                    if (input.getData().length != dataEntries)
                    {
                        throw new NeuralNetworkException("Input " + input.getName() + " in " + this.name + " has a different length.");
                    }
                }
            }
        }
    }

    /**
     * Make the pattern analyse the current input data.
     * Provide data via {@link Graph}.{@link #provideInputData(InputData[], UUID)}
     *
     * @return the result of the analyse in form of an {@link OutputData}
     */
    public OutputData analyse()
    {
        if (currentlyBlockedByInvalidData)
        {
            System.out.println("Blocked by invalid data.");
            return this.outputs[0].setConfidence(1);
        }

        // Convert the input data into a double tensor
        double[][] inputs = new double[this.inputs[0].getData().length][this.inputs.length];

        for (int i = 0; i < this.inputs[0].getData().length; i++)
        {
            for (int j = 0; j < this.inputs.length; j++)
            {
                inputs[i][j] = this.inputs[j].getData()[i];
            }
        }

        // Actual analyse
        if (this.trainingData != null && this.currentlyCheckedUUID.equals(this.trainingData.getUuid()))
        {
            // Look for the index of the trained output
            for (int i = 0; i < this.outputs.length; i++)
            {
                if (this.trainingData.getOutputData().getName().equals(this.outputs[i].getName()))
                {
                    this.graph.train(inputs, i);

                    if (--this.trainingData.trainingCycles < 0)
                    {
                        VerboseSender.sendVerboseMessage("Training of pattern " + this.name + " of output " + this.outputs[i].getName() + " finished.");
                        this.trainingData = null;
                    }
                    return null;
                }
            }

            throw new NeuralNetworkException("Cannot identify output " + this.trainingData.getOutputData().getName() + " in pattern " + this.name);
        }
        else
        {
            double[] results = graph.analyse(inputs);

            // Get the max. confidence
            int maxIndex = -1;
            double maxConfidence = -1;

            for (int i = 0; i < results.length; i++)
            {
                if (results[i] > maxConfidence)
                {
                    maxIndex = i;
                    maxConfidence = results[i];
                }
            }

            if (maxIndex == -1)
            {
                throw new NeuralNetworkException("Invalid confidences: " + Arrays.toString(results));
            }

            return new OutputData(this.outputs[maxIndex].getName()).setConfidence(maxConfidence);
        }
    }

    /**
     * Saves this pattern as a file.
     */
    public void saveToFile()
    {
        try
        {
            final File saveFile = FileUtilities.saveFileInFolder(this.name + ".pattern", FileUtilities.AACADDITIONPRO_DATAFOLDER + "/heuristics");
            FileOutputStream fileOutputStream = new FileOutputStream(saveFile);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();

            fileOutputStream.close();
        } catch (IOException e)
        {
            VerboseSender.sendVerboseMessage("Could not save pattern " + this.name + ". See the logs for further information.", true, true);
            e.printStackTrace();
        }
    }
}

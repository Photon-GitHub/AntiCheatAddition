package de.photon.AACAdditionPro.heuristics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PatternDeserializer
{
    private final String name;

    public PatternDeserializer(String name)
    {
        this.name = name;
    }

    public Pattern load() throws IOException
    {
        try ( DataInputStream input = new DataInputStream( new GZIPInputStream( PatternDeserializer.class.getResourceAsStream( "/" + this.name ) ) ) )
        {
            // Documentation of the data is in PatternSerializer#save()
            byte version = input.readByte();
            if ( version != 0 )
            {
                throw new IOException( "Wrong version in pattern file: " + this.name );
            }

            // Pattern data
            String patternName = input.readUTF();
            int inputLength = input.readByte() & 0xFF;
            InputData[] inputs = new InputData[inputLength];
            for ( int i = 0; i < inputLength; i++ )
            {
                String inputName = new String( new char[]{ (char) (input.readByte() & 0xFF) } );
                inputs[i] = InputData.VALID_INPUTS.get( inputName );
                if ( inputs[i] == null )
                {
                    throw new IOException( "Pattern " + this.name + " wanted to get input " + inputName + " which is not valid" );
                }
            }

            // Graph data
            ActivationFunction function = ( input.readBoolean() ) ? ActivationFunctions.HYPERBOLIC_TANGENT : ActivationFunctions.LOGISTIC;

            int matrixLength = input.readInt();
            Double[][] matrix = new Double[matrixLength][];
            for ( int i = 0; i < matrixLength; i++ )
            {
                int layerLength = input.readInt();
                matrix[i] = new Double[layerLength];
                for ( int i1 = 0; i1 < layerLength; i1++ )
                {
                    boolean data = input.readBoolean();
                    if ( data )
                    {
                        matrix[i][i1] = input.readDouble();
                    } else
                    {
                        matrix[i][i1] = null;
                    }
                }
            }

            int weightMatrixLength = input.readInt();
            double[][] weightMatrix = new double[weightMatrixLength][];
            for ( int i = 0; i < weightMatrixLength; i++ )
            {
                int layerLength = input.readInt();
                weightMatrix[i] = new double[layerLength];
                for ( int i1 = 0; i1 < layerLength; i1++ )
                {
                    weightMatrix[i][i1] = input.readDouble();
                }
            }

            int neuronLength = input.readInt();

            int neuronLayerLength = input.readInt();
            int[] neuronLayer = new int[neuronLayerLength];
            for ( int i = 0; i < neuronLayerLength; i++ )
            {
                neuronLayer[i] = input.readInt();
            }

            Graph graph = new Graph( function, matrix, weightMatrix, neuronLength, neuronLayer );
            return new Pattern( patternName, inputs, graph );
        }
    }

}

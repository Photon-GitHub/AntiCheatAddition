package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Checks for suspicious end-of inventory line timedeltas.
 */
public class HC00000003 extends Pattern
{

    public HC00000003()
    {
        super("HC000000003", new InputData[]{
                InputData.VALID_INPUTS.get('T'),
                InputData.VALID_INPUTS.get('X'),
                InputData.VALID_INPUTS.get('Y')
        });
    }

    @Override
    public Double analyse(Map<Character, InputData> inputData)
    {
        // See the constructor for the indices
        double[][] inputArray = this.provideInputData(inputData);

        final double[] averages = new double[]{
                Arrays.stream(inputArray[0]).average().orElse(0D),
                Arrays.stream(inputArray[1]).average().orElse(0D),
                Arrays.stream(inputArray[2]).average().orElse(0D)
        };

        byte flags = 0;
        double flagTimeOffset = 0D;
        for (int direction = 1; direction < 3; direction++)
        {
            for (int i = 0; i < inputArray[direction].length; i++)
            {
                // Significant change.
                if (!MathUtils.roughlyEquals(inputArray[direction][i], averages[direction], 4))
                {
                    flags++;
                    flagTimeOffset += MathUtils.offset(inputArray[0][i], averages[0]);
                }
            }
        }

        // Take the average.
        flagTimeOffset /= flags;

        // Have some fail-save with the greater 1.
        return flags > 1 ? Math.pow(Math.E, -0.0005 * (flagTimeOffset * flagTimeOffset)) : 0;
    }
}
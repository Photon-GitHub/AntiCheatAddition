package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Checks for too constant delta times
 */
public class HC00000002 extends Pattern
{
    public HC00000002()
    {
        super("HC000000002", new InputData[]{
                InputData.VALID_INPUTS.get('T')
        });
    }

    @Override
    public Double analyse(Map<Character, InputData> inputData)
    {
        // See the constructor for the indices
        double[][] inputArray = this.provideInputData(inputData);

        // Use a offset sum to detect too consistent clicking.
        // orElse(0) is ok as the steam (and thus the array) must be empty to reach this part of code.
        final double average = Arrays.stream(inputArray[0]).average().orElse(0);
        double offsetSum = MathUtils.offsetSum(inputArray[0], average, offset -> true);

        return Math.pow(Math.E, -0.05 * (offsetSum * offsetSum));
    }
}
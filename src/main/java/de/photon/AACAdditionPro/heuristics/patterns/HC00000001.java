package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Checks for a plausible distance/time ratio to detect purposefully randomized inventory interactions
 */
public class HC00000001 extends Pattern
{
    /**
     * A threshold after which an inventory interaction is flagged as idling and thus not evaluated.
     */
    private static final double IDLE_THRESHOLD = 600;

    public HC00000001()
    {
        super("HC000000001", new InputData[]{
                InputData.VALID_INPUTS.get('T'),
                InputData.VALID_INPUTS.get('X'),
                InputData.VALID_INPUTS.get('Y')
        });
    }

    @Override
    public double analyse(Map<Character, InputData> inputData)
    {
        // See the constructor for the indices
        double[][] inputArray = this.provideInputData(inputData);

        // Use a offset sum to detect too consistent clicking.
        // orElse(0) is ok as the steam (and thus the array) must be empty to reach this part of code.
        final double average = Arrays.stream(inputArray[0]).average().orElse(0);
        final double offsetSum = MathUtils.offsetSum(inputArray[0], average, offset -> offset <= IDLE_THRESHOLD);

        // Calculate min and max distance
        double minDistance = Double.MAX_VALUE;
        double maxDistance = Double.MIN_VALUE;
        for (int i = 0; i < inputArray[1].length; i++)
        {
            final double distance = Math.hypot(inputArray[1][i], inputArray[2][i]);

            if (distance < minDistance)
            {
                minDistance = distance;
            }

            if (distance > maxDistance)
            {
                maxDistance = distance;
            }
        }

        return Math.tanh(((offsetSum / 150) * (maxDistance - minDistance)) / 4);
    }

    @Override
    public double getWeight()
    {
        return 1.3D;
    }
}

package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Checks for too consistent delta times
 */
public class HC00000002 extends Pattern
{
    /**
     * A threshold after which an inventory interaction is flagged as idling and thus not evaluated.
     */
    private static final double IDLE_TIME_THRESHOLD = 2000;

    /**
     * How many idle interactions are allowed before the dataset is flagged as not eligible for evaluation.
     */
    private static final double MAX_IDLE_TIME_COUNTER = 4;


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
        final double[] idleRemovedData = Arrays.stream(inputArray[0]).filter(offset -> offset < IDLE_TIME_THRESHOLD).toArray();

        if (idleRemovedData.length < inputArray.length - MAX_IDLE_TIME_COUNTER)
        {
            // Too few datasets.
            return 0D;
        }

        final double average = Arrays.stream(idleRemovedData).average().orElse(0);
        final double offsetSum = MathUtils.offsetSum(idleRemovedData, average, offset -> true);

        return Math.pow(Math.E, -10 * (offsetSum * offsetSum));
    }
}
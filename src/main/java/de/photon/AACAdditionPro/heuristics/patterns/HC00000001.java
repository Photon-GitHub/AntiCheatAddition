package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Map;

/**
 * Checks for a plausible distance/time ratio to detect very constant delays even with changes in the deltas
 */
public class HC00000001 extends Pattern
{
    public HC00000001()
    {
        super("HC000000001", new InputData[]{
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

        // Use a offset sum to detect too consistent clicking.
        double average = Arrays.stream(inputArray[0]).summaryStatistics().getAverage();

        double offsetSum = 0;
        for (int i = 0; i < inputArray[0].length; i++)
        {
            VerboseSender.sendVerboseMessage("HC1: " + inputArray[0][i]);
            offsetSum += MathUtils.offset(inputArray[0][i], average);
        }

        final DoubleSummaryStatistics distanceSummary = new DoubleSummaryStatistics();
        for (int i = 0; i < inputArray[1].length; i++)
        {
            distanceSummary.accept(Math.hypot(inputArray[1][i], inputArray[2][i]));
        }

        return Math.tanh((Math.pow(Math.E, -(offsetSum * offsetSum)) * (distanceSummary.getMax() - distanceSummary.getMin())) / 4);
    }
}

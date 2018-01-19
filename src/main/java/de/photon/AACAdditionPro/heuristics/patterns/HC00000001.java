package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Map;

/**
 * Checks for a plausible distance/time ratio to detect purposefully randomized inventory interactions
 */
public class HC00000001 extends Pattern
{
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
    public Double analyse(Map<Character, InputData> inputData)
    {
        // See the constructor for the indices
        double[][] inputArray = this.provideInputData(inputData);

        // Use a offset sum to detect too consistent clicking.
        double average = Arrays.stream(inputArray[0]).summaryStatistics().getAverage();

        double offsetSum = 0;
        for (int i = 0; i < inputArray[0].length; i++)
        {
            final double offset = MathUtils.offset(inputArray[0][i], average);
            if (offset <= IDLE_THRESHOLD)
            {
                offsetSum += offset;
            }
        }

        final DoubleSummaryStatistics distanceSummary = new DoubleSummaryStatistics();
        for (int i = 0; i < inputArray[1].length; i++)
        {
            distanceSummary.accept(Math.hypot(inputArray[1][i], inputArray[2][i]));
        }

        VerboseSender.sendVerboseMessage("HC1: " + offsetSum + " | " + ((distanceSummary.getMax() - distanceSummary.getMin())) / 4);
        return Math.tanh(((offsetSum / 150) * (distanceSummary.getMax() - distanceSummary.getMin())) / 4);
    }
}

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
        this.provideInputData(inputData);

        // See the constructor for the indices
        final DoubleSummaryStatistics timeSummary = Arrays.stream(this.getInputs()[0].getData()).summaryStatistics();

        // Use a offset sum to detect too consistent clicking.
        double offsetSum = 0;
        for (int i = 0; i < this.getInputs()[0].getData().length; i++)
        {
            offsetSum += MathUtils.offset(this.getInputs()[0].getData()[i], timeSummary.getAverage());
        }

        final DoubleSummaryStatistics distanceSummary = new DoubleSummaryStatistics();
        for (int i = 0; i < this.getInputs()[1].getData().length; i++)
        {
            distanceSummary.accept(Math.hypot(this.getInputs()[1].getData()[i], this.getInputs()[2].getData()[i]));
        }

        VerboseSender.sendVerboseMessage("HC00000001: " + offsetSum + " | " + (distanceSummary.getMax() - distanceSummary.getMin()));
        return Math.tanh((Math.pow(Math.E, -(offsetSum * offsetSum)) * (distanceSummary.getMax() - distanceSummary.getMin())) / 4);
    }
}

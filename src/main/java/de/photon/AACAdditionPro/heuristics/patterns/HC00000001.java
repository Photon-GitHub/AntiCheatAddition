package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.Input;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.neural.DataSet;
import de.photon.AACAdditionPro.neural.Output;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Arrays;

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
        super("HC00000001", new Input.InputType[]{
                Input.InputType.TIMEDELTAS,
                Input.InputType.XDISTANCE,
                Input.InputType.YDISTANCE
        });
    }

    @Override
    public Output[] evaluateOrTrain()
    {
        final DataSet dataSet = this.createDataSetFromInputs(null);

        // Use a offset sum to detect too consistent clicking.
        // orElse(0) is ok as the steam (and thus the array) must be empty to reach this part of code.
        final double average = Arrays.stream(dataSet.getData()[0]).average().orElse(0);
        final double offsetSum = MathUtils.offsetSum(dataSet.getData()[0], average, offset -> offset <= IDLE_THRESHOLD);

        // Calculate min and max distance
        double minDistance = Double.MAX_VALUE;
        double maxDistance = Double.MIN_VALUE;
        for (int i = 0; i < dataSet.getData()[1].length; i++)
        {
            final double distance = Math.hypot(dataSet.getData()[1][i], dataSet.getData()[2][i]);

            if (distance < minDistance)
            {
                minDistance = distance;
            }

            if (distance > maxDistance)
            {
                maxDistance = distance;
            }
        }

        return this.createBinaryOutputFromConfidence(Math.tanh(((offsetSum / 150) * (maxDistance - minDistance)) / 4));
    }

    @Override
    public double getWeight()
    {
        return 1.3D;
    }
}

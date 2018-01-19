package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Map;

/**
 * Checks for completely constant inventory clicks with different interactions.
 */
public class HC00000002 extends Pattern
{
    private static final double MINIMUM_DISTANCE_CHANGE = 2;

    public HC00000002()
    {
        super("HC000000002", new InputData[]{
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

        Double lastX = null;
        Double lastY;
        Double lastTime = null;

        Double lastDistance = null;
        Double currentDistance = null;
        double flags = 0;
        for (int i = 0; i < inputArray[0].length; i++)
        {
            if (lastX != null)
            {
                currentDistance = Math.hypot(inputArray[1][i], inputArray[2][i]);

                final double distanceDelta = MathUtils.offset(currentDistance, lastDistance);
                final double timeDelta = MathUtils.offset(inputArray[0][i], lastTime) / 100;

                // Very different inventory action
                if (distanceDelta > MINIMUM_DISTANCE_CHANGE)
                {
                    flags += (distanceDelta / 6) * Math.pow(Math.E, -4.5D * (timeDelta * timeDelta));
                }
            }

            lastTime = inputArray[0][i];
            lastX = inputArray[1][i];
            lastY = inputArray[2][i];
            lastDistance = (currentDistance == null) ? Math.hypot(lastX, lastY) : currentDistance;
        }

        return Math.tanh(flags);
    }
}

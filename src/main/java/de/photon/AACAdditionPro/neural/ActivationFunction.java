package de.photon.AACAdditionPro.neural;

public interface ActivationFunction
{
    ActivationFunction LEAKY_RECTIFIED_LINEAR_UNIT = new ActivationFunction()
    {
        private static final double MODIFIER = 0.01D;

        @Override
        public double applyActivationFunction(double input)
        {
            if (input < 0)
            {
                input *= MODIFIER;
            }
            return input;
        }

        @Override
        public double applyDerivedActivationFunction(double input)
        {
            return input < 0 ? MODIFIER : 1;
        }

        @Override
        public double min()
        {
            return Double.MIN_VALUE;
        }

        @Override
        public double max()
        {
            return Double.MAX_VALUE;
        }
    };

    /**
     * Applies the {@link de.photon.AACAdditionPro.oldneural.ActivationFunction} to the input.
     *
     * @return the result of the application.
     */
    double applyActivationFunction(double input);

    /**
     * Applies the derived {@link de.photon.AACAdditionPro.oldneural.ActivationFunction} to the input.
     *
     * @return the result of the application.
     */
    double applyDerivedActivationFunction(double input);

    /**
     * The minimum value the function can reach.
     */
    default double min()
    {
        return -1;
    }

    /**
     * The maximum value the function can reach.
     */
    default double max()
    {
        return 1;
    }
}

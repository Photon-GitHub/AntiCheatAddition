package de.photon.AACAdditionPro.oldneural;

public enum ActivationFunctions implements ActivationFunction
{
    LEAKY_RECTIFIED_LINEAR_UNIT
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
                    return 0;
                }

                @Override
                public double max()
                {
                    return 1;
                }
            }
}

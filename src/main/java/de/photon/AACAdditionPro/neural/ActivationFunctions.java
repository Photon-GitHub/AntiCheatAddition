package de.photon.AACAdditionPro.neural;

public enum ActivationFunctions implements ActivationFunction
{
    LEAKY_RECTIFIED_LINEAR_UNIT
            {
                private static final double MODIFER = 0.01D;

                @Override
                public double applyActivationFunction(double input)
                {
                    if (input < 0)
                    {
                        input *= MODIFER;
                    }
                    return input;
                }

                @Override
                public double applyDerivedActivationFunction(double input)
                {
                    return input < 0 ? MODIFER : 1;
                }

                @Override
                public double min()
                {
                    return 0;
                }

                @Override
                public double max()
                {
                    return Double.MAX_VALUE;
                }
            }
}

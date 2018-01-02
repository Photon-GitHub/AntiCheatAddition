package de.photon.AACAdditionPro.heuristics;

public enum ActivationFunctions implements ActivationFunction
{
    LOGISTIC
            {
                @Override
                public double applyActivationFunction(double input)
                {
                    return 1 / (1 + Math.pow(Math.E, (-input)));
                }

                @Override
                public double applyDerivedActivationFunction(double input)
                {
                    final double epowx = Math.pow(Math.E, input);

                    return epowx / (epowx * epowx + 2 * epowx + 1);
                }
            },
    HYPERBOLIC_TANGENT
            {
                @Override
                public double applyActivationFunction(double input)
                {
                    return Math.tanh(input);
                }

                @Override
                public double applyDerivedActivationFunction(double input)
                {
                    double cosh = Math.cosh(input);
                    return 1 / (cosh * cosh);
                }

                @Override
                public double getBias()
                {
                    return 0.01;
                }
            }
}

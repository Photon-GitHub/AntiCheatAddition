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

                @Override
                public double min()
                {
                    return 0;
                }
            },
    HYPERBOLIC_TANGENT
            {
                @Override
                public double applyActivationFunction(double input)
                {
                    return Math.tanh(input - this.getBias());
                }

                @Override
                public double applyDerivedActivationFunction(double input)
                {
                    double cosh = Math.cosh(this.getBias() - input);
                    return 1 / (cosh * cosh);
                }

                @Override
                public double getBias()
                {
                    return 0.01;
                }
            }
}

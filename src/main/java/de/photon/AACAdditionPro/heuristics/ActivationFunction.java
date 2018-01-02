package de.photon.AACAdditionPro.heuristics;

public interface ActivationFunction
{
    double applyActivationFunction(double input);

    double applyDerivedActivationFunction(double input);

    default double getBias()
    {
        return 0;
    }
}
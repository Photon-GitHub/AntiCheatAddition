package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;

public enum ActivationFunction
{
    TANGENS_HYPERBOLICUS;

    public static Double applyActivationFunction(final Double input, final ActivationFunction activationFunction)
    {
        switch (activationFunction) {
            case TANGENS_HYPERBOLICUS:
                return Math.tanh(input);
            default:
                throw new NeuralNetworkException("ActivationFunction not found.");
        }
    }

    public static Double applyDerivedActivationFunction(final Double input, final ActivationFunction activationFunction)
    {
        switch (activationFunction) {
            case TANGENS_HYPERBOLICUS:
                return (4 * Math.pow(Math.cosh(input), 2)) / Math.pow(Math.cosh(2 * input), 2);
            default:
                throw new NeuralNetworkException("ActivationFunction not found.");
        }
    }
}
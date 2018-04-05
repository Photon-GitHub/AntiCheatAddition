package de.photon.AACAdditionPro.neural;

public interface ActivationFunction
{
    /**
     * Applies the {@link ActivationFunction} to the input.
     *
     * @return the result of the application.
     */
    double applyActivationFunction(double input);

    /**
     * Applies the derived {@link ActivationFunction} to the input.
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
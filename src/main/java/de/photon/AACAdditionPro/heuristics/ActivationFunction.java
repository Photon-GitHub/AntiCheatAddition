package de.photon.AACAdditionPro.heuristics;

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
     * This handles the bias if the {@link Graph} must not have 0 neurons to start with in order to learn.
     */
    default double getBias()
    {
        return 0;
    }

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
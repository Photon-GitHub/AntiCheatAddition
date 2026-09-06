package de.photon.anticheataddition.util.mathematics;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;

import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

/**
 * Represents a simple polynomial.
 * Safe to use asynchronously.
 * The evaluation will be done via Horner's method to reduce unnecessary calculations.
 */
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public final class Polynomial implements DoubleFunction<Double>, DoubleUnaryOperator
{
    private final double[] coefficients;

    /**
     * Constructs a new {@link Polynomial}.
     *
     * @param coefficients the coefficients of the polynomial, with descending degree
     *                     <p></p>
     *                     Example: 5,0,7,9 would create the polynomial 5*x^3 + 0*x^2 + 7*x + 9.
     * @throws IllegalArgumentException if no coefficients are supplied
     * @throws NullPointerException if coefficients is null
     */
    public Polynomial(double... coefficients)
    {
        Preconditions.checkNotNull(coefficients, "Tried to create Polynomial with null coefficients.");
        Preconditions.checkArgument(coefficients.length > 0, "A polynomial needs at least one coefficient.");
        this.coefficients = coefficients.clone();
    }

    @Override
    public Double apply(double variable)
    {
        return applyAsDouble(variable);
    }

    /** Evaluates without boxing the result, preserving Horner's multiplication/addition rounding. */
    @Override
    public double applyAsDouble(double variable)
    {
        double result = this.coefficients[0];
        for (int i = 1, n = this.coefficients.length; i < n; ++i) {
            result *= variable;
            result += this.coefficients[i];
        }
        return result;
    }
}

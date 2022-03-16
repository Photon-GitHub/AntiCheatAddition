package de.photon.anticheataddition.util.mathematics;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.function.DoubleFunction;

/**
 * Represents a simple polynomial.
 * Safe to use asynchronously.
 * The evaluation will be done via Horner's method to reduce unnecessary calculations.
 */
public class Polynomial implements DoubleFunction<Double>
{
    private final double[] coefficients;

    /**
     * Constructs a new {@link Polynomial}.
     *
     * @param coefficients the coefficients of the polynomial, with descending degree
     *                     <p></p>
     *                     Example: 5,0,7,9 would create the polynomial 5*x^3 + 0*x^2 + 7*x + 9.
     */
    public Polynomial(double... coefficients)
    {
        this.coefficients = Preconditions.checkNotNull(coefficients, "Tried to create Polynomial with null coefficients.");
    }

    @Override
    public Double apply(double variable)
    {
        double result = this.coefficients[0];
        for (int i = 1, n = this.coefficients.length; i < n; ++i) {
            result *= variable;
            result += this.coefficients[i];
        }
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polynomial that = (Polynomial) o;
        return Arrays.equals(coefficients, that.coefficients);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(coefficients);
    }
}

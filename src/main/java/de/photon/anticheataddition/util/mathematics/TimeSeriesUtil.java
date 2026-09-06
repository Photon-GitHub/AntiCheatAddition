package de.photon.anticheataddition.util.mathematics;

/**
 * Pure operations on equally spaced samples. Inputs must be finite and nonempty unless stated otherwise.
 * Detrending returns a new array; centering modifies its input. Moments use population normalization.
 */
public final class TimeSeriesUtil
{
    private TimeSeriesUtil() {}

    /** Fits a quadratic at equally spaced positions in [-1, 1] and returns residuals. Requires at least three samples. */
    public static double[] detrendQuadratic(final double[] values)
    {
        final int length = values.length;
        if (length < 3) throw new IllegalArgumentException("quadratic detrending needs at least three samples");
        double sumY = 0D;
        double sumXY = 0D;
        double sumX2Y = 0D;
        double sumX2 = 0D;
        double sumX4 = 0D;

        for (int i = 0; i < length; i++) {
            final double x = normalizedIndex(i, length);
            final double xSquared = x * x;
            sumY += values[i];
            sumXY += x * values[i];
            sumX2Y += xSquared * values[i];
            sumX2 += xSquared;
            sumX4 += xSquared * xSquared;
        }

        final double determinant = length * sumX4 - sumX2 * sumX2;
        final double a = (sumY * sumX4 - sumX2 * sumX2Y) / determinant;
        final double b = sumXY / sumX2;
        final double c = (length * sumX2Y - sumX2 * sumY) / determinant;

        final double[] residuals = new double[length];
        for (int i = 0; i < length; i++) {
            final double x = normalizedIndex(i, length);
            residuals[i] = values[i] - (a + b * x + c * x * x);
        }
        return residuals;
    }

    private static double normalizedIndex(final int index, final int length)
    {
        return 2D * index / (length - 1D) - 1D;
    }

    /** Subtracts the population mean in place. */
    public static void center(final double[] values)
    {
        double mean = 0D;
        for (double value : values) mean += value;
        mean /= values.length;
        for (int i = 0; i < values.length; i++) values[i] -= mean;
    }

    /** Mean squared magnitude over the nonempty half-open interval [fromInclusive, toExclusive). */
    public static double meanSquare(final double[] values, final int fromInclusive, final int toExclusive)
    {
        double sum = 0D;
        for (int i = fromInclusive; i < toExclusive; i++) sum += values[i] * values[i];
        return sum / (toExclusive - fromInclusive);
    }

    /** Largest absolute sample magnitude. */
    public static double maximumAbsolute(final double[] values)
    {
        double maximum = 0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }

    /** Difference between the maximum and minimum samples. */
    public static double range(final double[] values)
    {
        double minimum = values[0];
        double maximum = values[0];
        for (int i = 1; i < values.length; i++) {
            minimum = Math.min(minimum, values[i]);
            maximum = Math.max(maximum, values[i]);
        }
        return maximum - minimum;
    }

    /** Fraction of adjacent nonzero pairs whose signs differ; zero when no pairs are comparable. */
    public static double signChangeRatio(final double[] values)
    {
        int signChanges = 0;
        int comparablePairs = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] == 0D || values[i] == 0D) continue;
            comparablePairs++;
            if (values[i - 1] * values[i] < 0D) signChanges++;
        }
        return comparablePairs == 0 ? 0D : signChanges / (double) comparablePairs;
    }

    /** Normalized order-three permutation entropy. Ties retain temporal order; fewer than three samples return zero. */
    public static double permutationEntropy(final double[] values)
    {
        if (values.length < 3) return 0D;
        final int[] counts = new int[6];
        for (int i = 0; i < values.length - 2; i++) counts[ordinalPattern(values[i], values[i + 1], values[i + 2])]++;

        final int total = values.length - 2;
        double entropy = 0D;
        for (int count : counts) {
            if (count == 0) continue;
            final double probability = count / (double) total;
            entropy -= probability * Math.log(probability);
        }
        return entropy / Math.log(6D);
    }

    private static int ordinalPattern(final double first, final double second, final double third)
    {
        if (first <= second) {
            if (second <= third) return 0;
            return first <= third ? 1 : 2;
        }
        if (first <= third) return 3;
        return second <= third ? 4 : 5;
    }

    /** Wald–Wolfowitz runs statistic around zero, ignoring zeros; infinity when too few signs are available. */
    public static double runsZScore(final double[] values)
    {
        int positive = 0;
        int negative = 0;
        int runs = 0;
        int previousSign = 0;
        for (double value : values) {
            final int sign = value > 0D ? 1 : value < 0D ? -1 : 0;
            if (sign == 0) continue;
            if (sign > 0) positive++;
            else negative++;
            if (sign != previousSign) runs++;
            previousSign = sign;
        }

        final int total = positive + negative;
        if (positive == 0 || negative == 0 || total < 4) return Double.POSITIVE_INFINITY;
        final double expected = 1D + 2D * positive * negative / total;
        final double variance = 2D * positive * negative * (2D * positive * negative - total) /
                                ((double) total * total * (total - 1D));
        return variance <= 0D ? 0D : (runs - expected) / Math.sqrt(variance);
    }

}

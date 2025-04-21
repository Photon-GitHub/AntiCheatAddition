package de.photon.anticheataddition.util.mathematics;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Kolmogorov–Smirnov helper utilities – two‑sided tests against U[min(data), max(data)].
 *
 * <p>Typical workflow:
 * <pre>{@code
 * double[] samples = …;
 * boolean ok = KolmogorovSmirnov.isUniform(samples, 0.05);   // 95 % confidence
 * }</pre>
 */
@UtilityClass
public final class KolmogorovSmirnov
{

    /*────────────────────────────  public API  ────────────────────────────*/

    /** Full result (D and p) for doubles. */
    public static KsResult uniformTest(double[] sample)
    {
        double[] norm = normalize(sample);
        return testSortedNormalized(norm);
    }

    /** Full result (D and p) for longs. */
    public static KsResult uniformTest(long[] sample)
    {
        double[] norm = normalize(sample);
        return testSortedNormalized(norm);
    }

    /**
     * Immutable result of a two‑sided Kolmogorov–Smirnov test.
     *
     * <p>Notation:</p>
     * <ul>
     *   <li>{@code dStatistic} – the Kolmogorov–Smirnov <em>D</em> statistic <br>D≔sup<sub>x</sub>|F<sub>n</sub>(x)‑F₀(x)|, range 0…1.</li>
     *   <li>{@code pValue} – the exact two‑sided tail probability P(D<sub>n</sub>≥D|H₀: sample∼U(min,max)).</li>
     * </ul>
     *
     * <p>Interpretation: A <strong>large D</strong> (or small p) signals that the
     * empirical CDF deviates strongly from the ideal CDF, therefore the
     * sample is unlikely to be drawn from that distribution.</p>
     *
     * @param dStatistic Kolmogorov–Smirnov D statistic.
     * @param pValue     Two‑sided p‑value (probability of observing D or larger
     *                   under the null hypothesis). Essentially the probability that
     *                   the sample is from the assumed distribution.
     */
    public record KsResult(double dStatistic, double pValue)
    {
        /**
         * <strong>Decision helper:</strong>returns {@code true} if the null
         * hypothesis of the distribution <em>cannot</em> be rejected at the given
         * significance level&nbsp;α.
         *
         * @param alpha significance level in(0,1), e.g.0.05 for 95% confidence
         *
         * @return {@code true} iff p≥α
         */
        public boolean significanceTest(double alpha)
        {
            Preconditions.checkArgument(alpha > 0 && alpha < 1, "alpha must be in (0,1)");
            return pValue >= alpha;
        }
    }

    /*────────────────────────  normalisation helpers  ─────────────────────*/

    /** Normalise arbitrary doubles to [0,1] and sort ascending. */
    public static double[] normalize(double[] data)
    {
        if (data.length == 0) return new double[0];

        double min = Doubles.min(data);
        double max = Doubles.max(data);
        if (min == max) return new double[data.length];

        return Arrays.stream(data)
                     .map(x -> (x - min) / (max - min))
                     .sorted()
                     .toArray();
    }

    /** Normalise arbitrary longs to [0,1] and sort ascending. */
    public static double[] normalize(long[] data)
    {
        if (data.length == 0) return new double[0];

        double min = Longs.min(data);
        double max = Longs.max(data);
        if (min == max) return new double[data.length];

        return Arrays.stream(data)
                     .mapToDouble(x -> (x - min) / (max - min))
                     .sorted()
                     .toArray();
    }

    /*────────────────────  internal KS implementation  ───────────────────*/

    private static KsResult testSortedNormalized(double[] sortedNorm)
    {
        final int n = sortedNorm.length;
        Preconditions.checkArgument(n > 1, "The KS test requires at least 2 data points");

        final double d = ksStatistic(sortedNorm, n);
        final double lambda = (Math.sqrt(n) + 0.12 + 0.11 / Math.sqrt(n)) * d;
        final double p = ksPValue(lambda);
        return new KsResult(d, p);
    }

    /** Two‑sided D = sup|Fₙ(x)‑x|. */
    private static double ksStatistic(double[] x, int n)
    {
        return IntStream.range(0, n)
                        .mapToDouble(i -> {
                            double empUp = (i + 1.0) / n;
                            double empDown = i / (double) n;
                            double diffUp = Math.abs(empUp - x[i]);
                            double diffDown = Math.abs(x[i] - empDown);
                            return Math.max(diffUp, diffDown);
                        })
                        .max()
                        .orElse(0.0);
    }

    /**
     * Tail probability Q<sub>KS</sub>(λ)=2Σ<sub>j=1…∞</sub>(‑1)<sup>j‑1</sup>e<sup>‑2j²λ²</sup>.
     * <p>The loop stops when the next term would change the sum by &lt;1e‑12
     * or after 1000 terms – that is enough even for tiny p.</p>
     */
    private static double ksPValue(double lambda)
    {
        double sum = 0.0;
        for (int j = 1; j <= 1000; j++) {
            double sign = (j & 1) == 1 ? 1.0 : -1.0;          // faster than Math.pow
            double term = 2.0 * sign * Math.exp(-2.0 * j * j * lambda * lambda);
            sum += term;
            if (Math.abs(term) < 1e-12) break;
        }
        return Math.clamp(sum, 0.0, 1.0);
    }
}
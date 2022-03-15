package de.photon.anticheataddition.util.mathematics;

import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class PolynomialTest
{
    @Test
    void zeroTest()
    {
        val random = new Random();
        val results = new double[5];
        results[0] = new Polynomial(0).apply(0);
        results[1] = new Polynomial(0).apply(random.nextDouble());
        results[2] = new Polynomial(random.nextDouble(), 0).apply(0);
        results[3] = new Polynomial(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0).apply(0);
        results[4] = new Polynomial(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), 0).apply(0);

        // Only zeros.
        Assertions.assertArrayEquals(new double[]{0, 0, 0, 0, 0}, results);
    }

    @Test
    void constantTest()
    {
        val samples = 10;
        val random = new Random();

        val actual = new double[samples];
        val expected = new double[samples];

        for (int i = 0; i < samples; i++) {
            double coefficient = random.nextDouble();
            actual[i] = new Polynomial(coefficient).apply(random.nextDouble());
            expected[i] = coefficient;
        }

        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    void evaluationTest()
    {
        final int samples = 10;
        final double epsilon = 0.000001D;

        val polynomial = new Polynomial(7, 5, 1, 9);

        double actual;
        double expected;
        double x;
        for (int i = 0; i < samples; ++i) {
            x = ThreadLocalRandom.current().nextDouble(-500, 500);
            actual = polynomial.apply(x);
            expected = 7 * x * x * x + 5 * x * x + x + 9;
            Assertions.assertTrue(MathUtil.inRange(expected - epsilon, expected + epsilon, expected), "Evaluation failed: Epsilon " + epsilon + " | x " + x + " | actual " + actual + " | expected " + expected);
        }
    }
}

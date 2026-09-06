package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class MathematicsRegressionTest
{
    @Test
    void polynomialOwnsItsCoefficientsAndSupportsPrimitiveEvaluation()
    {
        double[] coefficients = {2, 3, 4};
        Polynomial polynomial = new Polynomial(coefficients);
        int hash = polynomial.hashCode();
        coefficients[0] = 99;
        assertEquals(new Polynomial(2, 3, 4), polynomial);
        assertEquals(hash, polynomial.hashCode());
        DoubleUnaryOperator primitive = polynomial;
        DoubleFunction<Double> boxed = polynomial;
        assertEquals(18D, primitive.applyAsDouble(2));
        assertEquals(18D, boxed.apply(2));
        assertThrows(IllegalArgumentException.class, Polynomial::new);
        assertThrows(NullPointerException.class, () -> new Polynomial((double[]) null));
    }

    @Test
    void modularOperationsReduceWithoutIntermediateOverflow()
    {
        int mod = Integer.MAX_VALUE;
        int initial = mod - 1;
        BigInteger base = BigInteger.valueOf(initial);
        BigInteger modulus = BigInteger.valueOf(mod);
        for (int operand : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            BigInteger value = BigInteger.valueOf(operand);
            assertEquals(base.add(value).mod(modulus).intValue(), new ModularInteger(initial, mod).add(operand).get());
            assertEquals(base.subtract(value).mod(modulus).intValue(), new ModularInteger(initial, mod).sub(operand).get());
            assertEquals(base.multiply(value).mod(modulus).intValue(), new ModularInteger(initial, mod).mul(operand).get());
        }
    }

    @Test
    void averagesAndGaussianSumAvoidIntermediateIntegerOverflow()
    {
        assertEquals(Integer.MAX_VALUE, DataUtil.average(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals((double) Long.MAX_VALUE, DataUtil.average(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(2_147_450_880, MathUtil.gaussianSumFormulaTo(65_535));
    }

    @Test
    void signChangesDoNotDependOnMagnitude()
    {
        assertEquals(1D, TimeSeriesUtil.signChangeRatio(new double[]{Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_VALUE}));
        assertEquals(0D, TimeSeriesUtil.signChangeRatio(new double[]{-0D, 0D}));
    }

    @Test
    void correlationAvoidsOverflowInProductOfVariances()
    {
        double[] values = {-1E100, 0, 1E100};
        assertEquals(1D, DataUtil.correlation(values, values), 1E-15D);
    }

    @Test
    void boundingBoxCopyIsIndependentAndPreservesEqualityAndHash()
    {
        AxisAlignedBB box = new AxisAlignedBB(-1, -2, -3, 1, 2, 3);
        AxisAlignedBB copy = box.clone();
        assertNotSame(box, copy);
        assertEquals(box, copy);
        assertEquals(box.hashCode(), copy.hashCode());
        copy.offset(1, 0, 0);
        assertNotEquals(box, copy);
        assertEquals(-1D, box.getMinX());
    }
}

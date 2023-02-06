package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataUtilTest
{
    private static final double DELTA = 0.001;

    @Test
    void testSquaredError()
    {
        Assertions.assertEquals(0, DataUtil.squaredError(0, 0));
        Assertions.assertEquals(0, DataUtil.squaredError(Integer.MAX_VALUE, Integer.MAX_VALUE));
        Assertions.assertEquals(0, DataUtil.squaredError(Double.MAX_VALUE, Double.MAX_VALUE), DELTA);
        Assertions.assertEquals(25, DataUtil.squaredError(0, 5));
        Assertions.assertEquals(3, DataUtil.squaredError(0, 1D, 1D, 1D), DELTA);
        Assertions.assertEquals(171.5, DataUtil.squaredError(0, -4D, 1.5D, 2D, 7D, 8D, 0D, 4D, -4.5D), DELTA);
        Assertions.assertEquals(66.25, DataUtil.squaredError(-4, -4D, 1.5D, 2D), DELTA);
    }
}

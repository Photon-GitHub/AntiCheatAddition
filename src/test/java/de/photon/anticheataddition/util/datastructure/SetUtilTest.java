package de.photon.anticheataddition.util.datastructure;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;


class SetUtilTest
{
    @Test
    void enumSetCollectorTest()
    {
        val result = Arrays.stream(TestEnum.values()).collect(SetUtil.toImmutableEnumSet());
        Assertions.assertIterableEquals(EnumSet.allOf(TestEnum.class), result);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.remove(TestEnum.TEST1));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.add(TestEnum.TEST1));
    }

    @Test
    void differenceTest()
    {
        val first = EnumSet.allOf(TestEnum.class);
        val second = EnumSet.of(TestEnum.TEST1, TestEnum.TEST2);

        val result = EnumSet.noneOf(TestEnum.class);
        result.addAll(SetUtil.difference(first, second));

        Assertions.assertIterableEquals(EnumSet.complementOf(second), result);
    }

    private enum TestEnum
    {
        TEST1,
        TEST2,
        TEST3,
        TEST4,
        TEST5
    }
}

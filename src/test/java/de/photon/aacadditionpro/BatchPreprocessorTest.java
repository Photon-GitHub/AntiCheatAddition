package de.photon.aacadditionpro;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.util.datastructure.ImmutablePair;
import de.photon.aacadditionpro.util.datastructure.batch.BatchPreprocessors;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class BatchPreprocessorTest
{
    @Test
    void emptyTest()
    {
        List<DoubleStatistics> statistics = ImmutableList.of(new DoubleStatistics(), new DoubleStatistics());

        Assertions.assertEquals(Collections.emptyList(), BatchPreprocessors.zipToPairs(Collections.emptyList()));
        Assertions.assertEquals(Collections.emptyList(), BatchPreprocessors.zipOffsetOne(Collections.emptyList()));
        Assertions.assertEquals(statistics, BatchPreprocessors.reducePairToDoubleStatistics(Collections.emptyList(), (a, b) -> 5, (a, b) -> 10));
        Assertions.assertEquals(statistics, BatchPreprocessors.zipReduceToDoubleStatistics(Collections.emptyList(), (a, b) -> 5, (a, b) -> 10));
        Assertions.assertEquals(Collections.emptyList(), BatchPreprocessors.combineTwoObjectsToEnd(Collections.emptyList(), (a, b) -> 42));
        Assertions.assertEquals(Collections.emptyList(), BatchPreprocessors.combineTwoObjectsToStart(Collections.emptyList(), (a, b) -> 42));
    }

    @Test
    void zipToPairsTest()
    {
        // One element is ignored.
        Assertions.assertEquals(ImmutableList.of(), BatchPreprocessors.zipToPairs(ImmutableList.of(1)));
        Assertions.assertEquals(ImmutableList.of(ImmutablePair.of(1, 2)), BatchPreprocessors.zipToPairs(ImmutableList.of(1, 2)));
        Assertions.assertEquals(ImmutableList.of(ImmutablePair.of(1, 2), ImmutablePair.of(3, 4), ImmutablePair.of(5, 6)), BatchPreprocessors.zipToPairs(ImmutableList.of(1, 2, 3, 4, 5, 6)));
        // Last element ignore.
        Assertions.assertEquals(ImmutableList.of(ImmutablePair.of(1, 2), ImmutablePair.of(3, 4), ImmutablePair.of(5, 6)), BatchPreprocessors.zipToPairs(ImmutableList.of(1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    void zipOffsetOneTest()
    {
        // One element is ignored.
        Assertions.assertEquals(ImmutableList.of(), BatchPreprocessors.zipOffsetOne(ImmutableList.of(1)));
        Assertions.assertEquals(ImmutableList.of(ImmutablePair.of(1, 2)), BatchPreprocessors.zipOffsetOne(ImmutableList.of(1, 2)));
        Assertions.assertEquals(ImmutableList.of(ImmutablePair.of(1, 2), ImmutablePair.of(2, 3)), BatchPreprocessors.zipOffsetOne(ImmutableList.of(1, 2, 3)));
    }

    @Test
    void reducePairToDoubleStatisticsTest()
    {
        val reduceOne = BatchPreprocessors.reducePairToDoubleStatistics(ImmutableList.of(ImmutablePair.of(1, 2)), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(1, reduceOne.get(0).getSum());
        Assertions.assertEquals(2, reduceOne.get(1).getSum());

        val reduceTwo = BatchPreprocessors.reducePairToDoubleStatistics(ImmutableList.of(ImmutablePair.of(1, 2), ImmutablePair.of(2, 3)), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(3, reduceTwo.get(0).getSum());
        Assertions.assertEquals(5, reduceTwo.get(1).getSum());
    }

    @Test
    void zipReduceToDoubleStatisticsTest()
    {
        val reduceOne = BatchPreprocessors.zipReduceToDoubleStatistics(ImmutableList.of(1, 2), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(1, reduceOne.get(0).getSum());
        Assertions.assertEquals(2, reduceOne.get(1).getSum());

        val reduceTwo = BatchPreprocessors.zipReduceToDoubleStatistics(ImmutableList.of(1, 2, 3), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(3, reduceTwo.get(0).getSum());
        Assertions.assertEquals(5, reduceTwo.get(1).getSum());
    }
}

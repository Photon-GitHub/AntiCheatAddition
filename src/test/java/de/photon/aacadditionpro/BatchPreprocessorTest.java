package de.photon.aacadditionpro;

import de.photon.aacadditionpro.util.datastructure.Pair;
import de.photon.aacadditionpro.util.datastructure.batch.BatchPreprocessors;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class BatchPreprocessorTest
{
    @Test
    void emptyTest()
    {
        List<DoubleStatistics> statistics = List.of(new DoubleStatistics(), new DoubleStatistics());

        Assertions.assertEquals(List.of(), BatchPreprocessors.zipToPairs(List.of()));
        Assertions.assertEquals(List.of(), BatchPreprocessors.zipOffsetOne(List.of()));
        Assertions.assertEquals(statistics, BatchPreprocessors.reducePairToDoubleStatistics(List.of(), (a, b) -> 5, (a, b) -> 10));
        Assertions.assertEquals(statistics, BatchPreprocessors.zipReduceToDoubleStatistics(List.of(), (a, b) -> 5, (a, b) -> 10));
        Assertions.assertEquals(List.of(), BatchPreprocessors.combineTwoObjectsToEnd(List.of(), (a, b) -> 42));
        Assertions.assertEquals(List.of(), BatchPreprocessors.combineTwoObjectsToStart(List.of(), (a, b) -> 42));
    }

    @Test
    void zipToPairsTest()
    {
        // One element is ignored.
        Assertions.assertEquals(List.of(), BatchPreprocessors.zipToPairs(List.of(1)));
        Assertions.assertEquals(List.of(Pair.of(1, 2)), BatchPreprocessors.zipToPairs(List.of(1, 2)));
        Assertions.assertEquals(List.of(Pair.of(1, 2), Pair.of(3, 4), Pair.of(5, 6)), BatchPreprocessors.zipToPairs(List.of(1, 2, 3, 4, 5, 6)));
        // Last element ignore.
        Assertions.assertEquals(List.of(Pair.of(1, 2), Pair.of(3, 4), Pair.of(5, 6)), BatchPreprocessors.zipToPairs(List.of(1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    void zipOffsetOneTest()
    {
        // One element is ignored.
        Assertions.assertEquals(List.of(), BatchPreprocessors.zipOffsetOne(List.of(1)));
        Assertions.assertEquals(List.of(Pair.of(1, 2)), BatchPreprocessors.zipOffsetOne(List.of(1, 2)));
        Assertions.assertEquals(List.of(Pair.of(1, 2), Pair.of(2, 3)), BatchPreprocessors.zipOffsetOne(List.of(1, 2, 3)));
    }

    @Test
    void reducePairToDoubleStatisticsTest()
    {
        val reduceOne = BatchPreprocessors.reducePairToDoubleStatistics(List.of(Pair.of(1, 2)), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(1, reduceOne.get(0).getSum());
        Assertions.assertEquals(2, reduceOne.get(1).getSum());

        val reduceTwo = BatchPreprocessors.reducePairToDoubleStatistics(List.of(Pair.of(1, 2), Pair.of(2, 3)), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(3, reduceTwo.get(0).getSum());
        Assertions.assertEquals(5, reduceTwo.get(1).getSum());
    }

    @Test
    void zipReduceToDoubleStatisticsTest()
    {
        val reduceOne = BatchPreprocessors.zipReduceToDoubleStatistics(List.of(1, 2), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(1, reduceOne.get(0).getSum());
        Assertions.assertEquals(2, reduceOne.get(1).getSum());

        val reduceTwo = BatchPreprocessors.zipReduceToDoubleStatistics(List.of(1, 2, 3), (a, b) -> a, (a, b) -> b);
        Assertions.assertEquals(3, reduceTwo.get(0).getSum());
        Assertions.assertEquals(5, reduceTwo.get(1).getSum());
    }
}

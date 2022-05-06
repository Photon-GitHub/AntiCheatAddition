package de.photon.anticheataddition.util.datastructure.batch;

import com.google.common.collect.Lists;
import de.photon.anticheataddition.util.datastructure.Pair;
import de.photon.anticheataddition.util.datastructure.statistics.DoubleStatistics;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

@UtilityClass
public final class BatchPreprocessors
{
    /**
     * This will zip neighboring elements of the list.
     * This returns (0, 1), (2, 3), (4, 5) ...
     * Therefore, every element is at most in one pair.
     * <p>
     * The last element may be ignored if a pair cannot fully be formed.
     */
    public static <T> List<Pair<T, T>> zipToPairs(List<T> input)
    {
        final List<Pair<T, T>> output = new ArrayList<>(input.size());
        T last = null;
        for (T t : input) {
            if (last == null) last = t;
            else {
                output.add(Pair.of(last, t));
                last = null;
            }
        }
        return output;
    }

    /**
     * This will zip neighboring elements of the list.
     * This returns (0, 1), (1, 2), (2, 3) ...
     * Therefore, elements can appear multiple times in the list!
     * <p>
     * The last element may be ignored if a pair cannot fully be formed.
     */
    public static <T> List<Pair<T, T>> zipOffsetOne(List<T> input)
    {
        return combineTwoObjectsToEnd(input, Pair::of);
    }

    /**
     * This method allows for the reduction from a {@link List} of {@link Pair}s to an {@link Pair} of {@link DoubleStatistics}, which is often used in {@link BatchProcessor}s to
     * calculate both the expected and the actual delays.
     */
    @SafeVarargs
    public static <T> List<DoubleStatistics> reducePairToDoubleStatistics(List<Pair<T, T>> input, ToDoubleBiFunction<T, T>... mappers)
    {
        val statistics = new DoubleStatistics[mappers.length];
        for (int i = 0; i < statistics.length; ++i) statistics[i] = new DoubleStatistics();

        for (val pair : input) {
            for (int i = 0; i < mappers.length; ++i) {
                statistics[i].accept(mappers[i].applyAsDouble(pair.getFirst(), pair.getSecond()));
            }
        }

        return List.of(statistics);
    }

    /**
     * Shortcut for often used reducePairToDoubleStatistics(zipOffsetOne(...), ... ) with performance improvements.
     */
    // We only use the varargs in a loop, we do not return them or cast them  -> safe varargs.
    @SafeVarargs
    public static <T> List<DoubleStatistics> zipReduceToDoubleStatistics(List<T> input, ToDoubleBiFunction<T, T>... mappers)
    {
        val statistics = new DoubleStatistics[mappers.length];
        for (int i = 0; i < statistics.length; ++i) statistics[i] = new DoubleStatistics();

        if (!input.isEmpty()) {
            final Iterator<T> iterator = input.iterator();
            T old = iterator.next();
            T current;

            while (iterator.hasNext()) {
                current = iterator.next();

                for (int i = 0; i < mappers.length; ++i) statistics[i].accept(mappers[i].applyAsDouble(old, current));

                old = current;
            }
        }
        return List.of(statistics);
    }

    /**
     * <p>Combines two element according to a BiFunction.</p>
     * <p>This will take combine n elements to n-1 elements.</p>
     *
     * <p>It will call the combiner function for two elements of the list, omitting the first one in its first call.
     * combiner will be called according to the following scheme:</p>
     * <p>combiner(0,1) -> combiner(1,2) -> combiner(2,3) -> ... -> combiner(n-2, n-1)</p>
     * with n-1 being the last element in the list.
     *
     * @param input    the input list that shall be processed.
     * @param combiner the combiner function that is used to combine two elements.
     *
     * @return an empty {@link List} if input has less than two elements or the {@link List} of the combined elements.
     */
    public static <T, U> List<U> combineTwoObjectsToEnd(List<T> input, BiFunction<T, T, U> combiner)
    {
        if (input.isEmpty()) return List.of();

        final List<U> output = new ArrayList<>(input.size());
        final Iterator<T> iterator = input.iterator();
        T old = iterator.next();
        T current;
        while (iterator.hasNext()) {
            current = iterator.next();
            output.add(combiner.apply(old, current));
            old = current;
        }
        return output;
    }

    /**
     * <p>Combines two element according to a BiFunction.</p>
     * <p>This will take combine n elements to n-1 elements.</p>
     *
     * <p>It will call the combiner function for two elements of the list, omitting the last one in its first call.
     * combiner will be called according to the following scheme:</p>
     * <p>combiner(n-1,n-2) -> combiner(n-2,n-3) -> combiner(n-3,n-4) -> ... -> combiner(1, 0)</p>
     * with n-1 being the last element in the list.
     *
     * @param input    the input list that shall be processed.
     * @param combiner the combiner function that is used to combine two elements.
     *
     * @return an empty {@link List} if input has less than two elements or the {@link List} of the combined elements.
     */
    public static <T, U> List<U> combineTwoObjectsToStart(List<T> input, BiFunction<T, T, U> combiner)
    {
        return combineTwoObjectsToEnd(Lists.reverse(input), combiner);
    }
}

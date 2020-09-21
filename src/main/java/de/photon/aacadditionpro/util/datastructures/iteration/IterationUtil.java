package de.photon.aacadditionpro.util.datastructures.iteration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Provides special iteration methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IterationUtil
{
    /**
     * This uses {@link #twoObjectsIterationToEnd(List, BiConsumer)} to map a {@link List} to a {@link List} of between objects.
     *
     * @param iterable  the {@link List} to iterate over.
     * @param predicate a {@link BiPredicate} to check whether a pair of Ts shall be mapped with the mapping function.
     * @param mapping   the function that will be called on each pair of Ts via {@link #twoObjectsIterationToEnd(List, BiConsumer)} to create a new object that will be inserted into the result list.
     */
    public static <T, V> List<V> pairCombine(List<T> iterable, BiPredicate<T, T> predicate, BiFunction<T, T, V> mapping)
    {
        final List<V> result = new ArrayList<>(iterable.size() - 1);
        twoObjectsIterationToEnd(iterable, (old, current) -> {
            if (predicate.test(old, current)) {
                result.add(mapping.apply(old, current));
            }
        });
        return result;
    }

    /**
     * This uses {@link #twoObjectsIterationToEnd(List, BiConsumer)} to map a {@link List} to a {@link List} of between objects.
     *
     * @param iterable the {@link List} to iterate over.
     * @param mapping  the function that will be called on each pair of Ts via {@link #twoObjectsIterationToEnd(List, BiConsumer)} to create a new object that will be inserted into the result list.
     */
    public static <T, V> List<V> pairCombine(List<T> iterable, BiFunction<T, T, V> mapping)
    {
        return pairCombine(iterable, (old, current) -> true, mapping);
    }

    /**
     * Allows for an iteration with two objects present each time.
     * Assumes that the newer objects are added at the end of the {@link List}.
     */
    public static <T> void twoObjectsIterationToEnd(List<T> iterable, BiConsumer<T, T> consumer)
    {
        if (iterable instanceof RandomAccess) {
            for (int i = 1, n = iterable.size(); i < n; ++i) {
                consumer.accept(iterable.get(i - 1), iterable.get(i));
            }
        } else {
            twoObjectsIterationToEnd((Iterable<T>) iterable, consumer);
        }
    }

    /**
     * Allows for an iteration with two objects present each time.
     * Assumes that the newer objects are added at the end of the {@link Iterable}.
     */
    public static <T> void twoObjectsIterationToEnd(Iterable<T> iterable, BiConsumer<T, T> consumer)
    {
        twoObjectsIteration(iterable.iterator(), consumer);
    }

    /**
     * Allows for an iteration with two objects present each time.
     * Assumes that the newer objects are added at the front of the {@link Deque}.
     */
    public static <T> void twoObjectsIterationFromEnd(Deque<T> iterable, BiConsumer<T, T> consumer)
    {
        twoObjectsIteration(iterable.descendingIterator(), consumer);
    }

    /**
     * Allows for an iteration with two objects present each time.
     * Assumes that the newer objects are added at the front of the {@link List}.
     */
    public static <T> void twoObjectsIterationFromEnd(List<T> iterable, BiConsumer<T, T> consumer)
    {
        for (int i = iterable.size() - 1; i > 0; --i) {
            consumer.accept(iterable.get(i), iterable.get(i - 1));
        }
    }

    /**
     * Util method for custom iterator.
     */
    private static <T> void twoObjectsIteration(Iterator<T> itr, BiConsumer<T, T> consumer)
    {
        T old;
        T current;

        // At least one element present.
        if (!itr.hasNext()) {
            return;
        }

        current = itr.next();
        while (itr.hasNext()) {
            old = current;
            current = itr.next();

            consumer.accept(old, current);
        }
    }
}

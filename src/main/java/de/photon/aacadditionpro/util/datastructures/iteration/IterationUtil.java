package de.photon.aacadditionpro.util.datastructures.iteration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.BiConsumer;

/**
 * Provides special iteration methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IterationUtil
{
    /**
     * Allows for an iteration with two objects present each time.
     * Assumes that the newer objects are added at the end of the list.
     */
    public static <T> void twoObjectsIterationToEnd(List<T> iterable, BiConsumer<T, T> consumer)
    {
        if (iterable instanceof RandomAccess) {
            for (int i = 1, n = iterable.size(); i < n; i++) {
                consumer.accept(iterable.get(i - 1), iterable.get(i));
            }
        } else {
            twoObjectsIterationToEnd((Iterable<T>) iterable, consumer);
        }
    }

    /**
     * Allows for an iteration with two objects present each time.
     * Assumes that the newer objects are added at the end of the list.
     */
    public static <T> void twoObjectsIterationToEnd(Iterable<T> iterable, BiConsumer<T, T> consumer)
    {
        Iterator<T> itr = iterable.iterator();
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

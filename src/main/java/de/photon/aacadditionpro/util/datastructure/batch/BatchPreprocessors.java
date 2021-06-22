package de.photon.aacadditionpro.util.datastructure.batch;

import de.photon.aacadditionpro.util.datastructure.ImmutablePair;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchPreprocessors
{
    /**
     * This will zip neighboring elements of the list.
     * This returns (0, 1), (2, 3), (4, 5) ...
     * Therefore every element is at most in one pair.
     * <p>
     * The last element may be ignored if a pair cannot fully be formed.
     */
    public static <T> List<ImmutablePair<T, T>> zipToPairs(List<T> input)
    {
        final List<ImmutablePair<T, T>> output = new ArrayList<>(input.size());
        T last = null;
        for (T t : input) {
            if (last == null) last = t;
            else {
                output.add(ImmutablePair.of(last, t));
                last = null;
            }
        }
        return output;
    }

    /**
     * This will zip neighboring elements of the list.
     * This returns (0, 1), (1, 2), (2, 3) ...
     * Therefore elements can appear multiple times in the list!
     * <p>
     * The last element may be ignored if a pair cannot fully be formed.
     */
    public static <T> List<ImmutablePair<T, T>> zipOffsetOne(List<T> input)
    {
        return combineTwoObjectsToEnd(input, ImmutablePair::of);
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
        final List<U> output = new ArrayList<>(input.size());

        T old = input.get(0);
        T current;
        final ListIterator<T> iterator = input.listIterator(1);
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
        final List<U> output = new ArrayList<>(input.size());

        T old = input.get(input.size() - 1);
        for (int i = input.size() - 2; i >= 0; --i) {
            T current = input.get(i);
            output.add(combiner.apply(old, current));
            old = current;
        }
        return output;
    }
}

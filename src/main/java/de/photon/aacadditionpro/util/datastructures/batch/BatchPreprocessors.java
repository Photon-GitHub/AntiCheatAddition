package de.photon.aacadditionpro.util.datastructures.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

public final class BatchPreprocessors
{
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
    public static <T> List<T> combineTwoObjectsToEnd(List<T> input, BinaryOperator<T> combiner)
    {
        final List<T> output = new ArrayList<>(input.size());

        T old = input.get(0);
        for (int i = 1; i < input.size(); ++i) {
            T current = input.get(i);
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
    public static <T> List<T> combineTwoObjectsToStart(List<T> input, BinaryOperator<T> combiner)
    {
        final List<T> output = new ArrayList<>(input.size());

        T old = input.get(input.size() - 1);
        for (int i = input.size() - 2; i >= 0; --i) {
            T current = input.get(i);
            output.add(combiner.apply(old, current));
            old = current;
        }
        return output;
    }
}

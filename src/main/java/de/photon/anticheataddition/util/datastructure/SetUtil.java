package de.photon.anticheataddition.util.datastructure;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class SetUtil
{
    /**
     * This computes the difference of two {@link Set}s.
     * Compared to {@link com.google.common.collect.Sets#difference(Set, Set)} this actually computes the {@link Set} and does not return a view, therefore guaranteeing that
     * all computations are only performed once.
     */
    public static <T> Set<T> difference(Set<T> from, Set<T> remove)
    {
        final Set<T> result = new HashSet<>();
        for (T entity : from) {
            if (!remove.contains(entity)) result.add(entity);
        }
        return result;
    }
}

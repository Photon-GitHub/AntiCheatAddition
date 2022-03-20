package de.photon.anticheataddition.util.datastructure;

import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;

@UtilityClass
public class SetUtil
{
    /**
     * Directly collects a stream to an immutable EnumSet.
     * Guavas {@link Sets#toImmutableEnumSet()} cannot be used as minecraft 1.8's guava is very old.
     */
    public static <E extends Enum<E>> Collector<E, ?, Set<E>> toImmutableEnumSet()
    {
        //noinspection unchecked,rawtypes
        return (Collector) EnumSetAccumulator.TO_IMMUTABLE_ENUM_SET;
    }

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

    /**
     * Copy of the newer guava method to accumulate to immutable {@link EnumSet}s, as minecraft 1.8 does not have that method.
     */
    private static class EnumSetAccumulator<E extends Enum<E>>
    {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static final Collector<Enum<?>, ?, Set<? extends Enum<?>>> TO_IMMUTABLE_ENUM_SET =
                (Collector) Collector.<Enum, EnumSetAccumulator, Set<?>>of(
                        EnumSetAccumulator::new,
                        EnumSetAccumulator::add,
                        EnumSetAccumulator::combine,
                        EnumSetAccumulator::toImmutableSet,
                        Collector.Characteristics.UNORDERED);

        private EnumSet<E> set = null;

        public void add(E element)
        {
            if (set == null) set = EnumSet.of(element);
            else set.add(element);
        }

        public EnumSetAccumulator<E> combine(EnumSetAccumulator<E> other)
        {
            if (this.set == null) return other;
            else if (other.set == null) return this;
            else {
                this.set.addAll(other.set);
                return this;
            }
        }

        public Set<E> toImmutableSet()
        {
            return (set == null) ? Set.of() : Sets.immutableEnumSet(set);
        }
    }
}

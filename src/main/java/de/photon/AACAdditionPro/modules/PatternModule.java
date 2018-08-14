package de.photon.AACAdditionPro.modules;

import java.util.Set;
import java.util.function.Function;

/**
 * This indicates a {@link Module} which can be split up into different parts called {@link Pattern}s.
 */
public interface PatternModule extends Module
{
    /**
     * @return a {@link Set} of all {@link Pattern}s managed by this {@link PatternModule}
     */
    Set<Pattern> getPatterns();

    static void enablePatterns(final PatternModule module)
    {
        for (Pattern pattern : module.getPatterns())
        {
            Module.enableModule(pattern);
        }
    }

    /**
     * Represents a single {@link Pattern} that is hold by a {@link PatternModule}
     */
    abstract class Pattern<T> implements Function<T, Integer>, Module
    {
        /**
         * The name of the pattern in the config.
         */
        abstract String getName();

        @Override
        public String getConfigString()
        {
            return this.getModuleType().getConfigString() + '.' + this.getName();
        }
    }
}

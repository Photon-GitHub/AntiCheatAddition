package de.photon.AACAdditionPro.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.user.User;

import java.util.Set;
import java.util.function.BiFunction;

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

    static void disablePatterns(final PatternModule module)
    {
        for (Pattern pattern : module.getPatterns())
        {
            Module.disableModule(pattern);
        }
    }

    /**
     * Represents a single {@link Pattern} that is hold by a {@link PatternModule}
     */
    abstract class Pattern<T, U> implements BiFunction<T, U, Integer>, Module
    {
        protected boolean enabled = false;

        @Override
        public Integer apply(T t, U u)
        {
            return this.enabled ? process(t, u) : 0;
        }

        @Override
        public abstract String getConfigString();

        /**
         * Actually process the data.
         *
         * @return the vlIncrease of the {@link PatternModule}.
         */
        protected abstract int process(T t, U u);

        @Override
        public boolean shouldNotify()
        {
            return true;
        }

        @Override
        public void enable()
        {
            this.enabled = true;
        }

        @Override
        public void disable()
        {
            this.enabled = false;
        }
    }

    /**
     * Special handling class for {@link Pattern}s that use packets.
     * This class will automatically ensure that only the correct packets are used.
     */
    abstract class PacketPattern extends Pattern<User, PacketContainer>
    {
        private final Set<PacketType> packetTypesProcessed;

        /**
         * Constructs a new {@link PacketPattern}.
         *
         * @param packetTypesProcessed the {@link PacketType}s this {@link PacketPattern} will use.
         */
        protected PacketPattern(Set<PacketType> packetTypesProcessed)
        {
            this.packetTypesProcessed = packetTypesProcessed;
        }

        @Override
        public Integer apply(User user, PacketContainer packetContainer)
        {
            return this.enabled ?
                   (packetTypesProcessed.contains(packetContainer.getType()) ?
                    this.process(user, packetContainer) :
                    0) :
                   0;
        }
    }
}

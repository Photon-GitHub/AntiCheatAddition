package de.photon.aacadditionpro.command;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandAttributes
{
    private final int minArguments;
    private final int maxArguments;
    private final List<String> commandHelp;

    /**
     * Checks if the given argument count is valid by the attributes.
     *
     * @param arguments the argument count.
     *
     * @return <code>true</code> iff minArguments <= arguments <= maxArguments
     */
    public boolean argumentsInRange(int arguments)
    {
        return MathUtils.inRange(minArguments, maxArguments, arguments);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Factory for building a {@link CommandAttributes} instance.
     */
    public static class Builder
    {
        private int minArguments = 0;
        private int maxArguments = Byte.MAX_VALUE;
        private final List<String> commandHelp = new ArrayList<>();

        /**
         * The minimum arguments of the command that should be enforced.
         */
        public Builder minArguments(int minArguments)
        {
            this.minArguments = minArguments;
            return this;
        }

        /**
         * The maximum arguments of the command that should be enforced.
         */
        public Builder maxArguments(int maxArguments)
        {
            this.maxArguments = maxArguments;
            return this;
        }

        /**
         * Shortcut for setting both the minimum and maximum arguments to the same value.
         */
        public Builder exactArguments(int exactArg)
        {
            this.minArguments = exactArg;
            this.maxArguments = exactArg;
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder setCommandHelp(List<String> commandHelp)
        {
            this.commandHelp.clear();
            this.commandHelp.addAll(commandHelp);
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder setCommandHelp(String... commandHelp)
        {
            this.commandHelp.clear();
            this.commandHelp.addAll(Arrays.asList(commandHelp));
            return this;
        }

        /**
         * Add a single line to the command help.
         */
        public Builder addCommandHelpLine(String line)
        {
            this.commandHelp.add(line);
            return this;
        }

        public CommandAttributes build()
        {
            return new CommandAttributes(this.minArguments, this.maxArguments, ImmutableList.copyOf(this.commandHelp));
        }
    }
}

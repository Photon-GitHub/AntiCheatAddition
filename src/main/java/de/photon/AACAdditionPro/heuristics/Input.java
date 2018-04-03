package de.photon.AACAdditionPro.heuristics;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to provide input data for {@link Pattern}s.
 */
@AllArgsConstructor(suppressConstructorProperties = true)
public class Input
{
    @Getter
    private InputType inputType;
    public double[] data;

    /**
     * Determines whether the other {@link Input} is of the same {@link InputType}.
     */
    public boolean sameType(final Input other)
    {
        return this.inputType == other.inputType;
    }

    @AllArgsConstructor(suppressConstructorProperties = true)
    public enum InputType
    {
        CLICKTYPES("C"),
        MATERIALS("M"),
        TIMEDELTAS("T"),
        XDISTANCE("X"),
        YDISTANCE("Y");

        @Getter
        private final String keyString;

        /**
         * This parses all InputTypes from an argument.
         *
         * @param argument the {@link String} containing all the key-{@link String}s of {@link InputType}s.
         */
        public InputType[] parseInputTypesFromArgument(final String argument)
        {
            final List<InputType> inputTypes = new ArrayList<>();
            for (InputType inputType : InputType.values())
            {
                if (argument.contains(inputType.keyString))
                {
                    inputTypes.add(inputType);
                }
            }
            return inputTypes.toArray(new InputType[0]);
        }
    }
}

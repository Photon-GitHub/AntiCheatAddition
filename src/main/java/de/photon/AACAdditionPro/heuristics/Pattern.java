package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.neural.DataSet;
import de.photon.AACAdditionPro.neural.Output;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor(suppressConstructorProperties = true)
public abstract class Pattern
{
    public static final byte PATTERN_VERSION = 2;
    public static Output[] LEGIT_OUTPUT = new Output[]{
            new Output("cheating", 0),
            new Output("vanilla", 1)
    };

    @Getter
    @Setter
    private String name;
    @Getter
    private Input[] inputs;

    /**
     * This updates a certain {@link Input}.
     * If the desired {@link Input} is not present in this {@link Pattern} it will just be ignored.
     *
     * @param input the {@link Input} with the new data.
     */
    private void setInputData(final Input input)
    {
        for (Input internalInput : this.inputs)
        {
            if (internalInput.sameType(input))
            {
                internalInput.data = input.data;
            }
        }
    }

    /**
     * The actual analysis of the {@link Pattern} happens here.
     * The {@link DataSet} inputs are sorted alphabetically, so that it correlates with the {@link de.photon.AACAdditionPro.heuristics.Input.InputType} enum.
     */
    public abstract Output[] analyse(final DataSet dataSet);

    /**
     * Additional weight multiplicand to make sure a close-to-zero {@link de.photon.AACAdditionPro.oldheuristics.Pattern} has more impact than another.
     */
    public abstract double getWeight();
}

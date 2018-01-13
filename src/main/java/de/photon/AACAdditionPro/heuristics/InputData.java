package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;

public class InputData extends Data implements Serializable
{
    public static final transient HashMap<Character, InputData> VALID_INPUTS;

    static
    {
        // 7 Input values which will not change.
        VALID_INPUTS = new HashMap<>(7, 1);
        VALID_INPUTS.put('T', new InputData("TIMEDELTAS"));
        VALID_INPUTS.put('M', new InputData("MATERIALS"));
        VALID_INPUTS.put('X', new InputData("XDISTANCE"));
        VALID_INPUTS.put('Y', new InputData("YDISTANCE"));
        VALID_INPUTS.put('I', new InputData("INVENTORYTYPES"));
        VALID_INPUTS.put('S', new InputData("SLOTTYPES"));
        VALID_INPUTS.put('C', new InputData("CLICKTYPES"));
    }

    @Getter
    private double[] data;

    public InputData(String name)
    {
        super(name);
    }

    public InputData setData(double[] data)
    {
        this.data = data;
        return this;
    }
}

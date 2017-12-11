package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

public class InputData extends Data
{
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

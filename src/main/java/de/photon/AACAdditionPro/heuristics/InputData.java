package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

import java.io.Serializable;

public class InputData extends Data implements Serializable
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

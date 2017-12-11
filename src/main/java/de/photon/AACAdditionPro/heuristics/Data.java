package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

import java.io.Serializable;

public abstract class Data implements Serializable
{
    @Getter
    private String name;

    public Data(String name)
    {
        this.name = name;
    }
}

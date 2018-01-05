package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

import java.io.Serializable;

abstract class Data implements Serializable
{
    @Getter
    private String name;

    Data(String name)
    {
        this.name = name;
    }
}

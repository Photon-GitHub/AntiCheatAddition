package de.photon.AACAdditionPro.oldheuristics;

import lombok.Getter;

import java.io.Serializable;

@Deprecated
abstract class Data implements Serializable
{
    @Getter
    private String name;

    Data(String name)
    {
        this.name = name;
    }
}

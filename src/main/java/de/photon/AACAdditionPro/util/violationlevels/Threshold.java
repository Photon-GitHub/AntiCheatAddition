package de.photon.AACAdditionPro.util.violationlevels;

import lombok.Getter;

import java.util.List;

public class Threshold implements Comparable<Threshold>
{
    @Getter
    private final int vl;
    @Getter
    private final List<String> commandList;

    public Threshold(int vl, List<String> commandList)
    {
        this.vl = vl;
        this.commandList = commandList;
    }

    @Override
    public int compareTo(Threshold o)
    {
        return Integer.compare(vl, o.vl);
    }
}

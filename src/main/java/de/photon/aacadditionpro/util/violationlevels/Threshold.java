package de.photon.aacadditionpro.util.violationlevels;

import lombok.Getter;

import java.util.List;

class Threshold implements Comparable<Threshold>
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
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Threshold threshold = (Threshold) o;
        return vl == threshold.vl;
    }

    @Override
    public int hashCode()
    {
        return vl;
    }

    @Override
    public int compareTo(Threshold o)
    {
        return Integer.compare(vl, o.vl);
    }
}

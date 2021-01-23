package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.util.execute.ExecuteUtil;
import de.photon.aacadditionpro.util.execute.Placeholders;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

class Threshold implements Comparable<Threshold>
{
    @Getter
    private final int vl;
    private final List<String> commandList;

    public Threshold(int vl, List<String> commandList)
    {
        this.vl = vl;
        this.commandList = commandList;
    }

    /**
     * This executes the commands of this {@link Threshold}.
     */
    public void executeCommandList(Collection<Player> players)
    {
        for (String rawCommand : this.commandList) {
            ExecuteUtil.executeCommand(Placeholders.replacePlaceholders(rawCommand, players));
        }
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

package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.util.execute.ExecuteUtil;
import de.photon.aacadditionpro.util.execute.Placeholders;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode(doNotUseGetters = true, onlyExplicitlyIncluded = true)
class Threshold implements Comparable<Threshold>
{
    @Getter @EqualsAndHashCode.Include private final int vl;
    private final List<String> commandList;

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
    public int compareTo(Threshold o)
    {
        return Integer.compare(vl, o.vl);
    }
}

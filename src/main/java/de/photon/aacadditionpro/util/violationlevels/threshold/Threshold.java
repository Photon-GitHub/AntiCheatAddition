package de.photon.aacadditionpro.util.violationlevels.threshold;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.util.execute.ExecuteUtil;
import de.photon.aacadditionpro.util.execute.Placeholders;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@EqualsAndHashCode(doNotUseGetters = true, onlyExplicitlyIncluded = true)
public class Threshold implements Comparable<Threshold>
{
    @Getter @EqualsAndHashCode.Include private final int vl;
    @NotNull private final List<String> commandList;

    public Threshold(int vl, List<String> commandList)
    {
        Preconditions.checkNotNull(commandList, "Tried to define Threshold with null commands.");
        Preconditions.checkArgument(vl > 0, "Tried to define Threshold with vl smaller or equal to 0.");
        Preconditions.checkArgument(!commandList.isEmpty(), "Tried to define Threshold without commands.");

        this.vl = vl;
        this.commandList = ImmutableList.copyOf(commandList);
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
    public int compareTo(Threshold o)
    {
        return Integer.compare(vl, o.vl);
    }
}

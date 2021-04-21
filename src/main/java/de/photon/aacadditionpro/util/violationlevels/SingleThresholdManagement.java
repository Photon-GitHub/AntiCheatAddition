package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.util.execute.ExecuteUtil;
import de.photon.aacadditionpro.util.execute.Placeholders;
import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SingleThresholdManagement implements ThresholdManagement
{
    private final List<String> commands;

    public SingleThresholdManagement(List<String> thresholds)
    {
        commands = ImmutableList.copyOf(thresholds);
    }

    /**
     * Tries to load all thresholds from the given config key.
     *
     * @param thresholdSectionPath the given path to the section that contains the thresholds
     *
     * @return a mutable {@link List} containing all {@link Threshold}s.
     */
    public static SingleThresholdManagement loadThresholds(final String thresholdSectionPath)
    {
        // Make sure that the keys exist.
        return new SingleThresholdManagement(Objects.requireNonNull(ConfigUtils.loadImmutableStringOrStringList(thresholdSectionPath), "Severe loading error: Could not load command list from: " + thresholdSectionPath));
    }

    @Override
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        for (String rawCommand : this.commands) {
            ExecuteUtil.executeCommand(Placeholders.replacePlaceholders(rawCommand, players));
        }
    }
}

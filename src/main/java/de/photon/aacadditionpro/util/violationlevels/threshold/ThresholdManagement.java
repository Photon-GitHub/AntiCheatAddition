package de.photon.aacadditionpro.util.violationlevels.threshold;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface ThresholdManagement
{
    /**
     * Empty {@link ThresholdManagement} that doesn't have any {@link Threshold}s.
     */
    ThresholdManagement EMPTY = (fromVl, toVl, players) -> {};

    static ThresholdManagement loadThresholds(String configPath)
    {
        Preconditions.checkNotNull(configPath, "Tried to load null config path.");
        val keys = Preconditions.checkNotNull(ConfigUtils.loadKeys(configPath), "Config loading error: The keys loaded from " + configPath + " are null.");
        val thresholds = keys.stream().map(key -> new Threshold(Integer.parseInt(key), ConfigUtils.loadImmutableStringOrStringList(configPath + '.' + key))).collect(Collectors.toList());

        switch (thresholds.size()) {
            case 0:
                return EMPTY;
            case 1:
                return new SingleThresholds(thresholds.get(0));
            default:
                return new MultiThresholds(thresholds);
        }
    }

    static ThresholdManagement loadCommands(String configPath)
    {
        Preconditions.checkNotNull(configPath, "Tried to load null config path.");
        final List<String> commands = Preconditions.checkNotNull(ConfigUtils.loadImmutableStringOrStringList(configPath), "Config loading error: The commands at " + configPath + " could not be loaded.");
        return commands.isEmpty() ? EMPTY : new SingleThresholds(new Threshold(1, commands));
    }

    /**
     * Used to execute the commands of the {@link Threshold}s in this  {@link MultiThresholds}.
     */
    void executeThresholds(int fromVl, int toVl, Collection<Player> players);
}

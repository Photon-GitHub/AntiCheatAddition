package de.photon.aacadditionpro.util.violationlevels.threshold;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Collectors;

public interface ThresholdManagement
{
    static ThresholdManagement loadThresholds(String configPath)
    {
        Preconditions.checkNotNull(configPath, "Tried to load null config path.");
        val keys = Preconditions.checkNotNull(ConfigUtils.loadKeys(configPath), "Config loading error: The keys loaded from " + configPath + " are null.");
        val thresholds = keys.stream().map(key -> new Threshold(Integer.parseInt(key), ConfigUtils.loadImmutableStringOrStringList(configPath + '.' + key))).collect(Collectors.toList());

        switch (thresholds.size()) {
            case 0:
                return new EmptyThresholds();
            case 1:
                return new SingleThresholds(thresholds.get(0));
            default:
                return new MultiThresholds(thresholds);
        }
    }

    static ThresholdManagement loadCommands(String configPath)
    {
        Preconditions.checkNotNull(configPath, "Tried to load null config path.");
        val commands = Preconditions.checkNotNull(ConfigUtils.loadImmutableStringOrStringList(configPath), "Config loading error: The commands at " + configPath + " could not be loaded.");
        return commands.isEmpty() ? new EmptyThresholds() : new SingleThresholds(new Threshold(1, commands));
    }

    /**
     * Used to execute the commands of the {@link Threshold}s in this  {@link MultiThresholds}.
     */
    void executeThresholds(int fromVl, int toVl, Collection<Player> players);
}

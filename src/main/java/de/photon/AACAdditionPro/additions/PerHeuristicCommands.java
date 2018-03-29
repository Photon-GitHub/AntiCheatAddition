package de.photon.AACAdditionPro.additions;

import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.events.HeuristicsAdditionViolationEvent;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import de.photon.AACAdditionPro.util.files.ExternalConfigUtils;
import me.konsolas.aac.api.HackType;
import me.konsolas.aac.api.PlayerViolationEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerHeuristicCommands implements Module, Listener
{
    private static final Pattern HEURISTICS_PATTERN = Pattern.compile("P/(\\d{2})(\\S*)");
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("confidence (\\d{2}(\\.\\d+)?)");

    private final Map<UUID, Double> oldConfidences = new HashMap<>();

    /**
     * The map of the command that are defined in the config at certain violation-levels.
     */
    private ConcurrentMap<Integer, List<String>> thresholds;

    @Override
    public void subEnable()
    {
        // Load the thresholds
        thresholds = ConfigUtils.loadThresholds(this.getConfigString() + ".confidences");
        // Set AAC's min_confidence
        thresholds.keySet().stream().min(Integer::compareTo).ifPresent(
                minConfidence -> ExternalConfigUtils.requestConfigChange(ExternalConfigUtils.ExternalConfigs.AAC, new ExternalConfigUtils.RequestedConfigChange("heuristics.min_confidence", minConfidence)));
        ExternalConfigUtils.requestConfigChange(ExternalConfigUtils.ExternalConfigs.AAC, new ExternalConfigUtils.RequestedConfigChange("heuristics.thresholds", Collections.EMPTY_LIST));
    }

    @EventHandler(ignoreCancelled = true)
    public void on(final PlayerViolationEvent event)
    {
        // Only Heuristics-Events are listened for.
        if (event.getHackType() == HackType.HEURISTICS)
        {
            final Matcher patternMatcher = HEURISTICS_PATTERN.matcher(event.getMessage());
            final Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(event.getMessage());

            // Both matchers have found something
            if (patternMatcher.find() && confidenceMatcher.find())
            {
                // Directly remove all whitespaces from the matches.
                final String pattern = patternMatcher.group().trim();
                final double confidence = Double.parseDouble(confidenceMatcher.group(1).trim());

                // Heuristics-Event + Cancellation
                final HeuristicsAdditionViolationEvent additionEvent = new HeuristicsAdditionViolationEvent(event.getPlayer(), confidence, pattern);
                Bukkit.getPluginManager().callEvent(additionEvent);

                if (!additionEvent.isCancelled())
                {
                    // Full verbose
                    VerboseSender.sendVerboseMessage("Heuristics-Addon-Report | Player: " + event.getPlayer().getName() + " | Pattern: " + pattern + " | Confidence: " + confidence);

                    // Commands
                    executeHeuristicsCommands(confidence, event.getPlayer());

                    // Confidence - Management
                    oldConfidences.put(event.getPlayer().getUniqueId(), confidence);
                }
            }
        }
    }

    private void executeHeuristicsCommands(final double confidence, final Player player)
    {
        final double oldConfidence = oldConfidences.getOrDefault(player.getUniqueId(), 0D);

        try
        {
            thresholds.forEach(
                    (confidenceKey, commandsOfThreshold) ->
                    {
                        if (confidenceKey > oldConfidence && confidenceKey <= confidence)
                        {
                            for (final String command : commandsOfThreshold)
                            {
                                // Command cannot be null as of the new loading process.
                                // Sync command execution
                                CommandUtils.executeCommandWithPlaceholders(command, player, this.getModuleType(), confidence);
                            }
                        }
                    });
        } catch (final RuntimeException e)
        {
            VerboseSender.sendVerboseMessage("AACAdditionPro failed to execute the Heuristics-Addition commands. Are they formatted correctly?", true, true);
            e.printStackTrace();
        }
    }

    @EventHandler
    public void on(final PlayerQuitEvent e)
    {
        oldConfidences.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PER_HEURISTIC_COMMANDS;
    }
}
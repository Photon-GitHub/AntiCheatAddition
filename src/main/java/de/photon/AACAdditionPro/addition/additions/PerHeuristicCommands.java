package de.photon.AACAdditionPro.addition.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.addition.Addition;
import de.photon.AACAdditionPro.events.HeuristicsAdditionViolationEvent;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import me.konsolas.aac.api.HackType;
import me.konsolas.aac.api.PlayerViolationEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerHeuristicCommands implements Listener, Addition
{
    private static final String confidences = "Heuristic-Addition.confidences";

    private static final Pattern HEURISTICS_PATTERN = Pattern.compile("P/(\\d{2})");
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("confidence (\\d{2}(\\.\\d+)?)");

    private final HashMap<UUID, Double> oldConfidences = new HashMap<>();
    /**
     * The map of the command that are defined in the config at certain violation-levels.
     */
    private Map<Integer, List<String>> thresholds;

    @Override
    public void subEnable()
    {
        // Load the thresholds
        thresholds = ConfigUtils.loadThresholds(confidences);
    }

    @EventHandler
    public void on(final PlayerViolationEvent event)
    {
        // Only Heuristics-Events are listened for.
        if (event.getHackType() == HackType.HEURISTICS) {
            final Matcher patternMatcher = HEURISTICS_PATTERN.matcher(event.getMessage());
            final Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(event.getMessage());

            if (patternMatcher.find() && confidenceMatcher.find()) {
                final String pattern = patternMatcher.group(1).trim();
                final Double confidence = Double.parseDouble(confidenceMatcher.group(1).trim());

                // System.out.println("Pattern: " + pattern);
                // System.out.println("Confidence: " + confidence);

                // Heuristics-Event
                AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(new HeuristicsAdditionViolationEvent(event.getPlayer(), confidence, pattern));

                // Commands
                executeHeuristicsCommands(confidence, event.getPlayer());

                // Confidence - Management
                oldConfidences.put(event.getPlayer().getUniqueId(), confidence);
            }
        }
    }

    private void executeHeuristicsCommands(final double confidence, final Player player)
    {
        final double oldConfidence = oldConfidences.getOrDefault(player.getUniqueId(), 0D);

        try {
            for (final Map.Entry<Integer, List<String>> entry : thresholds.entrySet()) {
                if (entry.getKey() > oldConfidence && entry.getKey() <= confidence) {
                    for (final String command : entry.getValue()) {

                        if (command == null) {
                            throw new RuntimeException("Heuristics-Command is null.");
                        }

                        // Sync command execution
                        CommandUtils.executeCommandWithPlaceholders(player, command);
                    }
                }
            }
        } catch (final RuntimeException e) {
            System.err.println("AACAdditionPro failed to execute the Heuristics-Addition commands. Are they formatted correctly?");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void on(final PlayerQuitEvent e)
    {
        oldConfidences.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public String getConfigString()
    {
        return "Heuristic-Addition";
    }
}
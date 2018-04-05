package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.patterns.HC00000001;
import de.photon.AACAdditionPro.neural.Output;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.datawrappers.InventoryClick;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryHeuristics implements Listener, ViolationModule
{
    final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), -1);

    private double detection_confidence = AACAdditionPro.getInstance().getConfig().getDouble(this.getConfigString() + ".detection_confidence") / 100;

    // Concurrency as heuristics are potentially added concurrently.
    @Getter
    private static final Set<Pattern> PATTERNS;

    static
    {
        PATTERNS = ConcurrentHashMap.newKeySet();
        PATTERNS.addAll(NeuralPattern.loadPatterns());

        // Hardcoded patterns
        PATTERNS.add(new HC00000001());

        if (PATTERNS.isEmpty())
        {
            VerboseSender.sendVerboseMessage("InventoryHeuristics: No pattern has been loaded.", true, true);
        }
        else
        {
            PATTERNS.forEach(pattern -> VerboseSender.sendVerboseMessage("InventoryHeuristics: Loaded pattern " + pattern.getName() + "."));
        }
    }

    @EventHandler
    public void on(InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user) ||
            // Gamemode bypasses
            user.getPlayer().getGameMode() == GameMode.CREATIVE ||
            user.getPlayer().getGameMode() == GameMode.SPECTATOR)
        {
            return;
        }

        final double[] slotLocation = InventoryUtils.locateSlot(event.getRawSlot(), event.getWhoClicked().getOpenInventory().getTopInventory().getType());

        // Guarantees that there are no gaps in the Buffer.
        if (slotLocation == null)
        {
            return;
        }

        if (user.getInventoryHeuristicsData().bufferClick(new InventoryClick(slotLocation, event.getClick())))
        {
            // -1 because the first entry in the buffer doesn't create data.
            //
            // [0] = xDistance
            // [1] = yDistance
            // [2] = Time deltas
            // [3] = ClickTypes
            final double[][] inputMatrix = new double[4][user.getInventoryHeuristicsData().inventoryClicks.size() - 1];

            int index = user.getInventoryHeuristicsData().inventoryClicks.size() - 1;
            InventoryClick.BetweenClickInformation current;
            while (!user.getInventoryHeuristicsData().inventoryClicks.isEmpty())
            {
                current = user.getInventoryHeuristicsData().inventoryClicks.pop();
                inputMatrix[0][index] = current.xDistance;
                inputMatrix[1][index] = current.yDistance;

                // Timestamps
                // Decrease by approximately the factor 1 million to have more exact millis again.
                inputMatrix[2][index] = current.timeDelta;

                // ClickTypes
                inputMatrix[3][index] = current.clickType.ordinal();
                index--;
            }

            final String label = user.getInventoryHeuristicsData().trainingLabel;

            // Training
            for (Pattern pattern : PATTERNS)
            {
                final Output cheatingOutput = Arrays.stream(pattern.evaluateOrTrain(pattern.generateDataset(inputMatrix, label)))
                                                    .filter(output -> output.getLabel().equals("cheating"))
                                                    .findAny()
                                                    .orElseThrow(() -> new NeuralNetworkException("Could not find cheating output for pattern " + pattern.getName()));

                if (cheatingOutput.getConfidence() > detection_confidence)
                {
                    final InventoryHeuristicsEvent inventoryHeuristicsEvent = new InventoryHeuristicsEvent(user.getPlayer(), pattern.getName(), cheatingOutput.getConfidence());
                    Bukkit.getPluginManager().callEvent(inventoryHeuristicsEvent);

                    if (!inventoryHeuristicsEvent.isCancelled())
                    {
                        user.getInventoryHeuristicsData().setPatternConfidence(pattern.getName(), cheatingOutput.getConfidence());
                        VerboseSender.sendVerboseMessage("Player " + user.getPlayer().getName() + " has been detected by pattern " + pattern.getName() + " with a confidence of " + cheatingOutput.getConfidence());
                    }
                }
            }

            final double globalConfidence = user.getInventoryHeuristicsData().calculateGlobalConfidence();
            if (globalConfidence != 0)
            {
                // Might not be the case, i.e. no detections
                vlManager.setVL(user.getPlayer(), (int) (globalConfidence * 100));
            }
        }
    }

    /**
     * Gets a {@link NeuralPattern} by its name.
     *
     * @return the {@link NeuralPattern} which has the name equal to the provided {@link String} or null if no pattern was found.
     */
    public static Pattern getPatternByName(final String patternName)
    {
        for (Pattern pattern : PATTERNS)
        {
            if (pattern.getName().equals(patternName))
            {
                return pattern;
            }
        }
        return null;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY_HEURISTICS;
    }
}

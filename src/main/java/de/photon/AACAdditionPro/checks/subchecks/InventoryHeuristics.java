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
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InventoryHeuristics implements Listener, ViolationModule
{
    // Concurrency as heuristics are potentially added concurrently.
    public static final ConcurrentMap<String, Pattern> PATTERNS;
    private static final Map<String, Integer> INPUT_MAPPING;
    public static final int SAMPLES = 20;

    final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), -1);

    private double detection_confidence = AACAdditionPro.getInstance().getConfig().getDouble(this.getConfigString() + ".detection_confidence") / 100;

    static
    {
        // Input handling
        INPUT_MAPPING = new HashMap<>();
        INPUT_MAPPING.put("X", 0);
        INPUT_MAPPING.put("Y", 1);
        INPUT_MAPPING.put("T", 2);
        INPUT_MAPPING.put("C", 3);

        PATTERNS = new ConcurrentHashMap<>();
        // Directly load NeuralPatterns
        final Set<Pattern> patternsToAdd = new HashSet<>(NeuralPattern.loadPatterns());
        // Hardcoded patterns
        patternsToAdd.add(new HC00000001());

        if (patternsToAdd.isEmpty())
        {
            VerboseSender.sendVerboseMessage("InventoryHeuristics: No pattern has been loaded.", true, true);
        }
        else
        {
            patternsToAdd.forEach(
                    pattern ->
                    {
                        PATTERNS.put(pattern.getName(), pattern);
                        VerboseSender.sendVerboseMessage("InventoryHeuristics: Loaded pattern " + pattern.getName() + ".");
                    });
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
            // [0] = xDistance
            // [1] = yDistance
            // [2] = Time deltas
            // [3] = ClickTypes
            final double[][] inputMatrix = new double[4][user.getInventoryHeuristicsData().inventoryClicks.size()];

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

            // Training
            for (Pattern pattern : PATTERNS.values())
            {
                final Output cheatingOutput = Arrays.stream(pattern.evaluateOrTrain(pattern.generateDataset(inputMatrix, user.getInventoryHeuristicsData().trainingLabel)))
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

    public static int[] parseInputs(final String inputString)
    {
        final List<Integer> inputs = new ArrayList<>();
        INPUT_MAPPING.forEach((keyString, inputIndex) -> {
            if (inputString.contains(keyString))
            {
                inputs.add(inputIndex);
            }
        });

        int[] resultingInputs = new int[inputs.size()];
        for (int i = 0; i < inputs.size(); i++)
        {
            resultingInputs[i] = inputs.get(i);
        }
        return resultingInputs;
    }
}

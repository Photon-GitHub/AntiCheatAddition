package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.NeuralPattern;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.PatternDeserializer;
import de.photon.AACAdditionPro.heuristics.TrainingData;
import de.photon.AACAdditionPro.heuristics.patterns.HC00000001;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.storage.datawrappers.InventoryClick;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
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
        PatternDeserializer.loadPatterns(PATTERNS);

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
        if (User.isUserInvalid(user))
        {
            return;
        }

        if (user.getInventoryData().inventoryClicks.bufferObject(new InventoryClick(
                // The current item might be null and causes NPEs when .getType() is invoked.
                event.getCurrentItem() == null ? Material.AIR : event.getCurrentItem().getType(),

                event.getRawSlot(),

                event.getWhoClicked().getOpenInventory().getTopInventory().getType(),

                event.getSlotType(),

                event.getClick())))
        {
            // Create the new map and add the standard inputs
            final Map<Character, InputData> inputData = new HashMap<>(InputData.VALID_INPUTS);
            // Set the data array length
            inputData.values().forEach(anInputData -> anInputData.setData(new double[user.getInventoryData().inventoryClicks.size()]));

            final int[] i = {0};
            user.getInventoryData().inventoryClicks.clearLastTwoObjectsIteration((youngerClick, olderClick) -> {
                // Slot distance
                // Must be done first as of the continue!
                double[] locationOfYoungerClick = InventoryUtils.locateSlot(youngerClick.clickedRawSlot, youngerClick.inventoryType);
                double[] locationOfOlderClick = InventoryUtils.locateSlot(olderClick.clickedRawSlot, olderClick.inventoryType);

                if (locationOfOlderClick == null || locationOfYoungerClick == null)
                {
                    inputData.get('X').getData()[i[0]] = Double.MIN_VALUE;
                    inputData.get('Y').getData()[i[0]] = Double.MIN_VALUE;
                }
                else
                {
                    inputData.get('X').getData()[i[0]] = locationOfYoungerClick[0] - locationOfOlderClick[0];
                    inputData.get('Y').getData()[i[0]] = locationOfYoungerClick[1] - locationOfOlderClick[1];
                }

                // Timestamps
                // Decrease by approximately the factor 1 million to have more exact millis again.
                inputData.get('T').getData()[i[0]] = 60 / Math.min(1E-10, (youngerClick.timeStamp - olderClick.timeStamp) >> 2);

                // Materials
                inputData.get('M').getData()[i[0]] = youngerClick.type.ordinal();

                inputData.get('I').getData()[i[0]] = youngerClick.inventoryType.ordinal();

                // SlotTypes
                inputData.get('S').getData()[i[0]] = youngerClick.slotType.ordinal();

                // ClickTypes
                inputData.get('C').getData()[i[0]++] = youngerClick.clickType.ordinal();
            });

            final Map<Pattern, Double> outputDataMap = new HashMap<>(PATTERNS.size(), 1);

            // Training
            for (Pattern pattern : PATTERNS)
            {
                boolean training = false;
                if (pattern instanceof NeuralPattern)
                {
                    final NeuralPattern neuralPattern = (NeuralPattern) pattern;

                    for (TrainingData trainingData : neuralPattern.getTrainingDataSet())
                    {
                        if (trainingData.getUuid().equals(user.getPlayer().getUniqueId()))
                        {
                            training = true;
                            neuralPattern.pushInputData(trainingData.getOutputDataName(), inputData);
                            break;
                        }
                    }
                }

                // No analysis when training.
                if (!training)
                {
                    Double result = pattern.analyse(inputData);

                    if (result != null)
                    {
                        outputDataMap.put(pattern, result);
                    }
                }
            }

            user.getInventoryHeuristicsData().decayCycle();
            for (Map.Entry<Pattern, Double> entry : outputDataMap.entrySet())
            {
                // Pattern testing
                double value = entry.getValue();
                //TODO: THIS IS ONLY A WORKAROUND FOR THE 0.5 PROBLEM!!!
                if (value >= 0.5)
                {
                    value -= 0.5;
                }
                value *= 2;

                if (value > detection_confidence)
                {
                    final InventoryHeuristicsEvent inventoryHeuristicsEvent = new InventoryHeuristicsEvent(user.getPlayer(), entry.getKey().getName(), entry.getValue());
                    Bukkit.getPluginManager().callEvent(inventoryHeuristicsEvent);

                    if (!inventoryHeuristicsEvent.isCancelled())
                    {
                        user.getInventoryHeuristicsData().setPatternConfidence(entry.getKey().getName(), value);
                        VerboseSender.sendVerboseMessage("Player " + user.getPlayer().getName() + " has been detected by pattern " + entry.getKey().getName() + " with a confidence of " + value + " (Original: " + entry.getValue() + ")");
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

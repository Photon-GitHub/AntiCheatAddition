package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.PatternLoader;
import de.photon.AACAdditionPro.heuristics.TrainingData;
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
    private static final Set<Pattern> PATTERNS = ConcurrentHashMap.newKeySet();

    @Override
    public void subEnable()
    {
        new PatternLoader(PATTERNS);
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
            final InputData[] inputData = new InputData[InputData.VALID_INPUTS.size()];

            // You cannot loop here without inconsistencies.
            inputData[0] = new InputData(InputData.VALID_INPUTS.get("T").getName());
            inputData[1] = new InputData(InputData.VALID_INPUTS.get("M").getName());
            inputData[2] = new InputData(InputData.VALID_INPUTS.get("X").getName());
            inputData[3] = new InputData(InputData.VALID_INPUTS.get("Y").getName());
            inputData[4] = new InputData(InputData.VALID_INPUTS.get("I").getName());
            inputData[5] = new InputData(InputData.VALID_INPUTS.get("S").getName());
            inputData[6] = new InputData(InputData.VALID_INPUTS.get("C").getName());

            for (InputData anInputData : inputData)
            {
                anInputData.setData(new double[user.getInventoryData().inventoryClicks.size()]);
            }

            final int[] i = {0};
            user.getInventoryData().inventoryClicks.clearLastTwoObjectsIteration((youngerClick, olderClick) -> {
                // Slot distance
                // Must be done first as of the continue!
                double[] locationOfYoungerClick = InventoryUtils.locateSlot(youngerClick.clickedRawSlot, youngerClick.inventoryType);
                double[] locationOfOlderClick = InventoryUtils.locateSlot(olderClick.clickedRawSlot, olderClick.inventoryType);

                if (locationOfOlderClick == null || locationOfYoungerClick == null)
                {
                    inputData[2].getData()[i[0]] = Double.MIN_VALUE;
                    inputData[3].getData()[i[0]] = Double.MIN_VALUE;
                }
                else
                {
                    inputData[2].getData()[i[0]] = locationOfYoungerClick[0] - locationOfOlderClick[0];
                    inputData[3].getData()[i[0]] = locationOfYoungerClick[1] - locationOfOlderClick[1];
                }

                // Timestamps
                // Decrease by approximately the factor 1 million to have more exact millis again.
                inputData[0].getData()[i[0]] = 60 / Math.min(1E-10, (youngerClick.timeStamp - olderClick.timeStamp) >> 20);

                // Materials
                inputData[1].getData()[i[0]] = youngerClick.type.ordinal();

                // SlotTypes
                inputData[4].getData()[i[0]] = youngerClick.slotType.ordinal();

                // ClickTypes
                inputData[5].getData()[i[0]++] = youngerClick.clickType.ordinal();
            });

            final Map<Pattern, Double> outputDataMap = new HashMap<>(PATTERNS.size(), 1);

            for (Pattern pattern : PATTERNS)
            {
                boolean training = false;
                for (TrainingData trainingData : pattern.getTrainingDataSet())
                {
                    if (trainingData.getUuid().equals(user.getPlayer().getUniqueId()))
                    {
                        training = true;
                        pattern.pushInputData(trainingData.getOutputDataName(), inputData);
                        break;
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

            double flagSum = 0;
            for (Map.Entry<Pattern, Double> entry : outputDataMap.entrySet())
            {
                if (entry.getValue() > detection_confidence)
                {
                    final InventoryHeuristicsEvent inventoryHeuristicsEvent = new InventoryHeuristicsEvent(user.getPlayer(), entry.getKey().getName(), entry.getValue());
                    Bukkit.getPluginManager().callEvent(inventoryHeuristicsEvent);

                    if (!inventoryHeuristicsEvent.isCancelled())
                    {
                        double value = entry.getValue();
                        //TODO: THIS IS ONLY A WORKAROUND FOR THE 0.5 PROBLEM!!!
                        if (value >= 0.5)
                        {
                            value -= 0.5;
                        }
                        value *= 2;

                        flagSum += value;
                        VerboseSender.sendVerboseMessage("Player " + user.getPlayer().getName() + " has been detected by pattern " + entry.getKey().getName() + " with a confidence of " + value + " (Original: " + entry.getValue() + ")");
                    }
                }
            }

            if (flagSum != 0)
            {
                final double vl = Math.abs(Math.tanh(flagSum - 0.15));
                // Might not be the case, i.e. no detections
                vlManager.setVL(user.getPlayer(), (int) vl);
            }
        }
    }

    /**
     * Gets a {@link Pattern} by its name.
     *
     * @return the {@link Pattern} which has the name equal to the provided {@link String} or null if no pattern was found.
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

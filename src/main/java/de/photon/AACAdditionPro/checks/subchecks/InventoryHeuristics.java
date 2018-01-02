package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.heuristics.TrainingData;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.FileUtilities;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
        final File heuristicsFolder = new File(FileUtilities.AACADDITIONPRO_DATA_FOLDER + "/heuristics");

        if (!heuristicsFolder.exists())
        {
            if (heuristicsFolder.mkdirs())
            {
                VerboseSender.sendVerboseMessage("InventoryHeuristics folder created.", true, false);
                VerboseSender.sendVerboseMessage("Please download the latest patterns from https://github.com/Photon-GitHub/AACAdditionPro/tree/master/patterns", true, false);
            }
            else
            {
                VerboseSender.sendVerboseMessage("Unable to load file create the heuristics-folder.", true, true);
            }
        }

        final File[] patternFiles = heuristicsFolder.listFiles();
        if (patternFiles != null)
        {
            for (final File file : patternFiles)
            {
                try
                {
                    final FileInputStream fileInputStream = new FileInputStream(file);
                    final ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                    PATTERNS.add((Pattern) objectInputStream.readObject());

                    objectInputStream.close();
                    fileInputStream.close();
                } catch (IOException | ClassNotFoundException e)
                {
                    VerboseSender.sendVerboseMessage("Unable to load file " + file.getPath() + " as a pattern.", true, true);
                    e.printStackTrace();
                }
            }
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
                inputData[0].getData()[i[0]] = youngerClick.timeStamp - olderClick.timeStamp;

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
                        pattern.getTrainingInputs().get(trainingData.getOutputDataName()).push(inputData);
                        break;
                    }
                }

                // No analysis when training.
                if (!training)
                {
                    final Double result = pattern.analyse(inputData);
                    if (result != null)
                    {
                        outputDataMap.put(pattern, result);
                    }
                }
            }

            double flagSum = 0;
            for (Map.Entry<Pattern, Double> entry : outputDataMap.entrySet())
            {
                System.out.println("Player: " + user.getPlayer() + " Pattern: " + entry.getKey().getName() + " Confidence: " + entry.getValue());
                if (entry.getValue() > detection_confidence)
                {
                    final InventoryHeuristicsEvent inventoryHeuristicsEvent = new InventoryHeuristicsEvent(user.getPlayer(), entry.getKey().getName(), entry.getValue());
                    Bukkit.getPluginManager().callEvent(inventoryHeuristicsEvent);

                    if (!inventoryHeuristicsEvent.isCancelled())
                    {
                        flagSum += entry.getValue();
                        VerboseSender.sendVerboseMessage("Player " + user.getPlayer().getName() + " has been detected by " + entry.getKey().getName() + " with a confidence of " + entry.getValue());
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

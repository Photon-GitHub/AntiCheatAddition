package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.OutputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.FileUtilities;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.storage.datawrappers.InventoryClick;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InventoryHeuristics implements Listener, ViolationModule
{
    /**
     * Registry of all pattern names in the heuristics folder of resources.
     */
    private static final Set<String> PATTERN_NAMES = new HashSet<>(Arrays.asList(
            "AAAA00290001.pattern",
            "AAAB00080001.pattern")
    );

    ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), -1);

    // Concurrency as heuristics are potentially added concurrently.
    @Getter
    private static final Set<Pattern> PATTERNS = ConcurrentHashMap.newKeySet();

    @Override
    public void subEnable()
    {
        final File heuristicsFolder = new File(FileUtilities.AACADDITIONPRO_DATA_FOLDER + "/heuristics");

        if (!heuristicsFolder.exists())
        {
            // Default heuristics
            PATTERN_NAMES.forEach(patternName -> {
                try
                {
                    FileUtilities.saveFileInFolder("heuristics/" + patternName);
                } catch (IOException e)
                {
                    VerboseSender.sendVerboseMessage("Unable to save pattern " + patternName + " as a pattern.");
                    e.printStackTrace();
                }
            });
        }

        final File[] patternFiles = heuristicsFolder.listFiles();
        if (patternFiles != null)
        {
            for (final File file : patternFiles)
            {
                try
                {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                    PATTERNS.add((Pattern) objectInputStream.readObject());

                    objectInputStream.close();
                    fileInputStream.close();
                } catch (IOException | ClassNotFoundException e)
                {
                    VerboseSender.sendVerboseMessage("Unable to load file " + file.getPath() + " as a pattern.");
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

                // The inventory might be null and causes NPEs when .getType() is invoked.
                event.getClickedInventory() == null ?
                event.getWhoClicked().getOpenInventory().getType() :
                event.getClickedInventory().getType(),

                event.getSlotType(),

                event.getClick())))
        {
            InputData[] inputData = new InputData[InputData.VALID_INPUTS.size()];

            int dataIndex = 0;
            for (InputData data : InputData.VALID_INPUTS.values())
            {
                inputData[dataIndex] = new InputData(data.getName());
                inputData[dataIndex++].setData(new double[user.getInventoryData().inventoryClicks.size()]);
            }

            InventoryClick[] lastAndCurrent = new InventoryClick[]{
                    user.getInventoryData().inventoryClicks.get(0),
                    user.getInventoryData().inventoryClicks.get(1)
            };

            // Start at 1 to make deltas happen.
            for (int i = 1; !user.getInventoryData().inventoryClicks.isEmpty(); i++)
            {
                lastAndCurrent[0] = lastAndCurrent[1];
                lastAndCurrent[1] = user.getInventoryData().inventoryClicks.remove(0);

                // Slot distance
                // Must be done first as of the continue!
                double[] locationOfLastClick = InventoryUtils.locateSlot(lastAndCurrent[0].clickedRawSlot, lastAndCurrent[0].inventoryType);
                double[] locationOfCurrentClick = InventoryUtils.locateSlot(lastAndCurrent[1].clickedRawSlot, lastAndCurrent[1].inventoryType);

                if (locationOfCurrentClick == null || locationOfLastClick == null)
                {
                    inputData[2].getData()[i - 1] = Double.MIN_VALUE;
                    inputData[3].getData()[i - 1] = Double.MIN_VALUE;
                }
                else
                {
                    inputData[2].getData()[i - 1] = locationOfLastClick[0] - locationOfCurrentClick[0];
                    inputData[3].getData()[i - 1] = locationOfLastClick[1] - locationOfCurrentClick[1];
                }

                // Timestamps
                inputData[0].getData()[i - 1] = lastAndCurrent[1].timeStamp - lastAndCurrent[0].timeStamp;

                // Materials
                inputData[1].getData()[i - 1] = lastAndCurrent[0].type.ordinal();

                // SlotTypes
                inputData[3].getData()[i - 1] = lastAndCurrent[0].slotType.ordinal();

                // ClickTypes
                inputData[4].getData()[i - 1] = lastAndCurrent[0].clickType.ordinal();
            }

            final Map<Pattern, OutputData> outputDataMap = new HashMap<>(PATTERNS.size());

            for (Pattern pattern : PATTERNS)
            {
                // Totally ok to do with the array as the provideInputData() method filters out the required information and ignores the rest.
                pattern.provideInputData(inputData);

                final OutputData result = pattern.analyse(user.getPlayer().getUniqueId());
                if (result != null)
                {
                    outputDataMap.put(pattern, result);
                }
            }

            //Debug
            outputDataMap.forEach((pattern, outputData) -> System.out.println("Pattern: " + pattern + " | OutputData: " + outputData));

            // Filter out all the VANILLA results
            final Set<Map.Entry<Pattern, OutputData>> flagEntrySet = outputDataMap.entrySet().stream().filter(entry -> !entry.getValue().getName().equals("VANILLA")).collect(Collectors.toSet());

            double flagSum = 0;
            for (Map.Entry<Pattern, OutputData> entry : flagEntrySet)
            {
                flagSum += entry.getValue().getConfidence();
                VerboseSender.sendVerboseMessage("Player " + user.getPlayer().getName() + " has been detected by " + entry.getKey().getName() + " with a confidence of " + entry.getValue().getConfidence());
            }

            final double vl = Math.abs(10 / (Math.tanh(flagSum) - 1.1));
            // Might not be the case, i.e. no detections
            vlManager.setVL(user.getPlayer(), (int) vl);
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
}

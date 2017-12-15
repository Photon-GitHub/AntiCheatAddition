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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryHeuristics implements Listener, ViolationModule
{
    ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    // Concurrency as patterns are potentially added concurrently.
    @Getter
    private static final Set<Pattern> PATTERNS = ConcurrentHashMap.newKeySet();

    @Override
    public void subEnable()
    {
        final File heuristicsFolder = new File(FileUtilities.AACADDITIONPRO_DATAFOLDER + "/heuristics");

        if (heuristicsFolder.exists())
        {
            final File[] patternFiles = heuristicsFolder.listFiles();
            if (patternFiles != null)
            {
                for (File file : patternFiles)
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

        if (user.getInventoryData().inventoryClicks.bufferObject(new InventoryClick(event.getCurrentItem().getType(), event.getRawSlot(), event.getClickedInventory().getType(), event.getSlotType(), event.getClick())))
        {
            InputData[] inputData = new InputData[]{
                    new InputData("TIMEDELTAS"),
                    new InputData("MATERIALS"),
                    new InputData("RAWSLOTS"),
                    new InputData("INVENTORYTYPES"),
                    new InputData("SLOTTYPES"),
                    new InputData("CLICKTYPES")
            };

            for (InputData data : inputData)
            {
                data.setData(new double[user.getInventoryData().inventoryClicks.size()]);
            }

            InventoryClick[] lastAndCurrent = new InventoryClick[]{
                    user.getInventoryData().inventoryClicks.get(0),
                    user.getInventoryData().inventoryClicks.get(1)
            };

            // Start at 1 to make deltas happen.
            int i = 1;
            while (i < user.getInventoryData().inventoryClicks.size())
            {
                // Timestamps
                inputData[0].getData()[i - 1] = lastAndCurrent[1].timeStamp - lastAndCurrent[0].timeStamp;
                // Materials
                inputData[1].getData()[i - 1] = lastAndCurrent[0].type.ordinal();
                // Slot distance
                inputData[2].getData()[i - 1] = InventoryUtils.vectorDistance(
                        Objects.requireNonNull(InventoryUtils.locateSlot(lastAndCurrent[0].clickedRawSlot, lastAndCurrent[0].inventoryType, lastAndCurrent[0].slotType)),
                        Objects.requireNonNull(InventoryUtils.locateSlot(lastAndCurrent[1].clickedRawSlot, lastAndCurrent[1].inventoryType, lastAndCurrent[1].slotType)));
                // SlotTypes
                inputData[3].getData()[i - 1] = lastAndCurrent[0].slotType.ordinal();
                // ClickTypes
                inputData[4].getData()[i - 1] = lastAndCurrent[0].clickType.ordinal();

                lastAndCurrent[0] = lastAndCurrent[1];
                lastAndCurrent[1] = user.getInventoryData().inventoryClicks.get(++i);
            }

            final List<OutputData> outputData = new ArrayList<>(PATTERNS.size());

            for (Pattern pattern : PATTERNS)
            {
                // Totally ok to do with the array as the provideInputData() method filters out the required information and ignores the rest.
                pattern.provideInputData(inputData, user.getPlayer().getUniqueId());

                OutputData result = pattern.analyse();
                if (result != null)
                {
                    outputData.add(result);
                }
            }

            // Get the highest confidence and flag:
            Optional<OutputData> maxConfidenceData = outputData.stream()
                                                               // Filter out all the VANILLA results
                                                               .filter(output -> !output.getName().equals("VANILLA"))
                                                               // get the max confidence for flagging
                                                               .max(Comparator.comparingDouble(OutputData::getConfidence));

            // Might not be the case, i.e. no detections
            maxConfidenceData.ifPresent(maxConfidenceOutput -> vlManager.flag(
                    user.getPlayer(),
                    // Use power 7 here to make sure higher confidences will flag significantly more.
                    (int) (100 * Math.pow(maxConfidenceOutput.getConfidence(), 7)),
                    -1,
                    () -> {},
                    () -> {}));
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

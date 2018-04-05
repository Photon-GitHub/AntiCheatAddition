package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;
import de.photon.AACAdditionPro.heuristics.Input;
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

import java.util.EnumMap;
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

        if (user.getInventoryData().inventoryClicks.bufferObject(new InventoryClick(slotLocation, event.getClick())))
        {
            // Create input map
            final EnumMap<Input.InputType, double[]> inputMap = new EnumMap<>(Input.InputType.class);
            for (Input.InputType inputType : Input.InputType.values())
            {
                inputMap.put(inputType, new double[user.getInventoryData().inventoryClicks.size() - 1]);
            }

            assert !user.getInventoryData().inventoryClicks.isEmpty();

            // Start at one so the
            InventoryClick olderClick = user.getInventoryData().inventoryClicks.get(0);
            InventoryClick youngerClick;
            for (int index = 1; index < user.getInventoryData().inventoryClicks.size(); index++)
            {
                youngerClick = user.getInventoryData().inventoryClicks.get(index);

                inputMap.get(Input.InputType.XDISTANCE)[index] = youngerClick.slotLocation[0] - olderClick.slotLocation[0];
                inputMap.get(Input.InputType.YDISTANCE)[index] = youngerClick.slotLocation[1] - olderClick.slotLocation[1];

                // Timestamps
                // Decrease by approximately the factor 1 million to have more exact millis again.
                inputMap.get(Input.InputType.TIMEDELTAS)[index] = 0.1 * (youngerClick.timeStamp - olderClick.timeStamp);

                // ClickTypes
                inputMap.get(Input.InputType.CLICKTYPES)[index] = (double) youngerClick.clickType.ordinal();

                olderClick = youngerClick;
            }

            // Deploy the inputs in the patterns.
            inputMap.forEach(((inputType, doubles) ->
            {
                for (Pattern pattern : PATTERNS)
                {
                    pattern.setInputData(new Input(inputType, doubles));
                }
            }));

            final Map<Pattern, Output[]> outputDataMap = new HashMap<>(PATTERNS.size(), 1);

            // Training
            for (Pattern pattern : PATTERNS)
            {
                // Training
                if (pattern instanceof NeuralPattern &&
                    user.getInventoryHeuristicsData().trainedPattern != null &&
                    user.getInventoryHeuristicsData().trainedPattern.getName().equals(pattern.getName()))
                {
                    ((NeuralPattern) pattern).train(user.getInventoryHeuristicsData().trainingLabel);
                    user.getInventoryHeuristicsData().trainingLabel = null;
                    user.getInventoryHeuristicsData().trainedPattern = null;
                }
                // Evaluation only
                else
                {
                    outputDataMap.put(pattern, pattern.evaluate());
                }
            }

            outputDataMap.forEach((pattern, outputs) -> {
                Output cheatingOutput = null;
                for (Output output : outputs)
                {
                    if (output.getLabel().equals("cheating"))
                    {
                        cheatingOutput = output;
                        break;
                    }
                }

                if (cheatingOutput == null)
                {
                    throw new NeuralNetworkException("Could not find cheating output for pattern " + pattern.getName());
                }

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
            });

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

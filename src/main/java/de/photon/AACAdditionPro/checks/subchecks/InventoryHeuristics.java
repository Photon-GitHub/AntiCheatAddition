package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.events.InventoryHeuristicsEvent;
import de.photon.AACAdditionPro.heuristics.ActivationFunction;
import de.photon.AACAdditionPro.heuristics.NeuralNetwork;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.storage.datawrappers.InventoryClick;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class InventoryHeuristics implements Listener, AACAdditionProCheck
{
    private final Stack<Long> deltaTimes = new Stack<>();
    private final Stack<Integer> startPositions = new Stack<>();
    private final Stack<Integer> targetPositions = new Stack<>();
    private final Stack<Integer> materials = new Stack<>();
    private NeuralNetwork neuralNetwork;
    private Player training;
    private Player checking;
    private InventoryClick lastClick;

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryClickEvent event)
    {
        if ((training != null && training.getPlayer().getUniqueId().equals(event.getWhoClicked().getUniqueId())) ||
            (checking != null && checking.getPlayer().getUniqueId().equals(event.getWhoClicked().getUniqueId())))
        {

            final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

            // Not bypassed
            if (AACAdditionProCheck.isUserInvalid(user)) {
                return;
            }

            if (lastClick == null) {
                lastClick = new InventoryClick(
                        user,
                        event.getCurrentItem() != null ?
                        event.getCurrentItem().getType() :
                        Material.AIR,
                        event.getRawSlot(),
                        event.getClick().ordinal()
                );
            } else {
                deltaTimes.push(System.currentTimeMillis() - lastClick.timeStamp);
                startPositions.push(event.getRawSlot());
                targetPositions.push(lastClick.position);
                materials.push(lastClick.type.ordinal());
                lastClick = null;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryHeuristicsEvent event)
    {
        if (event.isTraining()) {
            train(event.getPlayer(), event.getPattern());
        } else {
            check(event.getPlayer());
        }
    }

    private void train(final Player player, final String s)
    {
        if (checking != null || training != null) {
            VerboseSender.sendVerboseMessage("Already checking or training");
            return;
        }

        training = player;
        neuralNetwork.debugMatrix();
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                AACAdditionPro.getInstance(), () ->
                {
                    training = null;
                    while (!deltaTimes.isEmpty()) {
                        neuralNetwork.setInput("deltaTimes", Double.valueOf(deltaTimes.pop()));
                        neuralNetwork.setInput("startPositions", Double.valueOf(startPositions.pop()));
                        neuralNetwork.setInput("targetPositions", Double.valueOf(targetPositions.pop()));
                        neuralNetwork.setInput("materials", Double.valueOf(materials.pop()));
                        neuralNetwork.train(s);
                    }
                    VerboseSender.sendVerboseMessage("Training finished.");
                    neuralNetwork.debugMatrix();
                }, 200);
    }

    private void check(final Player player)
    {
        if (checking != null || training != null) {
            VerboseSender.sendVerboseMessage("Already checking or training");
            return;
        }

        checking = player;
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                AACAdditionPro.getInstance(), () ->
                {
                    final TreeMap<String, Double> patternSums = new TreeMap<>();
                    checking = null;
                    while (!deltaTimes.isEmpty())
                    {
                        neuralNetwork.setInput("deltaTimes", Double.valueOf(deltaTimes.pop()));
                        neuralNetwork.setInput("startPositions", Double.valueOf(startPositions.pop()));
                        neuralNetwork.setInput("targetPositions", Double.valueOf(targetPositions.pop()));
                        neuralNetwork.setInput("materials", Double.valueOf(materials.pop()));

                        final Map<String, Double> currentResult = neuralNetwork.check();
                        currentResult.forEach((name, value) -> patternSums.put(name, value + patternSums.getOrDefault(name, 0D)));
                    }

                    final Map<String, Double> resultMap = patternSums.descendingMap();

                    resultMap.forEach((name_of_pattern, value) -> VerboseSender.sendVerboseMessage(name_of_pattern + ": " + value));
                }, 200);
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.INVENTORY_HEURISTICS;
    }

    @Override
    public void subEnable()
    {
        neuralNetwork = new NeuralNetwork(
                ActivationFunction.TANGENS_HYPERBOLICUS,
                new String[]{
                        "deltaTimes",
                        "startPositions",
                        "targetPositions",
                        "materials"
                },
                new String[]{
                        "VANILLA",
                        "CER_AUTOARMOR"
                },
                10, 10);
    }
}

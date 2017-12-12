package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashSet;

public class InventoryHeuristics implements Listener, ViolationModule
{
    ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    @Getter
    private static final HashSet<Pattern> PATTERNS = new HashSet<>();

    private double getSlotDistance(int firstSlot, int secondSlot, InventoryType.SlotType firstType, InventoryType.SlotType secondType)
    {
        float[] firstCoords = new float[2];
        float[] secondCoords = new float[2];
        return Math.hypot(firstCoords[0] - secondCoords[0], firstCoords[1] - secondCoords[1]);
    }

    /**
     * @return the coords of a slot or null if it is invalid.
     */
    private float[] locateSlot(int slot, InventoryType.SlotType type)
    {


        // TODO: MANUAL TESTING WHAT THE SLOTNUMBERS ARE...
        switch (type)
        {
            // Y = 0
            case QUICKBAR:
                return new float[]{
                        slot % 9,
                        0
                };

            case CRAFTING:
                return new float[]
                        {
                                // 80 and 82
                                slot % 2 == 0 ?
                                5.5F :
                                6.5F,
                                // 82 and 83
                                slot > 81 ?
                                6.5F :
                                7.5F
                        };

            case RESULT:
                return new float[]{
                        7.5F,
                        7
                };

            case ARMOR:
                break;
            case CONTAINER:

                break;
            case FUEL:
                break;
            default:
                break;
        }

        // cases: OUTSIDE
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

    /*// TODO: DIFFERENT INVENTORY TYPES!
    // Lowest priority to get all the Events before they are processed.
    @EventHandler(priority = EventPriority.LOWEST)
    public void on(InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }
    }

        InventoryUtils.locateSlot(event.getRawSlot(),event.getInventory().

    getType());

    // Buffer the data in the user's InventoryData and check if there are enough entries
        if(user.getInventoryData().inventoryClicks.bufferObject(new

    InventoryClick(user, event.getCurrentItem().

    getType(),event.

    getRawSlot(),event.

    getSlotType(),event.

    getClick().

    ordinal())))

    {
        // Buffer filled, now check
        final Stack<Long> deltaTimes = new Stack<>();
        final Stack<Double> slotDistances = new Stack<>();
        final Stack<Integer> slotTypes = new Stack<>();
        final Stack<Integer> clickTypes = new Stack<>();

        user.getInventoryData().inventoryClicks.clearLastObjectIteration(
                // The most recently added element is in last (method)
                (last, current) ->
                {
                    deltaTimes.push(last.timeStamp - current.timeStamp);
                    slotDistances.push(this.getSlotDistance(last.clickedRawSlot, current.clickedRawSlot, last.slotType, current.slotType));
                    // Use last here as of the method.
                    slotTypes.push(last.slotType.ordinal());
                    clickTypes.push(last.clickType);
                });

        // Training
        if (trainingPattern.containsKey(user))
        {
            final NeuralNetwork trainedNetwork = this.getNetworkByPattern(trainingPattern.get(user));

            // Invalid pattern
            if (trainedNetwork == null)
            {
                throw new NeuralNetworkException("Tried to train invalid pattern " + trainingPattern.get(user));
            }

            // Init training
            // Just take one stack here as all have the same size
            while (!deltaTimes.empty())
            {
                trainedNetwork.setInput("deltaTimes", deltaTimes.pop());
                trainedNetwork.setInput("slotdistance", slotDistances.pop());
                trainedNetwork.setInput("slotTypes", slotTypes.pop());
                trainedNetwork.setInput("clickTypes", clickTypes.pop());
                // TODO: REAL OUTPUT
                trainedNetwork.train("NONLEGIT");
            }

        }
        else
        // Checking
        {
            final Map<Short, Double> legitResults = new HashMap<>(neuralNetworks.size());

            // Init training
            // Just take one stack here as all have the same size
            while (!deltaTimes.empty())
            {
                neuralNetworks.forEach((neuralNetwork -> neuralNetwork.setInput("deltaTimes", deltaTimes.peek())));
                neuralNetworks.forEach((neuralNetwork -> neuralNetwork.setInput("slotDistances", slotDistances.peek())));
                neuralNetworks.forEach((neuralNetwork -> neuralNetwork.setInput("slotTypes", slotTypes.peek())));
                neuralNetworks.forEach((neuralNetwork -> neuralNetwork.setInput("clickTypes", clickTypes.peek())));

                neuralNetworks.forEach(neuralNetwork ->
                                       {
                                           neuralNetwork.calculate();
                                           legitResults.put(neuralNetwork.pattern, legitResults.getOrDefault(neuralNetwork.pattern, 0D) + neuralNetwork.extractOutputNeurons().get("legit"));
                                       });
                deltaTimes.pop();
                slotDistances.pop();
                slotTypes.pop();
                clickTypes.pop();
            }

            // Get the average legit coverage
            final byte divisor = (byte) legitResults.size();
            legitResults.replaceAll((key, value) -> value /= divisor);

            @SuppressWarnings("ConstantConditions")
            Map.Entry<Short, Double> minEntry = legitResults.entrySet().stream().min((o1, o2) -> {
                if (Objects.equals(o1.getValue(), o2.getValue()))
                {
                    return 0;
                }
                else if (o1.getValue() > o2.getValue())
                {
                    return 1;
                }
                return -1;
                // The .get is ok here as a minimum value is guaranteed here.
            }).get();

            int vl = (int) Math.floor(minEntry.getValue());

            // Set the vl of the player to minEntry
            PlayerAdditionViolationEvent violationEvent = new PlayerAdditionViolationEvent(user.getPlayer(), this.getAdditionHackType(), vl);
            Bukkit.getPluginManager().callEvent(violationEvent);

            if (!violationEvent.isCancelled())
            {
                vlManager.setVL(user.getPlayer(), vl);
            }
        }
    }*/
}

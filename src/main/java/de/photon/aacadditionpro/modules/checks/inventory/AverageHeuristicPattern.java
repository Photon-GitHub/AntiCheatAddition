package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.DoubleStatistics;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.server.ServerUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class AverageHeuristicPattern extends PatternModule.Pattern<User, InventoryClickEvent>
{
    @LoadFromConfiguration(configPath = ".max_ping")
    private double maxPing;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    @Override
    protected int process(User user, InventoryClickEvent event)
    {
        // Make sure that we have normal click actions.
        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) &&
            // Creative-clear might trigger this.
            user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            ServerUtil.getTPS() > minTps &&
            // Minimum ping
            (maxPing < 0 || ServerUtil.getPing(user.getPlayer()) <= maxPing))
        {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                user.getInventoryData().averageHeuristicMisclicks++;
            }
            // Shift - Double - Click shortcut will generate a lot of clicks.
            else if (user.getDataMap().getValue(DataKey.LAST_MATERIAL_CLICKED) != event.getCurrentItem().getType() &&
                     // Buffer the new click
                     user.getInventoryData().getAverageHeuristicClicks().bufferObject(InventoryClick.fromClickEvent(event)))
            {
                final List<InventoryClick.BetweenClickInformation> betweenClicks = new ArrayList<>();
                user.getInventoryData().getAverageHeuristicClicks().clearLastTwoObjectsIteration((youngElement, oldElement) ->
                                                                                                 {
                                                                                                     // Only add clicks from the same inventory to make sure that there won't be any bugs.
                                                                                                     if (oldElement.inventory.equals(youngElement.inventory)) {
                                                                                                         betweenClicks.add(new InventoryClick.BetweenClickInformation(oldElement, youngElement));
                                                                                                     }
                                                                                                 });

                // Not enough data to check as the player opened many different inventories.
                if (betweenClicks.size() < 8) {
                    user.getInventoryData().averageHeuristicMisclicks = 0;
                    return 0;
                }

                final DoubleStatistics statistics = new DoubleStatistics();
                for (InventoryClick.BetweenClickInformation betweenClick : betweenClicks) {
                    statistics.accept(betweenClick.timeDelta);
                }

                final double average = statistics.getAverage();
                double squaredErrorsSum = 0;
                for (InventoryClick.BetweenClickInformation betweenClick : betweenClicks) {
                    squaredErrorsSum += MathUtils.squaredError(average, betweenClick.timeDelta);
                }

                // One time 2 ticks offset and 2 times 1 tick offset * 15 minimum vl = 168750
                // 2500 error sum is legit achievable.
                // +1 to avoid division by 0
                double vl = 40000 / (squaredErrorsSum + 1);

                // Average below 1 tick is considered unhuman and increases vl.
                double ticks = average / 50;
                double averageMultiplier = 1.65 + ticks * (-0.171127 + (0.00709709 - 0.000102881 * ticks) * ticks);
                vl *= Math.max(averageMultiplier, 0.5);

                // Make sure that misclicks are applied correctly.
                vl /= (user.getInventoryData().averageHeuristicMisclicks + 1);

                // Mitigation for possibly better players.
                vl -= 10;
                message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " has bot-like click delays. (SE: " + squaredErrorsSum + " | A: " + average + " | MC: " + user.getInventoryData().averageHeuristicMisclicks + " | VLU: " + vl + ")";
                user.getInventoryData().averageHeuristicMisclicks = 0;
                return vl < 10 ? 0 : (int) Math.min(vl, 35);
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.AverageHeuristic";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

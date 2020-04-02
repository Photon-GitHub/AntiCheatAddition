package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.DoubleStatistics;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.server.ServerUtil;
import org.bukkit.GameMode;
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
            (user.getPlayer().getGameMode() == GameMode.SURVIVAL || user.getPlayer().getGameMode() == GameMode.ADVENTURE) &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            ServerUtil.getTPS() > minTps &&
            // Minimum ping
            (maxPing < 0 || ServerUtil.getPing(user.getPlayer()) <= maxPing))
        {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                user.getInventoryData().averageHeuristicMisclicks++;
            }
            // Buffer the new click
            else if (user.getInventoryData().getAverageHeuristicClicks().bufferObject(InventoryClick.fromClickEvent(event))) {
                final List<InventoryClick.BetweenClickInformation> betweenClicks = new ArrayList<>();
                user.getInventoryData().getAverageHeuristicClicks().clearLastTwoObjectsIteration((youngElement, oldElement) -> betweenClicks.add(new InventoryClick.BetweenClickInformation(oldElement, youngElement)));

                final DoubleStatistics statistics = new DoubleStatistics();
                for (InventoryClick.BetweenClickInformation betweenClick : betweenClicks) {
                    statistics.accept(betweenClick.timeDelta);
                }

                final double average = statistics.getAverage();
                double squaredErrorsSum = 0;
                for (InventoryClick.BetweenClickInformation betweenClick : betweenClicks) {
                    squaredErrorsSum += MathUtils.squaredError(average, betweenClick.timeDelta);
                }

                // One time 2 ticks offset and 5 times 1 tick offset * 15 minimum vl = 196875
                // (100 * 100 + 5 * 25 * 25) * 15
                // +1 to avoid division by 0
                double vl = 196875 / (squaredErrorsSum + 1);
                System.out.println("SquaredErrors: " + squaredErrorsSum + " | vl: " + vl);
                // Average below 1 tick is considered unhuman and increases vl.
                double ticks = average / 50;
                /*
                 Data points on ticks:
                    {2.5, 2, 1.9, 1.75, 1.6, 1.5, 1.4, 1.35, 1.35, 1.3, 1.25, 1.2,
                    1.2, 1.2, 1.1, 1.1, 1.1, 1.0, 1.0, 1.0, 1.0, 0.95, 0.95, 0.95, 0.9,
                    0.9, 0.9, 0.85, 0.85, 0.85, 0.8, 0.8, 0.8, 0.7, 0.7, 0.7} with an x^3 polynomial.
                 */
                double averageMultiplier = 2.38857 + ticks * (-0.171127 + (0.00709709 - 0.000102881 * ticks) * ticks);
                vl *= Math.max(averageMultiplier, 0.5);
                System.out.println("Average: " + average + " | vl: " + vl);
                // Make sure that misclicks are applied correctly.
                vl /= (user.getInventoryData().averageHeuristicMisclicks + 1);
                user.getInventoryData().averageHeuristicMisclicks = 0;
                System.out.println("VLMisclicks: " + vl);
                return vl < 15 ? 0 : (int) Math.min(vl, 40);
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

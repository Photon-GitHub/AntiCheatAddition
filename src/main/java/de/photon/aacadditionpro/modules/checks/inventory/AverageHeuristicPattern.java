package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.DoubleStatistics;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.server.ServerUtil;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class AverageHeuristicPattern extends PatternModule.Pattern<User, InventoryClickEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;
    @LoadFromConfiguration(configPath = ".timeout")
    @Getter
    private int timeout;

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
                // Make sure that misclicks are applied correctly.
                vl /= (user.getInventoryData().averageHeuristicMisclicks + 1);
                user.getInventoryData().averageHeuristicMisclicks = 0;
                System.out.println("VLMisclicks: " + vl);
                // Average below 1 tick is considered unhuman and increases vl.
                vl *= average / 50;
                System.out.println("Average: " + average + " | vl: " + vl);
                return (int) MathUtils.bound(15, 40, vl);
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

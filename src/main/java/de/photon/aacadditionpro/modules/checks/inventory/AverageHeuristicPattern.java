package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.BatchProcessorModule;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.InventoryData;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import de.photon.aacadditionpro.util.datastructures.iteration.IterationUtil;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.server.ServerUtil;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class AverageHeuristicPattern implements BatchProcessorModule<InventoryClick>, ListenerModule
{
    @Getter
    private static final AverageHeuristicPattern instance = new AverageHeuristicPattern();

    private BatchProcessor<InventoryClick> averageHeuristicBatchProcessor;


    @LoadFromConfiguration(configPath = ".max_ping")
    private double maxPing;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

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
            else if (user.getDataMap().getValue(DataKey.LAST_MATERIAL_CLICKED) != event.getCurrentItem().getType()) {
                user.getInventoryData().getBatch().addDataPoint(InventoryClick.fromClickEvent(event));
            }
        }
    }

    @Override
    public void enable()
    {
        averageHeuristicBatchProcessor = new AverageHeuristicBatchProcessor();
    }

    @Override
    public void disable()
    {
        averageHeuristicBatchProcessor.killProcessing();
    }

    @Override
    public BatchProcessor<InventoryClick> getBatchProcessor()
    {
        return averageHeuristicBatchProcessor;
    }

    @Override
    public boolean isSubModule()
    {
        return true;
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

    private static class AverageHeuristicBatchProcessor extends BatchProcessor<InventoryClick>
    {
        public AverageHeuristicBatchProcessor()
        {
            super(InventoryData.AVERAGE_HEURISTICS_BATCH_SIZE);
        }

        @Override
        public void processBatch(User user, List<InventoryClick> batch)
        {
            final List<InventoryClick.BetweenClickInformation> betweenClicks = IterationUtil.pairCombine(batch, (old, current) -> old.inventory.equals(current.inventory), InventoryClick.BetweenClickInformation::new);

            // Not enough data to check as the player opened many different inventories.
            if (betweenClicks.size() < 8) {
                user.getInventoryData().averageHeuristicMisclicks = 0;
                return;
            }

            final double average = betweenClicks.stream().mapToDouble(between -> between.timeDelta).average().orElseThrow(() -> new IllegalArgumentException("Could not get average of BetweenClick stream."));
            final double squaredErrorsSum = betweenClicks.stream().mapToDouble(between -> MathUtils.squaredError(average, between.timeDelta)).sum();

            // One time 2 ticks offset and 2 times 1 tick offset * 15 minimum vl = 168750
            // 2500 error sum is legit achievable.
            // +1 to avoid division by 0
            double vl = 40000 / (squaredErrorsSum + 1);

            // Average below 1 tick is considered inhuman and increases vl.
            final double ticks = average / 50;
            final double averageMultiplier = 1.65 + ticks * (-0.171127 + (0.00709709 - 0.000102881 * ticks) * ticks);
            vl *= Math.max(averageMultiplier, 0.5);

            // Make sure that misclicks are applied correctly.
            vl /= (user.getInventoryData().averageHeuristicMisclicks + 1);

            // Mitigation for possibly better players.
            vl -= 10;

            // Too low vl.
            if (vl < 10) {
                return;
            }

            final double finalVl = vl;
            Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(),
                                                                       (int) Math.min(vl, 35),
                                                                       0,
                                                                       () -> {},
                                                                       () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " has bot-like click delays. (SE: " + squaredErrorsSum + " | A: " + average + " | MC: " + user.getInventoryData().averageHeuristicMisclicks + " | VLU: " + finalVl + ")"));
            user.getInventoryData().averageHeuristicMisclicks = 0;
        }
    }
}
